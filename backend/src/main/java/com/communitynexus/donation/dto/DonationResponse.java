package com.communitynexus.donation.dto;

import com.communitynexus.donation.entity.DonationStatus;
import com.communitynexus.donation.entity.FoodDonation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DonationResponse(
        Long id,
        String donorName,
        DonationStatus status,
        Instant cookedAt,
        Instant safeUntil,
        long minutesLeft,
        double dayHighC,
        int totalServings,
        BigDecimal totalWeightKg,
        String city,
        Double lat,
        Double lng,
        String pickupNote,
        Long ngoId,
        List<DonationItemResponse> items
) {
    public static DonationResponse from(FoodDonation d) {
        return new DonationResponse(
                d.getId(), d.getDonorName(), d.getStatus(), d.getCookedAt(), d.getSafeUntil(),
                d.getStatus().isOpen() ? d.minutesLeft() : 0,
                d.getDayHighC(), d.getTotalServings(), d.getTotalWeightKg(),
                d.getCity(), d.getLat(), d.getLng(), d.getPickupNote(), d.getNgoId(),
                d.getItems().stream().map(DonationItemResponse::from).toList());
    }
}