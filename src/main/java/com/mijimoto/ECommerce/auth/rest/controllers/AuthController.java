package com.mijimoto.ECommerce.auth.rest.controllers;

import com.mijimoto.ECommerce.auth.rest.dto.*;
import com.mijimoto.ECommerce.auth.rest.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO req, HttpServletRequest servletReq) {
        if (req.getIp() == null || req.getIp().isBlank()) req.setIp(servletReq.getRemoteAddr());
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@RequestBody RefreshRequestDTO req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) RefreshRequestDTO req) {
        String refreshToken = req == null ? null : req.getRefreshToken();
        authService.logout(authorization, refreshToken);
        return ResponseEntity.noContent().build();
    }

    // ===== EMAIL VERIFICATION =====
    @PostMapping("/send-verify-email")
    public ResponseEntity<Void> sendVerifyEmail(@RequestParam String email) {
        authService.sendVerificationEmail(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String code) {
        authService.verifyEmail(code);
        return ResponseEntity.ok("Email verified successfully");
    }

    // ===== PASSWORD RESET =====
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String code, @RequestParam String newPassword) {
        authService.resetPassword(code, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }
}
