package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.Store;
import com.communitynexus.inventory.entity.StoreCategory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public record StoreResponse(
        Long id,
        String name,
        StoreCategory category,
        String city,
        Double lat,
        Double lng,
        String addressLine,
        LocalTime opensAt,
        LocalTime closesAt,
        boolean active,
        boolean openNow,
        boolean stale,            // true → hidden from search until the shopkeeper confirms stock
        Instant lastConfirmedAt
) {
    public static StoreResponse from(Store s, Duration staleAfter, ZoneId zone) {
        return new StoreResponse(
                s.getId(), s.getName(), s.getCategory(), s.getCity(), s.getLat(), s.getLng(),
                s.getAddressLine(), s.getOpensAt(), s.getClosesAt(), s.isActive(),
                s.isOpenAt(LocalTime.now(zone)), s.isStale(staleAfter), s.getLastConfirmedAt());
    }
}