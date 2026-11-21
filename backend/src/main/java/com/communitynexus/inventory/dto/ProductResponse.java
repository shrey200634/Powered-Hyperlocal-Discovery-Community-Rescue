package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.Product;
import com.communitynexus.inventory.entity.ProductCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        Long storeId,
        String name,
        ProductCategory category,
        BigDecimal price,
        int quantity,
        LocalDate expiryDate,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal saveItPrice   // only when on the Second-Life Shelf
) {
    public static ProductResponse from(Product p) {
        return from(p, null);
    }

    public static ProductResponse from(Product p, BigDecimal saveItPrice) {
        return new ProductResponse(p.getId(), p.getStore().getId(), p.getName(), p.getCategory(),
                p.getPrice(), p.getQuantity(), p.getExpiryDate(), saveItPrice);
    }
}