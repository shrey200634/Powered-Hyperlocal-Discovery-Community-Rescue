package com.communitynexus.reservation.dto;

import com.communitynexus.reservation.entity.Reservation;
import com.communitynexus.reservation.entity.ReservationSource;
import com.communitynexus.reservation.entity.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
        Long id,
        Long productId,
        String productName,
        Long storeId,
        String storeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total,
        ReservationStatus status,
        ReservationSource source,
        Instant holdExpiresAt,
        Long secondsLeft,
        String pickupCode
) {
    public static ReservationResponse forCustomer(Reservation r) {
        return build(r, r.getPickupCode());
    }

    public static ReservationResponse forShop(Reservation r) {
        return build(r, null);
    }

    private static ReservationResponse build(Reservation r, String code) {
        long left = r.getStatus().isActive()
                ? Math.max(0, Duration.between(Instant.now(), r.getHoldExpiresAt()).toSeconds())
                : 0;
        return new ReservationResponse(
                r.getId(), r.getProductId(), r.getProductName(), r.getStoreId(), r.getStoreName(),
                r.getQuantity(), r.getUnitPrice(), r.getUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())),
                r.getStatus(), r.getSource(), r.getHoldExpiresAt(), left, code);
    }
}