package com.communitynexus.user.dto;

import com.communitynexus.auth.Role;
import com.communitynexus.common.Language;
import com.communitynexus.user.entity.UserProfile;

public record ProfileResponse(
        Long accountId,
        String name,
        Role role,
        Language language,
        String phone,
        boolean seniorMode,
        boolean liteMode,
        boolean verified,
        boolean needsVerification   // true for NGO / SHOPKEEPER / VOLUNTEER → app shows "badge pending"
) {
    public static ProfileResponse from(UserProfile p) {
        return new ProfileResponse(
                p.getAccountId(), p.getName(), p.getRole(), p.getLanguage(), p.getPhone(),
                p.isSeniorMode(), p.isLiteMode(), p.isVerified(), p.getRole().requiresVerification());
    }
}