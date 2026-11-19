package com.communitynexus.auth.entity;

import com.communitynexus.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "auth_otp_challenges",
        indexes = @Index(name = "idx_auth_otp_phone", columnList = "phone"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpChallenge extends BaseEntity {

    public static final int MAX_ATTEMPTS = 5;

    @Column(nullable = false, length = 15)
    private String phone;

    /** BCrypt hash of the 6-digit OTP. */
    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean consumed;

    public static OtpChallenge create(String phone, String otpHash, Instant expiresAt) {
        OtpChallenge c = new OtpChallenge();
        c.phone = phone;
        c.otpHash = otpHash;
        c.expiresAt = expiresAt;
        c.attempts = 0;
        c.consumed = false;
        return c;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean hasAttemptsLeft() {
        return attempts < MAX_ATTEMPTS;
    }

    public void recordFailedAttempt() {
        this.attempts++;
    }

    public void consume() {
        this.consumed = true;
    }
}