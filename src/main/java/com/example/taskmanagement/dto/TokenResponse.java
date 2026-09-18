package com.example.taskmanagement.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType) {

    public static TokenResponse bearer(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }
}
