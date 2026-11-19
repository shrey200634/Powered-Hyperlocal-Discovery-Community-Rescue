package com.communitynexus.user.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.common.exception.NexusException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One review request. Kept as history: who approved/rejected, when, and why. */
@Entity
@Table(name = "user_verifications",
        indexes = @Index(name = "idx_user_verif_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VerificationStatus status;

    @Column(name = "reviewed_by")
    private Long reviewedBy;          // admin's accountId

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(length = 300)
    private String note;

    public static Verification pending(UserProfile profile) {
        Verification v = new Verification();
        v.profile = profile;
        v.status = VerificationStatus.PENDING;
        return v;
    }

    public void approve(Long adminAccountId) {
        requirePending();
        this.status = VerificationStatus.APPROVED;
        this.reviewedBy = adminAccountId;
        this.reviewedAt = Instant.now();
        this.profile.markVerified();
    }

    public void reject(Long adminAccountId, String note) {
        requirePending();
        this.status = VerificationStatus.REJECTED;
        this.reviewedBy = adminAccountId;
        this.reviewedAt = Instant.now();
        this.note = note;
    }

    private void requirePending() {
        if (status != VerificationStatus.PENDING) {
            throw NexusException.conflict("ALREADY_REVIEWED", "This verification was already " + status);
        }
    }
}