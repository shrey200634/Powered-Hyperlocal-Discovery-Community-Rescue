package com.communitynexus.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record FlagRequest(
        @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal savePrice
) {
}