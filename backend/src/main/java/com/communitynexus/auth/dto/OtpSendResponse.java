package com.communitynexus.auth.dto;

public record OtpSendResponse(String message, long expiresInSeconds) {
}