package com.mijimoto.ECommerce.auth.rest.services;

import com.mijimoto.ECommerce.auth.persistence.entities.UserSessions;
import com.mijimoto.ECommerce.auth.persistence.entities.UserTokens;
import com.mijimoto.ECommerce.auth.persistence.repositories.UserSessionsRepository;
import com.mijimoto.ECommerce.auth.persistence.repositories.UserTokensRepository;
import com.mijimoto.ECommerce.user.persistence.entities.Users;
import com.mijimoto.ECommerce.user.persistence.repositories.UsersRepository;
import com.mijimoto.ECommerce.auth.util.CryptoUtils;
import com.mijimoto.ECommerce.auth.rest.dto.LoginResponseDTO;
import com.mijimoto.ECommerce.auth.rest.dto.LoginRequestDTO;
import com.mijimoto.ECommerce.auth.rest.dto.RefreshRequestDTO;
import com.mijimoto.ECommerce.auth.services.JwtService;
import com.mijimoto.ECommerce.auth.services.RedisTokenService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UsersRepository usersRepo;
    private final UserSessionsRepository sessionsRepo;
    private final UserTokensRepository tokensRepo;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final CryptoUtils crypto;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long accessTtlSeconds;
    private final long refreshTtlDays;

    public AuthService(UsersRepository usersRepo,
                       UserSessionsRepository sessionsRepo,
                       UserTokensRepository tokensRepo,
                       JwtService jwtService,
                       RedisTokenService redisTokenService,
                       CryptoUtils crypto,
                       BCryptPasswordEncoder passwordEncoder,
                       @Value("${app.jwt.access-token-expiry-seconds}") long accessTtlSeconds,
                       @Value("${app.jwt.refresh-token-expiry-days}") long refreshTtlDays) {
        this.usersRepo = usersRepo;
        this.sessionsRepo = sessionsRepo;
        this.tokensRepo = tokensRepo;
        this.jwtService = jwtService;
        this.redisTokenService = redisTokenService;
        this.crypto = crypto;
        this.passwordEncoder = passwordEncoder;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlDays = refreshTtlDays;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO req) {
        Users u = usersRepo.findByEmail(req.getEmail()).orElseThrow(() -> new RuntimeException("Invalid creds"));
        if (u.getIsActive() == null || !u.getIsActive()) throw new RuntimeException("User inactive");
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) throw new RuntimeException("Invalid creds");

        // create session
        UserSessions s = new UserSessions();
        s.setUsers(u);
        s.setSessionUuid(UUID.randomUUID().toString());
        s.setDeviceName(req.getDeviceName());
        s.setDeviceType(req.getDeviceType());
        s.setIpAddress(req.getIp());
        s.setUserAgent(req.getDeviceType());
        s.setIsActive(true);
        s.setCreatedAt(Date.from(Instant.now()));
        s.setExpiresAt(Date.from(Instant.now().plusSeconds(refreshTtlDays * 86400)));
        sessionsRepo.save(s);

        // create access token and store jti in redis
        var signed = jwtService.generateAccessToken(u.getId(), s.getSessionUuid());
        String jti = signed.jti();
        // store small payload
        String payload = String.format("{\"uid\":%d,\"session\":\"%s\"}", u.getId(), s.getSessionUuid());
        redisTokenService.storeJti(jti, payload, accessTtlSeconds);

        // create refresh token raw + hash
        String refreshRaw = crypto.generateRefreshTokenRaw(48);
        String refreshHash = crypto.hmacSha256Hex(refreshRaw);

        UserTokens tok = new UserTokens();
        tok.setUsers(u);
        tok.setUserSessions(s);
        tok.setTokenHash(refreshHash);
        tok.setTokenType("REFRESH");
        tok.setIsRevoked(false);
        tok.setCreatedAt(Date.from(Instant.now()));
        tok.setExpiresAt(Date.from(Instant.now().plusSeconds(refreshTtlDays * 86400)));
        tokensRepo.save(tok);

        LoginResponseDTO resp = new LoginResponseDTO();
        resp.setAccessToken(signed.token());
        resp.setExpiresAt(signed.expiresAt());
        resp.setRefreshToken(refreshRaw);
        resp.setSessionUuid(s.getSessionUuid());
        return resp;
    }

    @Transactional
    public LoginResponseDTO refresh(RefreshRequestDTO req) {
        // find token by hash
        String raw = req.getRefreshToken();
        String hash = crypto.hmacSha256Hex(raw);
        Optional<UserTokens> t = tokensRepo.findByTokenHash(hash);
        UserTokens token = t.orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (token.getIsRevoked()) throw new RuntimeException("Revoked");
        if (token.getExpiresAt().before(Date.from(Instant.now()))) throw new RuntimeException("Expired");

        Users u = token.getUsers();
        UserSessions session = token.getUserSessions();

        // rotate: revoke old token and create a new one
        token.setIsRevoked(true);
        token.setRevokedAt(Date.from(Instant.now()));
        tokensRepo.save(token);

        String newRefreshRaw = crypto.generateRefreshTokenRaw(48);
        String newHash = crypto.hmacSha256Hex(newRefreshRaw);
        UserTokens newTok = new UserTokens();
        newTok.setUsers(u);
        newTok.setUserSessions(session);
        newTok.setTokenHash(newHash);
        newTok.setTokenType("REFRESH");
        newTok.setIsRevoked(false);
        newTok.setCreatedAt(Date.from(Instant.now()));
        newTok.setExpiresAt(Date.from(Instant.now().plusSeconds(refreshTtlDays * 86400)));
        tokensRepo.save(newTok);

        // generate new access token
        var signed = jwtService.generateAccessToken(u.getId(), session.getSessionUuid());
        redisTokenService.storeJti(signed.jti(), String.format("{\"uid\":%d,\"session\":\"%s\"}", u.getId(), session.getSessionUuid()), accessTtlSeconds);

        LoginResponseDTO resp = new LoginResponseDTO();
        resp.setAccessToken(signed.token());
        resp.setExpiresAt(signed.expiresAt());
        resp.setRefreshToken(newRefreshRaw);
        resp.setSessionUuid(session.getSessionUuid());
        return resp;
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // delete access jti if present
        if (accessToken != null && accessToken.startsWith("Bearer ")) accessToken = accessToken.substring(7);
        try {
            if (accessToken != null) {
                var claims = jwtService.parseToken(accessToken).getBody();
                String jti = claims.getId();
                if (jti != null) redisTokenService.deleteJti(jti);
            }
        } catch (Exception ignored) {}

        if (refreshToken != null) {
            String hash = crypto.hmacSha256Hex(refreshToken);
            tokensRepo.findByTokenHash(hash).ifPresent(t -> {
                t.setIsRevoked(true);
                t.setRevokedAt(Date.from(Instant.now()));
                tokensRepo.save(t);
            });
        }
    }
}
