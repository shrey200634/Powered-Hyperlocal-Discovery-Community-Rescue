package com.communitynexus.inventory.geo;

import com.communitynexus.inventory.entity.Store;
import com.communitynexus.inventory.repo.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoIndexRebuilder implements ApplicationRunner {

    private final StoreRepository storeRepo;
    private final StoreGeoIndex geoIndex;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        Map<String, List<Store>> byCity = storeRepo.findByActiveTrue()
                .stream()
                .collect(Collectors.groupingBy(Store::getCity));

        byCity.forEach((city, stores) -> {
            geoIndex.clear(city);   // also drops shops that were deactivated
            stores.forEach(s -> geoIndex.add(city, s.getId(), s.getLat(), s.getLng()));
            log.info("Geo index rebuilt for {}: {} stores", city, stores.size());
        });
    }
}