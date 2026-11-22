package com.communitynexus.reservation.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.ReservedItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Doc C6 "reservations": user_id, product_id, store_id, status, hold_expires_at, source.
 * Store/product names are SNAPSHOTS so "My Orders" never needs Inventory's tables.
 */
@Entity
@Table(name = "res_reservations", indexes = {
        @Index(name = "idx_res_user_status", columnList = "user_id, status"),
        @Index(name = "idx_res_store_status", columnList = "store_id, status"),
        @Index(name = "idx_res_status_expiry", columnList = "status, hold_expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {


    @Version
    private Long version;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "store_owner_id", nullable = false)
    private Long storeOwnerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReservationSource source;

    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    /** 4 digits the customer says at the counter (doc 03: "Pickup code confirms it"). */
    @Column(name = "pickup_code", nullable = false, length = 4)
    private String pickupCode;

    @Column(name = "closed_at")
    private Instant closedAt;

    public static Reservation hold(Long userId, ReservedItem item, int quantity, ReservationSource source,
                                   Instant holdExpiresAt, String pickupCode) {
        Reservation r = new Reservation();
        r.userId = userId;
        r.productId = item.productId();
        r.productName = item.productName();
        r.storeId = item.storeId();
        r.storeName = item.storeName();
        r.storeOwnerId = item.storeOwnerId();
        r.quantity = quantity;
        r.unitPrice = item.unitPrice();
        r.status = ReservationStatus.PENDING;
        r.source = source;
        r.holdExpiresAt = holdExpiresAt;
        r.pickupCode = pickupCode;
        return r;
    }

    // ---------- state machine ----------

    public void confirmByShop() {
        requireStatus(ReservationStatus.PENDING);
        this.status = ReservationStatus.CONFIRMED;
    }

    public void pickUp(String code) {
        requireActive();
        if (isPastHold()) {
            throw NexusException.conflict("HOLD_EXPIRED", "This hold has expired");
        }
        if (!pickupCode.equals(code)) {
            throw NexusException.badRequest("WRONG_PICKUP_CODE", "The pickup code does not match");
        }
        close(ReservationStatus.PICKED_UP);
    }

    public void cancel() {
        requireActive();
        close(ReservationStatus.CANCELLED);
    }

    public void expire() {
        requireActive();
        close(ReservationStatus.EXPIRED);
    }

    public boolean isPastHold() {
        return holdExpiresAt.isBefore(Instant.now());
    }

    public boolean isOwnedBy(Long accountId) {
        return userId.equals(accountId);
    }

    public boolean isForShopOwner(Long accountId) {
        return storeOwnerId.equals(accountId);
    }

    private void close(ReservationStatus to) {
        this.status = to;
        this.closedAt = Instant.now();
    }

    private void requireActive() {
        if (!status.isActive()) {
            throw NexusException.conflict("RESERVATION_CLOSED", "This reservation is already " + status);
        }
    }

    private void requireStatus(ReservationStatus expected) {
        if (status != expected) {
            throw NexusException.conflict("INVALID_STATE", "Reservation is " + status + ", expected " + expected);
        }
    }
}