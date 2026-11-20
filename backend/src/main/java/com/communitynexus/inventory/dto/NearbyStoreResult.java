package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.StoreCategory;

import java.util.List;

public record NearbyStoreResult(
        Long storeId,
        String storeName,
        StoreCategory storeCategory,
        Double lat,
        Double lng,
        int distanceMeters,
        boolean openNow,
        List<ProductResponse> products
) {
}