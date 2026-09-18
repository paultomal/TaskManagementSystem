package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.ChangePasswordRequest;
import com.example.taskmanagement.dto.LoginRequest;
import com.example.taskmanagement.dto.RefreshRequest;
import com.example.taskmanagement.dto.TokenResponse;
import com.example.taskmanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Self-service password change using username + current password. No login or
     * OTP required. All existing sessions are revoked on success.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Force-logout: admin-only revocation of all refresh tokens for a user.
     */
    @PostMapping("/force-logout/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceLogout(@PathVariable String username) {
        authService.forceLogout(username);
        return ResponseEntity.noContent().build();
    }
}
