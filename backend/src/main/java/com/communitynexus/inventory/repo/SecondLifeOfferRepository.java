package com.communitynexus.inventory.repo;

import com.communitynexus.inventory.ProductCategory;
import com.communitynexus.inventory.entity.SecondLifeOffer;
import com.communitynexus.inventory.entity.SecondLifeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SecondLifeOfferRepository extends JpaRepository<SecondLifeOffer, Long> {

    Optional<SecondLifeOffer> findByProduct_IdAndStatus(Long productId, SecondLifeStatus status);

    boolean existsByProduct_IdAndStatus(Long productId, SecondLifeStatus status);

    List<SecondLifeOffer> findByProduct_IdInAndStatus(Collection<Long> productIds, SecondLifeStatus status);

    /** Food offers still at save-it price whose donate time has come. Uses the (status, donate_after) index. */
    @Query("""
            select o.id from SecondLifeOffer o
            where o.status = :status
              and o.donateAfter <= :now
              and o.product.category in :categories
            """)
    List<Long> findIdsDueForDonation(@Param("status") SecondLifeStatus status,
                                     @Param("categories") Collection<ProductCategory> categories,
                                     @Param("now") Instant now);
}