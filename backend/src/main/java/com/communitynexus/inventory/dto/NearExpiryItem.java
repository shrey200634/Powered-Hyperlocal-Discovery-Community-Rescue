package com.communitynexus.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NearExpiryItem(
        Long productId,
        String name,
        int quantity,
        BigDecimal price,
        LocalDate expiryDate,
        long daysLeft,
        BigDecimal suggestedSavePrice,
        boolean alreadyOnShelf
) {
}