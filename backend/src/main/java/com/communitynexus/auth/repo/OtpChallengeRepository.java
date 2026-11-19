package com.communitynexus.auth.repo;

import com.communitynexus.auth.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    /** The latest unused OTP for this phone. Older ones are ignored. */
    Optional<OtpChallenge> findTopByPhoneAndConsumedFalseOrderByCreatedAtDesc(String phone);
}