package com.logistics.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(Long id, String name, String email, String role) {}
}