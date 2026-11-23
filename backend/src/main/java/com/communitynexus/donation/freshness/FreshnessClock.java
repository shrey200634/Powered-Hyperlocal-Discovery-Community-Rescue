package com.communitynexus.donation.freshness;

import com.communitynexus.donation.FoodType;
import com.communitynexus.donation.config.DonationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


@Component
@RequiredArgsConstructor
public class FreshnessClock {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DonationProperties props;

    public record ItemInput(FoodType type, LocalDate packagedExpiry) {
    }

    public Instant safeUntil(Instant cookedAt, List<ItemInput> items, double dayHighC) {
        Instant earliest = null;
        for (ItemInput item : items) {
            Instant t = safeUntilFor(cookedAt, item, dayHighC);
            if (earliest == null || t.isBefore(earliest)) {
                earliest = t;
            }
        }
        return earliest;
    }

    private Instant safeUntilFor(Instant cookedAt, ItemInput item, double dayHighC) {
        if (item.type() == FoodType.PACKAGED) {
            // Printed date: safe until the END of that day. Heat is not applied (doc C5).
            return item.packagedExpiry().plusDays(1).atStartOfDay(IST).toInstant();
        }
        double hours = props.freshness().baseHours().get(item.type()) * heatFactor(dayHighC);
        return cookedAt.plus(Duration.ofMinutes(Math.round(hours * 60)));
    }

    public double heatFactor(double dayHighC) {
        var f = props.freshness();
        if (dayHighC < f.coolBelowC()) return 1.0;
        if (dayHighC > f.hotAboveC()) return f.hotFactor();
        return f.warmFactor();
    }
}