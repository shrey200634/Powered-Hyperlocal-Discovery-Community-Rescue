package com.communitynexus.search.dto;

import com.communitynexus.inventory.ProductCategory;
import com.communitynexus.inventory.dto.NearbyStoreResult;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResponse(
        String query,
        boolean understood,              // false → voice app asks again (doc C5)
        List<String> understoodTerms,
        ProductCategory understoodCategory,
        boolean found,
        boolean canAskBazaar,
        String say,                      // voice only: sentence to read aloud
        List<NearbyStoreResult> results
) {
    public static SearchResponse notUnderstood(String query, String say) {
        return new SearchResponse(query, false, List.of(), null, false, false, say, List.of());
    }
}