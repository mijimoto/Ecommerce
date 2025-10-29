package com.mijimoto.ECommerce.auth.rest.services;

import com.mijimoto.ECommerce.auth.persistence.entities.*;
import com.mijimoto.ECommerce.auth.persistence.repositories.*;
import com.mijimoto.ECommerce.user.persistence.entities.Users;
import com.mijimoto.ECommerce.user.persistence.repositories.UsersRepository;
import com.mijimoto.ECommerce.user.persistence.repositories.UserRolesRepository;
import com.mijimoto.ECommerce.auth.util.CryptoUtils;
import com.mijimoto.ECommerce.auth.rest.dto.*;
import com.mijimoto.ECommerce.auth.services.*;
import com.mijimoto.ECommerce.common.mail.MailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UsersRepository usersRepo;
    private final UserSessionsRepository sessionsRepo;
    private final UserTokensRepository tokensRepo;
    private final UserRolesRepository userRolesRepo;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final CryptoUtils crypto;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final StringRedisTemplate redis;

    private final long accessTtlSeconds;
    private final long refreshTtlDays;

    public AuthService(
            UsersRepository usersRepo,
            UserSessionsRepository sessionsRepo,
            UserTokensRepository tokensRepo,
            UserRolesRepository userRolesRepo,
            JwtService jwtService,
            RedisTokenService redisTokenService,
            CryptoUtils crypto,
            BCryptPasswordEncoder passwordEncoder,
            MailService mailService,
            StringRedisTemplate redis,
            @Value("${app.jwt.access-token-expiry-seconds}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-token-expiry-days}") long refreshTtlDays) {

        this.usersRepo = usersRepo;
        this.sessionsRepo = sessionsRepo;
        this.tokensRepo = tokensRepo;
        this.userRolesRepo = userRolesRepo;
        this.jwtService = jwtService;
        this.redisTokenService = redisTokenService;
        this.crypto = crypto;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.redis = redis;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlDays = refreshTtlDays;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO req) {
        Users u = usersRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!Boolean.TRUE.equals(u.getIsActive()))
            throw new RuntimeException("Please verify your email first");
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash()))
            throw new RuntimeException("Invalid credentials");

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

        // Fetch user roles directly using Lombok-generated getters
        List<String> roles = userRolesRepo.findByUsersId(u.getId())
                .stream()
                .map(ur -> ur.getRoles().getName()) // use Lombok-generated getName()
                .toList();

        // Generate access token with roles
        var signed = jwtService.generateAccessToken(u.getId(), s.getSessionUuid(), roles);

        // Store in Redis with roles included
        String payload = String.format(
            "{\"uid\":%d,\"session\":\"%s\",\"roles\":%s}",
            u.getId(),
            s.getSessionUuid(),
            roles.stream().map(r -> "\"" + r + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"))
        );
        redisTokenService.storeJti(signed.jti(), payload, accessTtlSeconds);

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

        return new LoginResponseDTO(signed.token(), refreshRaw, signed.expiresAt(), s.getSessionUuid());
    }

    public void sendVerificationEmail(String email) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (Boolean.TRUE.equals(user.getIsActive()))
            throw new RuntimeException("User already verified");

        String code = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("verify:" + code, email, Duration.ofMinutes(15));

        String verifyUrl = "http://localhost:8080/api/auth/verify-email?code=" + code;
        mailService.sendMail(email, "Verify your account", "Click here to verify: " + verifyUrl);
    }

    @Transactional
    public void verifyEmail(String code) {
        String email = redis.opsForValue().get("verify:" + code);
        if (email == null) throw new RuntimeException("Invalid or expired verification link");

        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(true);
        user.setUpdatedAt(Date.from(Instant.now()));
        usersRepo.save(user);

        redis.delete("verify:" + code);
    }

    public void forgotPassword(String email) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String code = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("reset:" + code, email, Duration.ofMinutes(10));

        String resetUrl = "http://localhost:8080/api/auth/reset-password?code=" + code;
        mailService.sendMail(email, "Password reset", "Click to reset your password: " + resetUrl);
    }

    @Transactional
    public void resetPassword(String code, String newPassword) {
        String email = redis.opsForValue().get("reset:" + code);
        if (email == null) throw new RuntimeException("Invalid or expired reset token");

        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Date.from(Instant.now()));
        usersRepo.save(user);
        redis.delete("reset:" + code);
    }

    @Transactional
    public LoginResponseDTO refresh(RefreshRequestDTO req) {
        String raw = req.getRefreshToken();
        String hash = crypto.hmacSha256Hex(raw);
        Optional<UserTokens> t = tokensRepo.findByTokenHash(hash);
        UserTokens token = t.orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (token.getIsRevoked()) throw new RuntimeException("Revoked");
        if (token.getExpiresAt().before(Date.from(Instant.now()))) throw new RuntimeException("Expired");

        Users u = token.getUsers();
        UserSessions session = token.getUserSessions();

        // Revoke old token
        token.setIsRevoked(true);
        token.setRevokedAt(Date.from(Instant.now()));
        tokensRepo.save(token);

        // Create new refresh token
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

        // Fetch user roles directly using Lombok-generated getters
        List<String> roles = userRolesRepo.findByUsersId(u.getId())
                .stream()
                .map(ur -> ur.getRoles().getName()) // use Lombok-generated getName()
                .toList();

        // Generate new access token with roles
        var signed = jwtService.generateAccessToken(u.getId(), session.getSessionUuid(), roles);

        String payload = String.format(
            "{\"uid\":%d,\"session\":\"%s\",\"roles\":%s}",
            u.getId(),
            session.getSessionUuid(),
            roles.stream().map(r -> "\"" + r + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"))
        );
        redisTokenService.storeJti(signed.jti(), payload, accessTtlSeconds);

        LoginResponseDTO resp = new LoginResponseDTO();
        resp.setAccessToken(signed.token());
        resp.setExpiresAt(signed.expiresAt());
        resp.setRefreshToken(newRefreshRaw);
        resp.setSessionUuid(session.getSessionUuid());
        return resp;
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) accessToken = accessToken.substring(7);
        try {
            if (accessToken != null) {
                var claims = jwtService.parseToken(accessToken).getPayload();
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
