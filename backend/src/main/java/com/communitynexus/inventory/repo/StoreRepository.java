package com.communitynexus.inventory.repo;

import com.communitynexus.inventory.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByOwnerIdOrderByIdAsc(Long ownerId);

    /** Used to rebuild the Redis geo index at startup. */
    List<Store> findByActiveTrue();
}