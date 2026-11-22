package com.communitynexus.reservation.redis;

import com.communitynexus.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryListener implements MessageListener {

    /** Redis and the app clocks can differ by a moment; accept expiries this close to the deadline. */
    private static final Duration CLOCK_TOLERANCE = Duration.ofSeconds(5);

    private final ReservationService reservationService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        HoldTimer.parseId(expiredKey).ifPresent(id -> {
            try {
                if (reservationService.expireIfDue(id, CLOCK_TOLERANCE)) {
                    log.info("Redis expiry: hold {} expired on time", id);
                }
            } catch (Exception e) {
                // sweeper will retry within a minute
                log.warn("Could not expire hold {} from Redis event: {}", id, e.getMessage());
            }
        });
    }
}