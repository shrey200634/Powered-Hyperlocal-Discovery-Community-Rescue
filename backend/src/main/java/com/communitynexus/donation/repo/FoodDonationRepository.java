package com.communitynexus.donation.repo;

import com.communitynexus.donation.entity.DonationStatus;
import com.communitynexus.donation.entity.FoodDonation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FoodDonationRepository extends JpaRepository<FoodDonation, Long> {

    @EntityGraph(attributePaths = "items")
    List<FoodDonation> findByDonorIdOrderByIdDesc(Long donorId);

    @EntityGraph(attributePaths = "items")
    Optional<FoodDonation> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<FoodDonation> findByNgoIdOrderByIdDesc(Long ngoId);

    /** Open donations in a city that are still safe. Uses the (status, safe_until) index. */
    @EntityGraph(attributePaths = "items")
    List<FoodDonation> findByCityAndStatusInAndSafeUntilAfter(String city,
                                                              Collection<DonationStatus> statuses,
                                                              Instant now);

    /** For the sweeper: not yet picked up, and the clock has run out. */
    @Query("select d.id from FoodDonation d where d.status in :statuses and d.safeUntil < :now")
    List<Long> findIdsPastSafeTime(@Param("statuses") Collection<DonationStatus> statuses,
                                   @Param("now") Instant now);

    /** Servings this NGO has already taken today (capacity check). */
    @Query("""
            select coalesce(sum(d.totalServings), 0) from FoodDonation d
            where d.ngoId = :ngoId and d.acceptedAt >= :since and d.status in :statuses
            """)
    long servingsAcceptedSince(@Param("ngoId") Long ngoId,
                               @Param("since") Instant since,
                               @Param("statuses") Collection<DonationStatus> statuses);
}