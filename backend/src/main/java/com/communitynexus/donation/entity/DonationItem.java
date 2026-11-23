package com.communitynexus.donation.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.donation.FoodGroup;
import com.communitynexus.donation.FoodType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Doc C6 "donation_items": name, food_group, servings_estimate, weight_kg. */
@Entity
@Table(name = "don_donation_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DonationItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false)
    private FoodDonation donation;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_group", nullable = false, length = 15)
    private FoodGroup foodGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", nullable = false, length = 12)
    private FoodType foodType;

    @Column(name = "servings_estimate", nullable = false)
    private int servings;

    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    /** Only for PACKAGED items: the printed date. */
    @Column(name = "packaged_expiry")
    private LocalDate packagedExpiry;

    static DonationItem of(FoodDonation donation, String name, FoodGroup group, FoodType type,
                           int servings, BigDecimal weightKg, LocalDate packagedExpiry) {
        DonationItem i = new DonationItem();
        i.donation = donation;
        i.name = name.trim();
        i.foodGroup = group;
        i.foodType = type;
        i.servings = servings;
        i.weightKg = weightKg;
        i.packagedExpiry = packagedExpiry;
        return i;
    }
}