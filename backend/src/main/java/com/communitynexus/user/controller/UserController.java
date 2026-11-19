package com.communitynexus.user.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.auth.Role;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.user.dto.*;
import com.communitynexus.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse myProfile(@AuthenticationPrincipal AuthUser me) {
        return profileService.getProfile(me.accountId());
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(@AuthenticationPrincipal AuthUser me,
                                           @Valid @RequestBody UpdateProfileRequest req) {
        return profileService.updateProfile(me.accountId(), req);
    }

    /** Ownership check (doc B5): you may read your own profile; only ADMIN may read others. */
    @GetMapping("/{accountId}")
    public ProfileResponse profile(@AuthenticationPrincipal AuthUser me, @PathVariable Long accountId) {
        if (!me.accountId().equals(accountId) && me.role() != Role.ADMIN) {
            throw new NexusException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only view your own profile");
        }
        return profileService.getProfile(accountId);
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody AddressRequest req) {
        return profileService.addAddress(me.accountId(), req);
    }

    @GetMapping("/me/addresses")
    public List<AddressResponse> myAddresses(@AuthenticationPrincipal AuthUser me) {
        return profileService.listAddresses(me.accountId());
    }
}