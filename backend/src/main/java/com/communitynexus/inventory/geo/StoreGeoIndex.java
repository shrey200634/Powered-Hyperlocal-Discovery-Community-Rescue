package com.communitynexus.inventory.geo;

import com.communitynexus.inventory.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.domain.geo.GeoReference;

import java.util.List;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class StoreGeoIndex {

    private static final String KEY_PREFIX = "geo:stores:";

    private final StringRedisTemplate redis;

    public static String key(String city) {
        return KEY_PREFIX + city;
    }


    public void addAfterCommit(Store store) {
        String city = store.getCity();
        Long id = store.getId();
        double lat = store.getLat();
        double lng = store.getLng();

        Runnable write = () -> add(city, id, lat, lng);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write.run();
                }
            });
        } else {
            write.run();
        }
    }

    public void add(String city, Long storeId, double lat, double lng) {
        // ⚠ Redis wants (longitude, latitude) — x first, then y. Mixing them up is the #1 geo bug.
        redis.opsForGeo().add(key(city), new Point(lng, lat), String.valueOf(storeId));
        log.debug("Indexed store {} in {}", storeId, key(city));
    }

    public void remove(String city, Long storeId) {
        redis.opsForGeo().remove(key(city), String.valueOf(storeId));
    }

    public void clear(String city) {
        redis.delete(key(city));
    }


    public List<StoreDistance> nearby(String city, double lat, double lng, int radiusMeters, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                key(city),
                GeoReference.fromCoordinate(lng, lat),                       // again: lng first
                new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit));

        if (results == null) {
            return List.of();
        }
        return results.getContent().stream()
                .map(StoreGeoIndex::toStoreDistance)
                .toList();
    }

    private static StoreDistance toStoreDistance(GeoResult<RedisGeoCommands.GeoLocation<String>> r) {
        Long storeId = Long.valueOf(r.getContent().getName());
        double meters = r.getDistance().getValue() * 1000.0;               // we asked in km
        return new StoreDistance(storeId, meters);
    }
}