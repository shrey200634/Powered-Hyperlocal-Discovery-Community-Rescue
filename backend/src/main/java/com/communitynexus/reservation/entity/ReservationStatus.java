package com.communitynexus.reservation.entity;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    PICKED_UP,
    EXPIRED,
    CANCELLED;

    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }
}