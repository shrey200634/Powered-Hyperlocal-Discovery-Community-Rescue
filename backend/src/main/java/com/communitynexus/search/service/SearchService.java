package com.communitynexus.search.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.InventoryCatalog;
import com.communitynexus.inventory.dto.NearbyStoreResult;
import com.communitynexus.inventory.dto.ProductResponse;
import com.communitynexus.search.dto.ParsedQuery;
import com.communitynexus.search.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final QueryParser parser;
    private final InventoryCatalog catalog;     // Inventory's PUBLIC API

    public SearchResponse search(String text, double lat, double lng, Integer radiusM, String city) {
        ParsedQuery pq = parser.parse(text);
        if (pq.isEmpty()) {
            throw NexusException.badRequest("EMPTY_QUERY", "Please tell us what you are looking for");
        }

        // No item word but a category ("dawa") → browse the whole category
        List<String> terms = pq.terms().isEmpty() ? List.of("") : pq.terms();

        // One Inventory call per term, merged by store
        Map<Long, NearbyStoreResult> byStore = new LinkedHashMap<>();
        for (String term : terms) {
            for (NearbyStoreResult r : catalog.findNearby(lat, lng, radiusM, term, pq.category(), city)) {
                byStore.merge(r.storeId(), r, SearchService::mergeProducts);
            }
        }

        List<NearbyStoreResult> results = byStore.values().stream()
                .sorted(Comparator.comparing(NearbyStoreResult::openNow).reversed()
                        .thenComparingInt(NearbyStoreResult::distanceMeters))
                .toList();

        boolean found = !results.isEmpty();
        return new SearchResponse(text, pq.terms(), pq.category(), found, !found, results);
    }

    /** Same shop matched two terms (e.g. "pav" and "bread") → one entry, products combined. */
    private static NearbyStoreResult mergeProducts(NearbyStoreResult a, NearbyStoreResult b) {
        Map<Long, ProductResponse> products = new LinkedHashMap<>();
        a.products().forEach(p -> products.put(p.id(), p));
        b.products().forEach(p -> products.putIfAbsent(p.id(), p));
        return new NearbyStoreResult(a.storeId(), a.storeName(), a.storeCategory(), a.lat(), a.lng(),
                a.distanceMeters(), a.openNow(), List.copyOf(products.values()));
    }
}