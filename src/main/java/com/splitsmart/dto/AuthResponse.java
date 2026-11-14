package com.splitsmart.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}