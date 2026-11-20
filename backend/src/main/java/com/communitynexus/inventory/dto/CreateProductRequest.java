package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.ProductCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer quantity,
        @FutureOrPresent(message = "Expiry date is already past") LocalDate expiryDate    // JSON: "2026-12-31"
) {
}