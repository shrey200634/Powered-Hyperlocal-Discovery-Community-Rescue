package com.communitynexus.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "nexus.inventory")
public record InventoryProperties(
        Duration staleAfter,
        ZoneId timeZone
) {
}