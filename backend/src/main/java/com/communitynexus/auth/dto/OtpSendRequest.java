package com.communitynexus.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Accepts "9876543210" or "+919876543210" (Indian mobile: starts 6-9, 10 digits). */
public record OtpSendRequest(
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone
) {
}