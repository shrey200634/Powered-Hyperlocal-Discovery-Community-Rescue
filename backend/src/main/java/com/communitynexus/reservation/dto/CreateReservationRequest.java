package com.communitynexus.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull Long productId,
        @Min(1) @Max(5) Integer quantity      // optional, default 1
) {
}