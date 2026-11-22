package com.communitynexus.reservation.event;

import java.time.Instant;


public record ReservationCreatedEvent(
        Long reservationId,
        Long userId,
        Long storeId,
        Long storeOwnerId,
        Long productId,
        String productName,
        int quantity,
        Instant holdExpiresAt,
        boolean deliveryWanted,
        Instant occurredAt
) {
}