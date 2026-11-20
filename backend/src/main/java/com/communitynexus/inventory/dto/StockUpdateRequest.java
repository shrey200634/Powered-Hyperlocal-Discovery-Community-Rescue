package com.communitynexus.inventory.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record StockUpdateRequest(
        @Min(0) Integer quantity,
        @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal price
) {
    @AssertTrue(message = "Send quantity or price")
    public boolean isSomethingToUpdate() {
        return quantity != null || price != null;
    }
}