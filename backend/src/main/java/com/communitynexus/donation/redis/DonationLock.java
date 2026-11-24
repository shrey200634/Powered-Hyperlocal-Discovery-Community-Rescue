package com.communitynexus.donation.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Slf4j
@Component
@RequiredArgsConstructor
public class DonationLock {

    private static final String PREFIX = "lock:donation:";

    private final StringRedisTemplate redis;

    /**
     * @return true  → you won, go ahead
     *         false → another NGO already holds the lock
     * If Redis is DOWN we return true and let MySQL's @Version protect us (doc B1: works on a bad day).
     */
    public boolean tryAcquire(Long donationId, Long ngoId, Duration ttl) {
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + donationId, String.valueOf(ngoId), ttl);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("Redis unavailable for donation lock {} - falling back to DB version check: {}", donationId, e.getMessage());
            return true;
        }
    }

    public void release(Long donationId) {
        try {
            redis.delete(PREFIX + donationId);
        } catch (Exception e) {
            log.warn("Could not release donation lock {}: {}", donationId, e.getMessage());
        }
    }
}