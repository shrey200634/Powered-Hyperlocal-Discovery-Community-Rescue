package com.communitynexus.donation.dto;

import com.communitynexus.donation.entity.DonationItem;
import com.communitynexus.donation.entity.FoodDonation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;


public record NearbyDonationResponse(
        Long id,
        String donorName,
        int distanceMeters,
        long minutesLeft,
        Instant safeUntil,
        int totalServings,
        BigDecimal totalWeightKg,
        Map<String, Integer> servingsByFoodGroup,
        List<String> itemNames
) {
    public static NearbyDonationResponse from(FoodDonation d, double distanceMeters, Map<String, Integer> byGroup) {
        return new NearbyDonationResponse(
                d.getId(), d.getDonorName(), (int) Math.round(distanceMeters), d.minutesLeft(), d.getSafeUntil(),
                d.getTotalServings(), d.getTotalWeightKg(), byGroup,
                d.getItems().stream().map(DonationItem::getName).toList());
    }
}