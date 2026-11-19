package com.communitynexus.user.dto;

import com.communitynexus.common.Language;
import jakarta.validation.constraints.Size;

/** Every field optional: send only what you want to change. */
public record UpdateProfileRequest(
        @Size(min = 1, max = 80, message = "Name must be 1-80 characters")
        String name,
        Language language,
        Boolean seniorMode,
        Boolean liteMode
) {
}