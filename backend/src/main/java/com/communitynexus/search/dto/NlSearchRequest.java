package com.communitynexus.search.dto;

import com.communitynexus.common.Language;
import jakarta.validation.constraints.*;

public record NlSearchRequest(
        @NotBlank @Size(max = 200) String text,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
        Integer radius,
        String city,
        Language language          // HI or EN; the spoken reply uses this. Default HI.
) {
}