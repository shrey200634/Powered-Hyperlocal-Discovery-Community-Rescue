package com.communitynexus.donation.repo;

import com.communitynexus.donation.entity.FoodDonation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodDonationRepository extends JpaRepository<FoodDonation, Long> {

    @EntityGraph(attributePaths = "items")
    List<FoodDonation> findByDonorIdOrderByIdDesc(Long donorId);

    @EntityGraph(attributePaths = "items")
    Optional<FoodDonation> findWithItemsById(Long id);
}