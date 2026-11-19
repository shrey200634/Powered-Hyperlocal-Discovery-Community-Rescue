package com.communitynexus.user.repo;

import com.communitynexus.user.entity.Verification;
import com.communitynexus.user.entity.VerificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    /** Loads the profile in the same query (avoids N+1 when listing the admin queue). */
    @EntityGraph(attributePaths = "profile")
    List<Verification> findByStatusOrderByCreatedAtAsc(VerificationStatus status);
}