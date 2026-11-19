package com.communitynexus.auth.dto;

import com.communitynexus.auth.Role;
import com.communitynexus.common.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Signup token is required")
        String signupToken,

        @NotBlank(message = "Name is required")
        @Size(max = 80, message = "Name is too long")
        String name,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Language is required")
        Language language
) {
}