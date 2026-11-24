package com.communitynexus.donation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Doc 05: the NGO "sets capacity". Send one or both. */
public record UpdateNgoRequest(
        @Min(1) @Max(5000) Integer capacityPerDay,
        @Min(1) @Max(5000) Integer headcount
) {
    @AssertTrue(message = "Send capacityPerDay or headcount")
    public boolean isSomethingToUpdate() {
        return capacityPerDay != null || headcount != null;
    }
}