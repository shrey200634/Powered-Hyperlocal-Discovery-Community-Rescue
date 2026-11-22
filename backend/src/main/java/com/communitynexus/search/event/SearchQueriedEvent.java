package com.communitynexus.search.event;

import java.time.Instant;
import java.util.List;


public record SearchQueriedEvent(
        Long logId,
        String city,
        String text,
        List<String> terms,
        String category,
        boolean found,
        double lat,
        double lng,
        String channel,
        Instant occurredAt
) {
}