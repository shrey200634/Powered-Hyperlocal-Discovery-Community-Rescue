package com.communitynexus.auth.event;

import com.communitynexus.auth.Role;
import com.communitynexus.common.Language;

import java.time.Instant;

public record UserRegisteredEvent(
        Long accountId,
        Role role,
        String phone,
        Language language,
        String name,
        Instant occurredAt
) {
}