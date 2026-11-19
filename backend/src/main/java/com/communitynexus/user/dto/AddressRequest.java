package com.communitynexus.user.dto;

import com.communitynexus.user.entity.AddressLabel;
import jakarta.validation.constraints.*;

public record AddressRequest(
        @NotNull(message = "Label is required")
        AddressLabel label,

        @NotBlank(message = "Address line is required")
        @Size(max = 300, message = "Address is too long")
        String line,

        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double lat,

        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double lng,

        Boolean makeDefault
) {
}