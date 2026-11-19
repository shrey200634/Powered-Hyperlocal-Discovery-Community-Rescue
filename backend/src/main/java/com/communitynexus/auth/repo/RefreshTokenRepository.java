package com.communitynexus.auth.repo;

import com.communitynexus.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** "Log out from all devices" — also used if we detect a stolen token. */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.account.id = :accountId and t.revoked = false")
    int revokeAllForAccount(@Param("accountId") Long accountId);
}