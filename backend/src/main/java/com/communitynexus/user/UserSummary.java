package com.communitynexus.user;

import com.communitynexus.auth.Role;
import com.communitynexus.common.Language;

public record UserSummary(
        Long accountId,
        String name,
        Role role,
        Language language,
        boolean seniorMode,
        boolean verified
) {
}