package com.communitynexus.donation.event;

import java.time.Instant;
import java.util.Map;


public record FoodPostedEvent(
        Long donationId,
        Long donorId,
        String city,
        double lat,
        double lng,
        int totalServings,
        Map<String, Integer> servingsByFoodGroup,   // e.g. {GRAIN=40, PROTEIN=40}
        Instant safeUntil,
        Instant occurredAt
) {
}