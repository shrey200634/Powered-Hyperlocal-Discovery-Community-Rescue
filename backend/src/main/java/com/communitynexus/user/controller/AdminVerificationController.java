package com.communitynexus.user.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.user.dto.RejectRequest;
import com.communitynexus.user.dto.VerificationResponse;
import com.communitynexus.user.entity.VerificationStatus;
import com.communitynexus.user.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SecurityConfig already limits /admin/** to ADMIN.
 * @PreAuthorize is a second lock on the same door (defence in depth):
 * if someone later changes the URL rules by mistake, this still holds.
 */
@RestController
@RequestMapping("/admin/verifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final VerificationService verificationService;

    @GetMapping
    public List<VerificationResponse> list(
            @RequestParam(defaultValue = "PENDING") VerificationStatus status) {
        return verificationService.list(status);
    }

    @PostMapping("/{id}/approve")
    public VerificationResponse approve(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return verificationService.approve(id, admin.accountId());
    }

    @PostMapping("/{id}/reject")
    public VerificationResponse reject(@AuthenticationPrincipal AuthUser admin,
                                       @PathVariable Long id,
                                       @Valid @RequestBody RejectRequest req) {
        return verificationService.reject(id, admin.accountId(), req.note());
    }
}