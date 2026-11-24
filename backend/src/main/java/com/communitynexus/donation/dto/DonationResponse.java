package com.communitynexus.donation.dto;

import com.communitynexus.donation.entity.DonationSource;
import com.communitynexus.donation.entity.DonationStatus;
import com.communitynexus.donation.entity.FoodDonation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DonationResponse(
        Long id,
        String donorName,
        DonationSource source,
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
        boolean clockRunning = d.getStatus().isOpen() || d.getStatus() == DonationStatus.ACCEPTED;
        return new DonationResponse(
                d.getId(), d.getDonorName(), d.getSourceOrDirect(), d.getStatus(), d.getCookedAt(), d.getSafeUntil(),
                clockRunning ? d.minutesLeft() : 0,
                d.getDayHighC(), d.getTotalServings(), d.getTotalWeightKg(),
                d.getCity(), d.getLat(), d.getLng(), d.getPickupNote(), d.getNgoId(),
                d.getItems().stream().map(DonationItemResponse::from).toList());
    }
}