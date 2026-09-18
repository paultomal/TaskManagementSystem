package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.ChangePasswordRequest;
import com.example.taskmanagement.dto.LoginRequest;
import com.example.taskmanagement.dto.TokenResponse;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Role;
import com.example.taskmanagement.model.User;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest request) {
        // Delegates credential checking to the AuthenticationManager (DaoAuthenticationProvider).
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            log.warn("Login failed for user '{}': {}", request.username(), e.getMessage());
            throw e;
        }
        TokenResponse tokens = issueTokens(request.username());
        log.info("Login successful for user '{}'", request.username());
        return tokens;
    }

    public TokenResponse refresh(String refreshToken) {
        String username;
        String jti;
        try {
            if (!JwtUtil.TYPE_REFRESH.equals(jwtUtil.getType(refreshToken))) {
                throw new BadCredentialsException("Not a refresh token");
            }
            username = jwtUtil.getUsername(refreshToken);
            jti = jwtUtil.getJti(refreshToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (!refreshTokenService.isValid(username, jti)) {
            throw new BadCredentialsException("Refresh token has been revoked or expired");
        }

        // Rotate: revoke the used token before issuing a fresh pair.
        refreshTokenService.revoke(username, jti);
        TokenResponse tokens = issueTokens(username);
        log.info("Access token refreshed for user '{}'", username);
        return tokens;
    }

    public void logout(String refreshToken) {
        try {
            String username = jwtUtil.getUsername(refreshToken);
            refreshTokenService.revoke(username, jwtUtil.getJti(refreshToken));
            log.info("Logout for user '{}'", username);
        } catch (JwtException ignored) {
            // Nothing to revoke for an unparseable token.
        }
    }

    public void forceLogout(String username) {
        refreshTokenService.revokeAll(username);
        log.info("Force-logout: revoked all sessions for user '{}'", username);
    }

    /**
     * Self-service password change: the caller proves ownership with the current
     * password (no login/token required). On success every existing refresh token
     * for the user is revoked so old sessions can't keep minting access tokens.
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        // Generic message on both branches to avoid revealing whether the username exists.
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAll(request.username());
        log.info("Password changed for user '{}' (all sessions revoked)", request.username());
    }

    private TokenResponse issueTokens(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        String rolesCsv = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.joining(","));

        String accessToken = jwtUtil.generateAccessToken(username, rolesCsv);
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(username, jti);
        refreshTokenService.store(username, jti, jwtUtil.getRefreshExpirationMs());

        return TokenResponse.bearer(accessToken, refreshToken);
    }
}
