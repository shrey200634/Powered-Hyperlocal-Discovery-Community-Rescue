package com.communitynexus.inventory.event;

import java.time.Instant;
import java.time.LocalDate;


public record ShelfReadyToDonateEvent(
        Long offerId,
        Long storeId,
        String storeName,
        Long storeOwnerId,       // the shopkeeper becomes the donor
        String city,
        double lat,
        double lng,
        Long productId,
        String productName,
        String category,         // DAIRY / BAKERY / GROCERY
        int quantity,            // only the UNSOLD units
        LocalDate expiryDate,
        Instant occurredAt
) {
}