package com.mijimoto.ECommerce.config.security;

import com.mijimoto.ECommerce.auth.security.JwtAuthFilter;
import com.mijimoto.ECommerce.auth.services.JwtService;
import com.mijimoto.ECommerce.auth.services.RedisTokenService;
import com.mijimoto.ECommerce.user.persistence.repositories.UsersRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final UsersRepository usersRepo;

    public SecurityConfig(JwtService jwtService, RedisTokenService redisTokenService, UsersRepository usersRepo) {
        this.jwtService = jwtService;
        this.redisTokenService = redisTokenService;
        this.usersRepo = usersRepo;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, redisTokenService, usersRepo);

        // Allow swagger and public endpoints
        String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs.yaml",
            "/webjars/**"
        };

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/health").permitAll()
                .requestMatchers(SWAGGER_WHITELIST).permitAll() // ✅ allow Swagger access
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Optional convenience bean
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return (BCryptPasswordEncoder) passwordEncoder();
    }
}
