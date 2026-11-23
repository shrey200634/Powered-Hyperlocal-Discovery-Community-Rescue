package com.communitynexus.donation.dto;

import com.communitynexus.donation.FoodGroup;
import com.communitynexus.donation.FoodType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DonationItemRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull FoodGroup foodGroup,
        @NotNull FoodType foodType,
        @NotNull @Min(1) @Max(1000) Integer servings,
        @DecimalMin("0.1") @DecimalMax("500.0") BigDecimal weightKg,
        LocalDate packagedExpiry
) {
    @AssertTrue(message = "Packaged items need their printed expiry date")
    public boolean isPackagedExpiryPresent() {
        return foodType != FoodType.PACKAGED || packagedExpiry != null;
    }
}