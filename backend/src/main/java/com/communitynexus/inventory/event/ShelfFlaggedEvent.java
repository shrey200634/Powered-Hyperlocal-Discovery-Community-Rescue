package com.communitynexus.inventory.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;


public record ShelfFlaggedEvent(
        Long offerId,
        Long storeId,
        Long productId,
        String productName,
        String category,
        int quantity,
        LocalDate expiryDate,
        BigDecimal savePrice,
        Instant donateAfter,
        Instant occurredAt
) {
}