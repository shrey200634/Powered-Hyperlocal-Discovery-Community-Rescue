package com.communitynexus.donation.entity;

import com.communitynexus.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "don_ngos", indexes = {
        @Index(name = "idx_don_ngos_account", columnList = "account_id", unique = true),
        @Index(name = "idx_don_ngos_city", columnList = "city")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ngo extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    /** Servings this shelter can take in one day. */
    @Column(name = "capacity_per_day", nullable = false)
    private int capacityPerDay;

    /** People served daily — used for Thali weekly targets. */
    @Column(nullable = false)
    private int headcount;

    /** How often this NGO accepts AND collects. Starts at 1.0, used in matching (doc C5, w4). */
    @Column(name = "acceptance_rate", nullable = false)
    private double acceptanceRate;

    @Column(nullable = false)
    private boolean active;

    public static Ngo create(Long accountId, String name, String city, double lat, double lng,
                             int capacityPerDay, int headcount) {
        Ngo n = new Ngo();
        n.accountId = accountId;
        n.name = name.trim();
        n.city = city.trim().toLowerCase();
        n.lat = lat;
        n.lng = lng;
        n.capacityPerDay = capacityPerDay;
        n.headcount = headcount;
        n.acceptanceRate = 1.0;
        n.active = true;
        return n;
    }

    public void updateCapacity(Integer capacityPerDay, Integer headcount) {
        if (capacityPerDay != null) this.capacityPerDay = capacityPerDay;
        if (headcount != null) this.headcount = headcount;
    }
}