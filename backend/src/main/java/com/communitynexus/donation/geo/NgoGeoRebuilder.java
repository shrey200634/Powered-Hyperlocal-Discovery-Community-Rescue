package com.communitynexus.donation.geo;

import com.communitynexus.donation.entity.Ngo;
import com.communitynexus.donation.repo.NgoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Same rule as stores: MySQL is the truth, Redis is rebuilt from it on every start. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NgoGeoRebuilder implements ApplicationRunner {

    private final NgoRepository ngoRepo;
    private final NgoGeoIndex geoIndex;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        try {
            Map<String, List<Ngo>> byCity = ngoRepo.findByActiveTrue().stream()
                    .collect(Collectors.groupingBy(Ngo::getCity));
            byCity.forEach((city, ngos) -> {
                geoIndex.clear(city);
                ngos.forEach(n -> geoIndex.add(city, n.getId(), n.getLat(), n.getLng()));
                log.info("NGO geo index rebuilt for {}: {} NGOs", city, ngos.size());
            });
        } catch (Exception e) {
            log.error("Could not rebuild NGO geo index - is Redis running?", e);
        }
    }
}