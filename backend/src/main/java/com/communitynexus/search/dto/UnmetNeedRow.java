package com.communitynexus.search.dto;

import java.time.Instant;

/** One line of "what people keep asking for and not finding". */
public record UnmetNeedRow(
        String text,
        Long timesAsked,
        Instant lastAsked,
        Double avgLat,
        Double avgLng
) {
}