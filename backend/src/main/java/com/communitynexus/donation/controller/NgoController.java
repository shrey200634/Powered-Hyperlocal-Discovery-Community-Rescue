package com.communitynexus.donation.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.donation.dto.CreateNgoRequest;
import com.communitynexus.donation.dto.NgoResponse;
import com.communitynexus.donation.dto.UpdateNgoRequest;
import com.communitynexus.donation.service.NgoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ngos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('NGO')")
public class NgoController {

    private final NgoService ngoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NgoResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateNgoRequest req) {
        return ngoService.create(me.accountId(), req);
    }

    @GetMapping("/mine")
    public NgoResponse mine(@AuthenticationPrincipal AuthUser me) {
        return ngoService.mine(me.accountId());
    }

    /** Doc 05: the NGO "sets capacity". */
    @PatchMapping("/mine")
    public NgoResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateNgoRequest req) {
        return ngoService.updateMine(me.accountId(), req);
    }
}