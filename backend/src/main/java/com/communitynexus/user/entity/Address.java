package com.communitynexus.user.entity;

import com.communitynexus.common.crypto.EncryptedStringConverter;
import com.communitynexus.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Doc C6 "addresses": line encrypted; lat/lng kept plain so we can measure distance.
 * Doc 12: exact address is shown only to the assigned volunteer.
 */
@Entity
@Table(name = "user_addresses",
        indexes = @Index(name = "idx_user_addr_profile", columnList = "profile_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AddressLabel label;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "line_enc", nullable = false, length = 1024)
    private String line;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    public static Address create(UserProfile profile, AddressLabel label, String line,
                                 double lat, double lng, boolean makeDefault) {
        Address a = new Address();
        a.profile = profile;
        a.label = label;
        a.line = line.trim();
        a.lat = lat;
        a.lng = lng;
        a.defaultAddress = makeDefault;
        return a;
    }

    public void clearDefault() {
        this.defaultAddress = false;
    }
}