package com.communitynexus.reservation.event;

import java.time.Instant;

public record ReservationPickedUpEvent(
        Long reservationId,
        Long userId,
        Long storeId,
        Long productId,
        int quantity,
        Instant occurredAt
) {
}