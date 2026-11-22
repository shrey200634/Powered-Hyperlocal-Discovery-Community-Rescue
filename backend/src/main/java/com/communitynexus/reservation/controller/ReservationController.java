package com.communitynexus.reservation.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.reservation.dto.CreateReservationRequest;
import com.communitynexus.reservation.dto.PickupRequest;
import com.communitynexus.reservation.dto.ReservationResponse;
import com.communitynexus.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    // ---- customer (citizen / senior) ----

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITIZEN', 'SENIOR')")
    public ReservationResponse create(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody CreateReservationRequest req) {
        return service.create(me.accountId(), req);
    }

    @GetMapping("/reservations/mine")
    public List<ReservationResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return service.mine(me.accountId());
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.get(me, id);
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.cancel(me.accountId(), id);
    }

    // ---- shopkeeper ----

    @GetMapping("/stores/{storeId}/reservations")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public List<ReservationResponse> forStore(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId) {
        return service.activeForStore(me.accountId(), storeId);
    }

    @PostMapping("/reservations/{id}/confirm")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ReservationResponse confirm(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.confirmByShop(me.accountId(), id);
    }

    @PostMapping("/reservations/{id}/confirm-pickup")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ReservationResponse confirmPickup(@AuthenticationPrincipal AuthUser me,
                                             @PathVariable Long id,
                                             @Valid @RequestBody PickupRequest req) {
        return service.confirmPickup(me.accountId(), id, req.code());
    }
}