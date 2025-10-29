package com.mijimoto.ECommerce.auth.security;

import com.mijimoto.ECommerce.auth.services.JwtService;
import com.mijimoto.ECommerce.auth.services.RedisTokenService;
import com.mijimoto.ECommerce.user.persistence.repositories.UsersRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final UsersRepository userRepo;
    
    public JwtAuthFilter(JwtService jwtService, RedisTokenService redisTokenService, UsersRepository userRepo) {
        this.jwtService = jwtService;
        this.redisTokenService = redisTokenService;
        this.userRepo = userRepo;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        
        String token = header.substring(7);
        
        try {
            Jws<Claims> parsed = jwtService.parseToken(token);
            Claims c = parsed.getPayload();
            
            String jti = c.getId();
            String sub = c.getSubject();
            
            if (jti == null || sub == null) throw new RuntimeException("Invalid token");
            
            // Check if token is revoked in Redis
            if (!redisTokenService.jtiExists(jti)) {
                chain.doFilter(req, res);
                return;
            }
            
            Integer userId = Integer.valueOf(sub);
            
            // Load user from database
            var userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                chain.doFilter(req, res);
                return;
            }
            
            // Extract roles from JWT claims
            List<String> roles = jwtService.extractRoles(c);
            
            // Convert roles to Spring Security authorities
            // Add ROLE_ prefix if not present (Spring Security convention)
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            // Set authentication with user and authorities
            var auth = new UsernamePasswordAuthenticationToken(
                    userOpt.get(), null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(auth);
            
        } catch (Exception ex) {
            // Invalid token - continue unauthenticated
        }
        
        chain.doFilter(req, res);
    }
}