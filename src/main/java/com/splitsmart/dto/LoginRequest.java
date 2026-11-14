package com.splitsmart.dto;

public record LoginRequest(
        String email,
        String password
) {
}