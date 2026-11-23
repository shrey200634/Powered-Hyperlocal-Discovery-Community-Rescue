package com.communitynexus.donation.dto;

import com.communitynexus.donation.entity.Ngo;

public record NgoResponse(
        Long id,
        Long accountId,
        String name,
        String city,
        Double lat,
        Double lng,
        int capacityPerDay,
        int headcount,
        double acceptanceRate,
        boolean active
) {
    public static NgoResponse from(Ngo n) {
        return new NgoResponse(n.getId(), n.getAccountId(), n.getName(), n.getCity(), n.getLat(), n.getLng(),
                n.getCapacityPerDay(), n.getHeadcount(), n.getAcceptanceRate(), n.isActive());
    }
}