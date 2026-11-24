package com.communitynexus.donation.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.donation.FoodGroup;
import com.communitynexus.donation.FoodType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Doc C6 "food_donations": donor_id, ngo_id, status, cooked_at, safe_until, photo_url, priority, lat, lng.
 * Index on (status, safe_until) as the doc says — the expiry sweeper and "nearby open donations" use it.
 */
@Entity
@Table(name = "don_food_donations", indexes = {
        @Index(name = "idx_don_fd_status_safe", columnList = "status, safe_until"),
        @Index(name = "idx_don_fd_donor", columnList = "donor_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodDonation extends BaseEntity {

    @Version
    private Long version;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "donor_name", nullable = false, length = 100)
    private String donorName;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    /** "Back gate, ask for Ramesh". Shown only to the accepted NGO and the volunteer. */
    @Column(name = "pickup_note", length = 200)
    private String pickupNote;

    @Column(name = "cooked_at", nullable = false)
    private Instant cookedAt;

    /** Doc C1: "safe_until is computed when the post is saved and checked on every later action." */
    @Column(name = "safe_until", nullable = false)
    private Instant safeUntil;

    @Column(name = "day_high_c", nullable = false)
    private double dayHighC;

    @Column(name = "total_servings", nullable = false)
    private int totalServings;

    @Column(name = "total_weight_kg", precision = 8, scale = 2)
    private BigDecimal totalWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DonationStatus status;

    @Column(name = "ngo_id")
    private Long ngoId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DonationItem> items = new ArrayList<>();

    public static FoodDonation post(Long donorId, String donorName, String city, double lat, double lng,
                                    String pickupNote, Instant cookedAt, Instant safeUntil, double dayHighC) {
        FoodDonation d = new FoodDonation();
        d.donorId = donorId;
        d.donorName = donorName;
        d.city = city.trim().toLowerCase();
        d.lat = lat;
        d.lng = lng;
        d.pickupNote = pickupNote == null ? null : pickupNote.trim();
        d.cookedAt = cookedAt;
        d.safeUntil = safeUntil;
        d.dayHighC = dayHighC;
        d.status = DonationStatus.POSTED;
        return d;
    }

    public void addItem(String name, FoodGroup group, FoodType type, int servings,
                        BigDecimal weightKg, LocalDate packagedExpiry) {
        items.add(DonationItem.of(this, name, group, type, servings, weightKg, packagedExpiry));
        totalServings += servings;
        if (weightKg != null) {
            totalWeightKg = (totalWeightKg == null ? BigDecimal.ZERO : totalWeightKg).add(weightKg);
        }
    }

    // ---------- Freshness Clock ----------

    public boolean isSafeAt(Instant when) {
        return when.isBefore(safeUntil);
    }

    public long minutesLeft() {
        return Math.max(0, Duration.between(Instant.now(), safeUntil).toMinutes());
    }

    // ---------- state changes ----------

    public void cancelByDonor() {
        if (!status.isOpen()) {
            throw NexusException.conflict("CANNOT_CANCEL", "This donation is already " + status);
        }
        this.status = DonationStatus.CANCELLED;
        this.closedAt = Instant.now();
    }
    /** Doc R4: checked on the server for every accept — whatever the screen shows. */
    public void accept(Long ngoId) {
        if (!status.isOpen()) {
            throw NexusException.conflict("ALREADY_ACCEPTED", "This food has already been taken by another NGO");
        }
        if (!isSafeAt(Instant.now())) {
            throw NexusException.conflict("DONATION_EXPIRED", "This food is past its safe time and can't be accepted");
        }
        this.status = DonationStatus.ACCEPTED;
        this.ngoId = ngoId;
        this.acceptedAt = Instant.now();
    }

    /** Freshness Clock ran out before pickup. */
    public boolean expireIfPastSafeTime() {
        boolean stillBeforePickup = status.isOpen() || status == DonationStatus.ACCEPTED;
        if (stillBeforePickup && !isSafeAt(Instant.now())) {
            this.status = DonationStatus.EXPIRED;
            this.closedAt = Instant.now();
            return true;
        }
        return false;
    }

    public boolean isAcceptedBy(Long ngoId) {
        return this.ngoId != null && this.ngoId.equals(ngoId);
    }

    public boolean isPostedBy(Long accountId) {
        return donorId.equals(accountId);
    }
}