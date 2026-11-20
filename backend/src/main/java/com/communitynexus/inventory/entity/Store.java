package com.communitynexus.inventory.entity;

import com.communitynexus.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "inv_stores", indexes = {
        @Index(name = "idx_inv_stores_owner", columnList = "owner_id"),
        @Index(name = "idx_inv_stores_city", columnList = "city")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreCategory category;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "address_line", length = 300)
    private String addressLine;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_confirmed_at", nullable = false)
    private Instant lastConfirmedAt;

    public static Store create(Long ownerId, String name, StoreCategory category, String city,
                               double lat, double lng, String addressLine,
                               LocalTime opensAt, LocalTime closesAt) {
        Store s = new Store();
        s.ownerId = ownerId;
        s.name = name.trim();
        s.category = category;
        s.city = normalizeCity(city);
        s.lat = lat;
        s.lng = lng;
        s.addressLine = addressLine == null ? null : addressLine.trim();
        s.opensAt = opensAt;
        s.closesAt = closesAt;
        s.active = true;
        s.lastConfirmedAt = Instant.now();
        return s;
    }

    public static String normalizeCity(String city) {
        return city.trim().toLowerCase();
    }

    public boolean isOwnedBy(Long accountId) {
        return ownerId.equals(accountId);
    }

    public void confirmStock() {
        this.lastConfirmedAt = Instant.now();
    }

    public boolean isStale(Duration staleAfter) {
        return lastConfirmedAt.plus(staleAfter).isBefore(Instant.now());
    }

    public boolean isOpenAt(LocalTime time) {
        if (opensAt.equals(closesAt)) {
            return true;
        }
        if (opensAt.isBefore(closesAt)) {
            return !time.isBefore(opensAt) && time.isBefore(closesAt);
        }
        return !time.isBefore(opensAt) || time.isBefore(closesAt);
    }
}