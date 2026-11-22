package com.communitynexus.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PickupRequest(
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "Pickup code is 4 digits") String code
) {
}