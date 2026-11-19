package com.communitynexus.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus.otp")
public record OtpProperties(
        Duration ttl,
        Duration resendCooldown,
        boolean logToConsole
) {
}