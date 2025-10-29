package com.mijimoto.ECommerce.auth.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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

    /**
     * Generate access token with user roles
     * @param userId User ID
     * @param sessionUuid Session UUID
     * @param roles List of role names (e.g., ["ADMIN", "USER"])
     */
    public SignedToken generateAccessToken(Integer userId, String sessionUuid, List<String> roles) {
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
                .claim("roles", roles)  // Add roles to JWT
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

    /**
     * Extract roles from JWT claims
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }
}