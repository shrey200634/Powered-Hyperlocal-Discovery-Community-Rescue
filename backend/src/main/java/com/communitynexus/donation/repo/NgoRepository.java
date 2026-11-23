package com.communitynexus.donation.repo;

import com.communitynexus.donation.entity.Ngo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NgoRepository extends JpaRepository<Ngo, Long> {

    Optional<Ngo> findByAccountId(Long accountId);

    boolean existsByAccountId(Long accountId);

    List<Ngo> findByActiveTrue();
}