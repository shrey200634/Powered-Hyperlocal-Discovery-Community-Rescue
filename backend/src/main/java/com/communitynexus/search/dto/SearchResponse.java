package com.communitynexus.search.dto;

import com.communitynexus.inventory.ProductCategory;
import com.communitynexus.inventory.dto.NearbyStoreResult;

import java.util.List;

public record SearchResponse(
        String query,
        List<String> understoodTerms,
        ProductCategory understoodCategory,
        boolean found,
        boolean canAskBazaar,        // doc C1: found = false "unlocks the Ask the Bazaar button"
        List<NearbyStoreResult> results
) {
}