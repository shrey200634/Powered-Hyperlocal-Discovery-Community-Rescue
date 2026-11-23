package com.communitynexus.donation.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.donation.dto.DonationResponse;
import com.communitynexus.donation.dto.PostDonationRequest;
import com.communitynexus.donation.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    /** Doc C1: POST /donations */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DONOR')")
    public DonationResponse post(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PostDonationRequest req) {
        return donationService.post(me.accountId(), req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('DONOR')")
    public List<DonationResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return donationService.mine(me.accountId());
    }

    /** Doc C1: POST /donations/{id}/cancel */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DONOR')")
    public DonationResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return donationService.cancel(me.accountId(), id);
    }
}