package com.communitynexus.auth.entity;

import com.communitynexus.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "auth_refresh_tokens",
        indexes = @Index(name = "idx_auth_rt_hash", columnList = "token_hash", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // same module, so a relation is OK
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** SHA-256 of the real token. We never store the token itself. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    public static RefreshToken create(Account account, String tokenHash, Instant expiresAt) {
        RefreshToken t = new RefreshToken();
        t.account = account;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.revoked = false;
        return t;
    }

    public boolean isUsable() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
    }
}