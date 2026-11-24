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
 * source_offer_id is UNIQUE: one Second-Life offer can never create two donations (idempotent bridge).
 */
@Entity
@Table(name = "don_food_donations", indexes = {
        @Index(name = "idx_don_fd_status_safe", columnList = "status, safe_until"),
        @Index(name = "idx_don_fd_donor", columnList = "donor_id"),
        @Index(name = "idx_don_fd_source_offer", columnList = "source_offer_id", unique = true)
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

    @Column(name = "pickup_note", length = 200)
    private String pickupNote;

    /** For shelf items this is the moment they came off the shelf. */
    @Column(name = "cooked_at", nullable = false)
    private Instant cookedAt;

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

    /** Null on rows created before 7C → treated as DIRECT. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DonationSource source;

    @Column(name = "source_offer_id")
    private Long sourceOfferId;

    @Column(name = "ngo_id")
    private Long ngoId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DonationItem> items = new ArrayList<>();

    // =====================================================================
    // Creation
    // =====================================================================

    /** A kitchen / hall posts cooked surplus. */
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
        d.source = DonationSource.DIRECT;
        return d;
    }

    /** The BRIDGE: unsold near-expiry shop items become a donation. The shopkeeper is the donor. */
    public static FoodDonation fromShelf(Long shopOwnerId, String storeName, String city, double lat, double lng,
                                         Instant safeUntil, double dayHighC, Long offerId) {
        FoodDonation d = post(shopOwnerId, storeName, city, lat, lng,
                "Ask at the counter - Second-Life Shelf items", Instant.now(), safeUntil, dayHighC);
        d.source = DonationSource.SHELF;
        d.sourceOfferId = offerId;
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

    // =====================================================================
    // Freshness Clock
    // =====================================================================

    public boolean isSafeAt(Instant when) {
        return when.isBefore(safeUntil);
    }

    public long minutesLeft() {
        return Math.max(0, Duration.between(Instant.now(), safeUntil).toMinutes());
    }

    // =====================================================================
    // State changes
    // =====================================================================

    public void cancelByDonor() {
        if (!status.isOpen()) {
            throw NexusException.conflict("CANNOT_CANCEL", "This donation is already " + status);
        }
        this.status = DonationStatus.CANCELLED;
        this.closedAt = Instant.now();
    }

    /** Throws the RIGHT reason if this donation can't be accepted now (doc R4). */
    public void checkCanBeAccepted() {
        switch (status) {
            case POSTED, MATCHED -> {
                // open — continue to the safety check below
            }
            case EXPIRED -> throw NexusException.conflict("DONATION_EXPIRED",
                    "This food is past its safe time and can't be accepted");
            case CANCELLED -> throw NexusException.conflict("DONATION_CANCELLED",
                    "The donor cancelled this donation");
            default -> throw NexusException.conflict("ALREADY_ACCEPTED",
                    "This food has already been taken by another NGO");
        }
        if (!isSafeAt(Instant.now())) {
            throw NexusException.conflict("DONATION_EXPIRED",
                    "This food is past its safe time and can't be accepted");
        }
    }

    public void accept(Long ngoId) {
        checkCanBeAccepted();
        this.status = DonationStatus.ACCEPTED;
        this.ngoId = ngoId;
        this.acceptedAt = Instant.now();
    }

    public boolean expireIfPastSafeTime() {
        boolean stillBeforePickup = status.isOpen() || status == DonationStatus.ACCEPTED;
        if (stillBeforePickup && !isSafeAt(Instant.now())) {
            this.status = DonationStatus.EXPIRED;
            this.closedAt = Instant.now();
            return true;
        }
        return false;
    }

    // =====================================================================
    // Who is involved
    // =====================================================================

    public boolean isPostedBy(Long accountId) {
        return donorId.equals(accountId);
    }

    public boolean isAcceptedBy(Long ngoId) {
        return this.ngoId != null && this.ngoId.equals(ngoId);
    }

    public DonationSource getSourceOrDirect() {
        return source == null ? DonationSource.DIRECT : source;
    }
}