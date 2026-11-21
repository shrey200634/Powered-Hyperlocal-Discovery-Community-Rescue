package com.communitynexus.inventory;

import com.communitynexus.inventory.dto.NearbyStoreResult;
import com.communitynexus.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class InventoryCatalog {

    private final ProductService productService;

    /** Shops near (lat,lng) that have products matching one term, optionally in one category. */
    public List<NearbyStoreResult> findNearby(double lat, double lng, Integer radiusM,
                                              String term, ProductCategory category, String city) {
        return productService.nearby(lat, lng, radiusM, term, category, city, false);
    }
}