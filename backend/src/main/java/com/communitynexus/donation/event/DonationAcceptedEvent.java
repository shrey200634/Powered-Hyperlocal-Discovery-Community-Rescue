package com.communitynexus.donation.event;

import java.time.Instant;


public record DonationAcceptedEvent(
        Long donationId,
        Long donorId,
        Long ngoId,
        Long ngoAccountId,
        double pickupLat,
        double pickupLng,
        double dropLat,
        double dropLng,
        int totalServings,
        Instant pickupBy,
        Instant occurredAt
) {
}