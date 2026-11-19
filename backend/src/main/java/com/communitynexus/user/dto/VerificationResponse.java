package com.communitynexus.user.dto;

import com.communitynexus.auth.Role;
import com.communitynexus.user.entity.Verification;
import com.communitynexus.user.entity.VerificationStatus;

import java.time.Instant;

/** Admin view. Phone is MASKED: the admin needs to identify the person, not see the full number. */
public record VerificationResponse(
        Long id,
        Long accountId,
        String name,
        Role role,
        String phoneMasked,
        VerificationStatus status,
        Instant requestedAt,
        Instant reviewedAt,
        String note
) {
    public static VerificationResponse from(Verification v) {
        var p = v.getProfile();
        return new VerificationResponse(
                v.getId(), p.getAccountId(), p.getName(), p.getRole(), mask(p.getPhone()),
                v.getStatus(), v.getCreatedAt(), v.getReviewedAt(), v.getNote());
    }

    /** "+919811111111" → "+91******1111" */
    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "+91******" + phone.substring(phone.length() - 4);
    }
}