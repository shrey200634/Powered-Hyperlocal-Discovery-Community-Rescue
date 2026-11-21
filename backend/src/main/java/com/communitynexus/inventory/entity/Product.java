package com.communitynexus.inventory.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.ProductCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "inv_products", indexes = {
        @Index(name = "idx_inv_products_store_cat", columnList = "store_id, category"),
        @Index(name = "idx_inv_products_name_key", columnList = "name_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_key", nullable = false, length = 120)
    private String nameKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCategory category;

    /** Money is ALWAYS BigDecimal, never double: 0.1 + 0.2 != 0.3 in double. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    /** Null for things that don't expire. Used by Second-Life Shelf in Part 4C. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    public static Product create(Store store, String name, ProductCategory category,
                                 BigDecimal price, int quantity, LocalDate expiryDate) {
        Product p = new Product();
        p.store = store;
        p.rename(name);
        p.category = category;
        p.price = price;
        p.setQuantity(quantity);
        p.expiryDate = expiryDate;
        return p;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw NexusException.badRequest("INVALID_QUANTITY", "Quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    public void changePrice(BigDecimal price) {
        this.price = price;
    }

    private void rename(String name) {
        this.name = name.trim();
        this.nameKey = keyOf(name);
    }

    public static String keyOf(String text) {
        return text.trim().toLowerCase();
    }

    public boolean isExpired(LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }
}