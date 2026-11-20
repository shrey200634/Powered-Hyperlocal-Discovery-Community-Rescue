package com.communitynexus.inventory.event;

import java.time.Instant;


public record StockUpdatedEvent(
        Long storeId,
        Long productId,
        String productName,
        String category,
        int quantity,
        String source,
        Instant occurredAt
) {
}