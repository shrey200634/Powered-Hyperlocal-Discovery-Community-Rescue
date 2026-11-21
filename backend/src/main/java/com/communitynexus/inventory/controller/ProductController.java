package com.communitynexus.inventory.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.inventory.dto.*;
import com.communitynexus.inventory.entity.ProductCategory;
import com.communitynexus.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ProductResponse add(@AuthenticationPrincipal AuthUser me,
                               @PathVariable Long storeId,
                               @Valid @RequestBody CreateProductRequest req) {
        return productService.addProduct(me.accountId(), storeId, req);
    }

    @GetMapping("/stores/{storeId}/products")
    public List<ProductResponse> list(@PathVariable Long storeId) {
        return productService.listForStore(storeId);
    }

    /** Doc C1: PATCH /products/{id}/stock */
    @PatchMapping("/products/{id}/stock")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ProductResponse updateStock(@AuthenticationPrincipal AuthUser me,
                                       @PathVariable Long id,
                                       @Valid @RequestBody StockUpdateRequest req) {
        return productService.updateStock(me.accountId(), id, req);
    }

    @GetMapping("/products/nearby")
    public List<NearbyStoreResult> nearby(@RequestParam double lat,
                                          @RequestParam double lng,
                                          @RequestParam(required = false) Integer radius,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) ProductCategory category,
                                          @RequestParam(required = false) String city,
                                          @RequestParam(defaultValue = "false") boolean deals) {
        return productService.nearby(lat, lng, radius, q, category, city, deals);
    }
}