package com.mijimoto.ECommerce.auth.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTtlSeconds;
    private final String issuer;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-expiry-seconds}") long accessTtlSeconds,
                      @Value("${app.jwt.access-token-issuer:auth}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTtlSeconds = accessTtlSeconds;
        this.issuer = issuer;
    }

    public record SignedToken(String token, String jti, Date expiresAt) {}

    public SignedToken generateAccessToken(Integer userId, String sessionUuid) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(accessTtlSeconds));

        String token = Jwts.builder()
                .id(jti)                 
                .subject(String.valueOf(userId)) 
                .issuer(issuer)          
                .issuedAt(iat)          
                .expiration(exp)         
                .claim("session_uuid", sessionUuid)
                .signWith(signingKey)   
                .compact();

        return new SignedToken(token, jti, exp);
    }

    public Jws<Claims> parseToken(String token) throws JwtException {
        return Jwts.parser()
                   .verifyWith(signingKey)      
                   .build()
                   .parseSignedClaims(token);  
    }
}
