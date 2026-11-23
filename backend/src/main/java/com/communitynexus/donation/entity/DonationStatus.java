package com.communitynexus.donation.entity;


public enum DonationStatus {
    POSTED,
    MATCHED,
    ACCEPTED,
    PICKED_UP,
    DELIVERED,
    CLOSED,
    OPEN_TABLE,
    EXPIRED,
    CANCELLED;

    public boolean isOpen() {
        return this == POSTED || this == MATCHED;
    }
}