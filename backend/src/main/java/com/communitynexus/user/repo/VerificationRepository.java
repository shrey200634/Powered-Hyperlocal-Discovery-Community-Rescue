package com.communitynexus.user.repo;

import com.communitynexus.user.entity.Verification;
import com.communitynexus.user.entity.VerificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    /** Loads the profile in the same query, so listing the admin queue doesn't do 1 extra query per row (N+1). */
    @EntityGraph(attributePaths = "profile")
    List<Verification> findByStatusOrderByCreatedAtAsc(VerificationStatus status);
}