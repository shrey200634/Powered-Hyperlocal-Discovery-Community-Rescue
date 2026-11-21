package com.communitynexus.inventory.repo;

import com.communitynexus.inventory.entity.SecondLifeOffer;
import com.communitynexus.inventory.entity.SecondLifeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SecondLifeOfferRepository extends JpaRepository<SecondLifeOffer, Long> {

    Optional<SecondLifeOffer> findByProduct_IdAndStatus(Long productId, SecondLifeStatus status);

    boolean existsByProduct_IdAndStatus(Long productId, SecondLifeStatus status);

    /** For search results: which of these products currently have a save-it price? */
    List<SecondLifeOffer> findByProduct_IdInAndStatus(Collection<Long> productIds, SecondLifeStatus status);
}