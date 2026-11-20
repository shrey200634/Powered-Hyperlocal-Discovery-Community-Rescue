package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.Product;
import com.communitynexus.inventory.entity.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        Long storeId,
        String name,
        ProductCategory category,
        BigDecimal price,
        int quantity,
        LocalDate expiryDate
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getStore().getId(), p.getName(), p.getCategory(),
                p.getPrice(), p.getQuantity(), p.getExpiryDate());
    }
}