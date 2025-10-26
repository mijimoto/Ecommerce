package com.mijimoto.ECommerce.auth.rest.controllers;

import com.mijimoto.ECommerce.auth.rest.dto.LoginRequestDTO;
import com.mijimoto.ECommerce.auth.rest.dto.LoginResponseDTO;
import com.mijimoto.ECommerce.auth.rest.dto.RefreshRequestDTO;
import com.mijimoto.ECommerce.auth.rest.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO req, HttpServletRequest servletReq) {
        if (req.getIp() == null || req.getIp().isBlank()) req.setIp(servletReq.getRemoteAddr());
        var resp = authService.login(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@RequestBody RefreshRequestDTO req) {
        var resp = authService.refresh(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) RefreshRequestDTO req) {
        String refreshToken = req == null ? null : req.getRefreshToken();
        authService.logout(authorization, refreshToken);
        return ResponseEntity.noContent().build();
    }
}
