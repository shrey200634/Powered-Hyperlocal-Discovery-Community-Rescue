package com.communitynexus.auth.dto;

import com.communitynexus.auth.Role;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Long accountId,
        Role role
) {
}