package com.communitynexus.donation.geo;

import com.communitynexus.common.tx.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NgoGeoIndex {

    private static final String PREFIX = "geo:ngos:";

    private final StringRedisTemplate redis;

    public static String key(String city) {
        return PREFIX + city;
    }

    public void addAfterCommit(String city, Long ngoId, double lat, double lng) {
        AfterCommit.run("geo add ngo " + ngoId, () -> add(city, ngoId, lat, lng));
    }

    public void add(String city, Long ngoId, double lat, double lng) {
        redis.opsForGeo().add(key(city), new Point(lng, lat), String.valueOf(ngoId));   // lng first!
    }

    public void clear(String city) {
        redis.delete(key(city));
    }

    public Map<Long, Double> nearby(String city, double lat, double lng, int radiusMeters, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                key(city),
                GeoReference.fromCoordinate(lng, lat),
                new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sortAscending().limit(limit));

        Map<Long, Double> out = new LinkedHashMap<>();
        if (results != null) {
            results.getContent().forEach(r ->
                    out.put(Long.valueOf(r.getContent().getName()), r.getDistance().getValue() * 1000.0));
        }
        return out;
    }
}