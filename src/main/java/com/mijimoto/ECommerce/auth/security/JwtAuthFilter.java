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
            Jws<io.jsonwebtoken.Claims> parsed = jwtService.parseToken(token);
            Claims c = parsed.getPayload();  // getPayload() instead of getBody()
            String jti = c.getId();
            String sub = c.getSubject();
            if (jti == null || sub == null) throw new RuntimeException("Invalid token");
            if (!redisTokenService.jtiExists(jti)) {
                // token revoked or expired
                chain.doFilter(req, res);
                return;
            }
            Integer userId = Integer.valueOf(sub);
            // load user if needed
            var userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                chain.doFilter(req, res);
                return;
            }
            // set an Authentication (authorities minimal)
            var auth = new UsernamePasswordAuthenticationToken(
                    userOpt.get(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            // invalid token => continue unauthenticated
        }
        chain.doFilter(req, res);
    }
}