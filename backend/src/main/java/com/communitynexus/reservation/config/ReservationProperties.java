package com.communitynexus.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus.reservation")
public record ReservationProperties(
        Duration holdDuration,
        int maxActivePerUser,
        long sweepEveryMs
) {
}