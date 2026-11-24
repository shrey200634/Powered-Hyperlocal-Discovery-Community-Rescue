package com.communitynexus.donation.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.donation.dto.DonationResponse;
import com.communitynexus.donation.dto.NearbyDonationResponse;
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

    // ---- donor ----

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

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DONOR')")
    public DonationResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return donationService.cancel(me.accountId(), id);
    }

    // ---- NGO ----

    /** Doc C1: GET /donations/nearby */
    @GetMapping("/nearby")
    @PreAuthorize("hasRole('NGO')")
    public List<NearbyDonationResponse> nearby(@AuthenticationPrincipal AuthUser me) {
        return donationService.nearbyForNgo(me.accountId());
    }

    /** Doc C1: POST /donations/{id}/accept */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('NGO')")
    public DonationResponse accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return donationService.accept(me.accountId(), id);
    }

    @GetMapping("/accepted")
    @PreAuthorize("hasRole('NGO')")
    public List<DonationResponse> accepted(@AuthenticationPrincipal AuthUser me) {
        return donationService.acceptedByMe(me.accountId());
    }

    // ---- donor / accepting NGO / admin ----

    @GetMapping("/{id}")
    public DonationResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return donationService.get(me, id);
    }
}