package com.communitynexus.donation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;

public record PostDonationRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
        @Size(max = 50) String city,
        @Size(max = 200) String pickupNote,

        @NotNull OffsetDateTime cookedAt,

        @DecimalMin("-10.0") @DecimalMax("55.0") Double dayHighC,

        @NotEmpty(message = "Add at least one item")
        @Size(max = 10)
        List<@Valid DonationItemRequest> items,

        @AssertTrue(message = "Please confirm the hygiene checklist")
        boolean hygieneConfirmed
) {
}