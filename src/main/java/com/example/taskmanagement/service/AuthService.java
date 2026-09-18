package com.example.taskmanagement.service;

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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    public TokenResponse login(LoginRequest request) {
        // Delegates credential checking to the AuthenticationManager (DaoAuthenticationProvider).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return issueTokens(request.username());
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
        return issueTokens(username);
    }

    public void logout(String refreshToken) {
        try {
            refreshTokenService.revoke(jwtUtil.getUsername(refreshToken), jwtUtil.getJti(refreshToken));
        } catch (JwtException ignored) {
            // Nothing to revoke for an unparseable token.
        }
    }

    public void forceLogout(String username) {
        refreshTokenService.revokeAll(username);
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
