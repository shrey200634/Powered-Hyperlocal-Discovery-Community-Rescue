package com.communitynexus.auth.entity;

import com.communitynexus.auth.Role;
import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.common.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "auth_accounts",
        indexes = @Index(name = "idx_auth_accounts_phone", columnList = "phone", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA needs it; nobody else should use it
public class Account extends BaseEntity {

    @Column(nullable = false, length = 15)
    private String phone;                 // E.164 style, e.g. +919876543210

    @Enumerated(EnumType.STRING)          // stores "CITIZEN", not 0 — safe if enum order changes
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** The only way to create an account — keeps it always valid. */
    public static Account create(String phone, Role role, Language language) {
        Account a = new Account();
        a.phone = phone;
        a.role = role;
        a.language = language;
        a.status = AccountStatus.ACTIVE;
        return a;
    }

    public void markLoggedIn() {
        this.lastLoginAt = Instant.now();
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}