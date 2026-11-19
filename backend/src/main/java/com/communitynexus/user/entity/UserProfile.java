package com.communitynexus.user.entity;

import com.communitynexus.auth.Role;
import com.communitynexus.common.Language;
import com.communitynexus.common.crypto.EncryptedStringConverter;
import com.communitynexus.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Doc C6 "users" table. One profile per auth account.
 * account_id is just a number — NO foreign key to auth_accounts,
 * because "no foreign keys cross a service boundary" (C6).
 */
@Entity
@Table(name = "user_profiles",
        indexes = @Index(name = "idx_user_profiles_account", columnList = "account_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Language language;

    /** Encrypted in DB (doc B5). Code sees a normal string. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_enc", nullable = false, length = 120)
    private String phone;

    /** Big buttons, larger text, voice-first (doc B4: a setting, not a separate app). */
    @Column(name = "senior_mode", nullable = false)
    private boolean seniorMode;

    /** No images or maps, for slow networks (doc 06). */
    @Column(name = "lite_mode", nullable = false)
    private boolean liteMode;

    /** Verified badge. Only meaningful for roles where role.requiresVerification(). */
    @Column(nullable = false)
    private boolean verified;

    /** Ward for the impact ledger and scoreboard. Set later from the address. */
    @Column(name = "ward_id")
    private Long wardId;

    public static UserProfile create(Long accountId, String name, Role role, Language language, String phone) {
        UserProfile p = new UserProfile();
        p.accountId = accountId;
        p.name = name;
        p.role = role;
        p.language = language;
        p.phone = phone;
        p.seniorMode = (role == Role.SENIOR);   // seniors start in Senior Mode
        p.liteMode = false;
        p.verified = false;
        return p;
    }

    /** Partial update: null means "don't change". */
    public void updateSettings(String name, Language language, Boolean seniorMode, Boolean liteMode) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        if (language != null) this.language = language;
        if (seniorMode != null) this.seniorMode = seniorMode;
        if (liteMode != null) this.liteMode = liteMode;
    }

    void markVerified() {          // package-private: only Verification.approve() may call it
        this.verified = true;
    }

    public void assignWard(Long wardId) {
        this.wardId = wardId;
    }
}