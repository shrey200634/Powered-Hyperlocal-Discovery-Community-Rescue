package com.communitynexus.donation.config;

import com.communitynexus.donation.FoodType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "nexus.donation")
public record DonationProperties(
        String defaultCity,
        double defaultDayHighC,
        Duration minSafeTimeToPost,
        Freshness freshness
) {
    public record Freshness(
            Map<FoodType, Double> baseHours,
            double coolBelowC,
            double hotAboveC,
            double warmFactor,
            double hotFactor
    ) {
    }
}