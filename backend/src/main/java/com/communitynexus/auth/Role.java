package com.communitynexus.auth;

public enum Role {
    CITIZEN,
    SENIOR,
    SHOPKEEPER,
    DONOR,        // restaurant, event hall, mess
    NGO,
    VOLUNTEER,
    MUNICIPAL,
    ADMIN;

    public boolean isSelfSignupAllowed() {
        return this != MUNICIPAL && this != ADMIN;
    }
}