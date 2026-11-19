package com.communitynexus.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "Please give a reason")
        @Size(max = 300)
        String note
) {
}