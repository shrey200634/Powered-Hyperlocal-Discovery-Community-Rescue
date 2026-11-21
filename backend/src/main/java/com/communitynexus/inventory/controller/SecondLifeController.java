package com.communitynexus.inventory.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.inventory.dto.FlagRequest;
import com.communitynexus.inventory.dto.NearExpiryItem;
import com.communitynexus.inventory.dto.SecondLifeOfferResponse;
import com.communitynexus.inventory.service.SecondLifeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHOPKEEPER')")
public class SecondLifeController {

    private final SecondLifeService secondLifeService;

    @GetMapping("/stores/{storeId}/near-expiry")
    public List<NearExpiryItem> nearExpiry(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId) {
        return secondLifeService.nearExpiry(me.accountId(), storeId);
    }

    /** Body is optional: send {} to accept the suggested save-it price. */
    @PostMapping("/products/{id}/second-life")
    @ResponseStatus(HttpStatus.CREATED)
    public SecondLifeOfferResponse flag(@AuthenticationPrincipal AuthUser me,
                                        @PathVariable Long id,
                                        @Valid @RequestBody(required = false) FlagRequest req) {
        return secondLifeService.flag(me.accountId(), id, req);
    }
}