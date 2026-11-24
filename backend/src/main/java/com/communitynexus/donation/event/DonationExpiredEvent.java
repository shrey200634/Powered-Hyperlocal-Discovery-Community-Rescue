package com.communitynexus.donation.event;

import java.time.Instant;

public record DonationExpiredEvent(
        Long donationId,
        Long donorId,
        Long ngoId,          // null if no NGO had accepted
        int totalServings,
        Instant occurredAt
) {
}