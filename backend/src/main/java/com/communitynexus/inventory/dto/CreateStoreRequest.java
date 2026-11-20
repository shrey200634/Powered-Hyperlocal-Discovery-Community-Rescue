package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.StoreCategory;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record CreateStoreRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull StoreCategory category,
        @NotBlank @Size(max = 50) String city,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
        @Size(max = 300) String addressLine,
        @NotNull LocalTime opensAt,      // JSON: "09:00"
        @NotNull LocalTime closesAt      // JSON: "21:30"
) {
}