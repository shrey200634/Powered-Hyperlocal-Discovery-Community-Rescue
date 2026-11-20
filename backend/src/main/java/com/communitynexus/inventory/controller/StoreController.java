package com.communitynexus.inventory.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.inventory.dto.CreateStoreRequest;
import com.communitynexus.inventory.dto.StoreResponse;
import com.communitynexus.inventory.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public StoreResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateStoreRequest req) {
        return storeService.create(me.accountId(), req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public List<StoreResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return storeService.myStores(me.accountId());
    }

    @GetMapping("/{id}")
    public StoreResponse get(@PathVariable Long id) {
        return storeService.get(id);
    }

    @PostMapping("/{id}/confirm-stock")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public StoreResponse confirmStock(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return storeService.confirmStock(me.accountId(), id);
    }
}