package com.communitynexus.reservation.redis;

import com.communitynexus.common.tx.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HoldTimer {

    public static final String PREFIX = "hold:reservation:";

    private final StringRedisTemplate redis;

    public void startAfterCommit(Long reservationId, Duration ttl) {
        AfterCommit.run("start hold timer " + reservationId,
                () -> redis.opsForValue().set(PREFIX + reservationId, "1", ttl));
    }

    /** Hold ended early (picked up / cancelled) → remove the key so no expiry message fires later. */
    public void stopAfterCommit(Long reservationId) {
        AfterCommit.run("stop hold timer " + reservationId,
                () -> redis.delete(PREFIX + reservationId));
    }

    public static Optional<Long> parseId(String key) {
        if (key == null || !key.startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(key.substring(PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}