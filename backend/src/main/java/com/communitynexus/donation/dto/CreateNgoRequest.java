package com.communitynexus.donation.dto;

import jakarta.validation.constraints.*;

public record CreateNgoRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 50) String city,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
        @NotNull @Min(1) @Max(5000) Integer capacityPerDay,
        @NotNull @Min(1) @Max(5000) Integer headcount
) {
}