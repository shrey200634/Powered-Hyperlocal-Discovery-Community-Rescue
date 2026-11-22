package com.communitynexus.inventory.repo;

import com.communitynexus.inventory.entity.Product;
import com.communitynexus.inventory.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStore_IdOrderByNameAsc(Long storeId);


    @Query("""
            select p from Product p
            join fetch p.store s
            where s.id in :storeIds
              and p.quantity > 0
              and (p.expiryDate is null or p.expiryDate >= :today)
              and p.nameKey like concat('%', :q, '%')
              and (:category is null or p.category = :category)
            """)
    List<Product> searchInStores(@Param("storeIds") Collection<Long> storeIds,
                                 @Param("q") String q,
                                 @Param("category") ProductCategory category,
                                 @Param("today") LocalDate today);

    //In-stock products of one store expiring between today and 'until' (soonest first)
    @Query("""
            select p from Product p
            where p.store.id = :storeId
              and p.quantity > 0
              and p.expiryDate between :today and :until
            order by p.expiryDate asc
            """)
    List<Product> findNearExpiry(@Param("storeId") Long storeId,
                                 @Param("today") LocalDate today,
                                 @Param("until") LocalDate until);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.quantity = p.quantity - :qty where p.id = :id and p.quantity >= :qty")
    int takeStock(@Param("id") Long productId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.quantity = p.quantity + :qty where p.id = :id")
    int returnStock(@Param("id") Long productId, @Param("qty") int qty);
}