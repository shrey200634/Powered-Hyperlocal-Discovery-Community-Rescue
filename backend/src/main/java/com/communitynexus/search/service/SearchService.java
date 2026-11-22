package com.communitynexus.search.service;

import com.communitynexus.common.Language;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.InventoryCatalog;
import com.communitynexus.inventory.dto.NearbyStoreResult;
import com.communitynexus.inventory.dto.ProductResponse;
import com.communitynexus.search.dto.*;
import com.communitynexus.search.entity.SearchChannel;
import com.communitynexus.search.repo.SearchQueryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_UNMET_ROWS = 20;

    private final QueryParser parser;
    private final InventoryCatalog catalog;
    private final SearchLogger searchLogger;
    private final SpokenReply spoken;
    private final SearchQueryLogRepository logRepo;

    /** Typed search: GET /search */
    public SearchResponse search(String text, double lat, double lng, Integer radiusM, String city) {
        ParsedQuery pq = parser.parse(text);
        if (pq.isEmpty()) {
            throw NexusException.badRequest("EMPTY_QUERY", "Please tell us what you are looking for");
        }
        return run(text, pq, lat, lng, radiusM, city, SearchChannel.TEXT, null);
    }

    /** Voice search: POST /search/nl. Never throws for "didn't understand": returns understood=false + a sentence. */
    public SearchResponse searchVoice(NlSearchRequest req) {
        Language lang = req.language() == null ? Language.HI : req.language();
        ParsedQuery pq = parser.parse(req.text());
        if (pq.isEmpty()) {
            return SearchResponse.notUnderstood(req.text(), spoken.didNotUnderstand(lang));
        }
        return run(req.text(), pq, req.lat(), req.lng(), req.radius(), req.city(), SearchChannel.VOICE, lang);
    }

    /** found = false "feeds the Unmet-Need Map". For ward office / admin. */
    @Transactional(readOnly = true)
    public List<UnmetNeedRow> unmet(String city, int days) {
        String cityKey = cityOrDefault(city);
        Instant since = Instant.now().minus(Duration.ofDays(Math.max(1, Math.min(days, 90))));
        return logRepo.topUnmet(cityKey, since, PageRequest.of(0, MAX_UNMET_ROWS));
    }

    // ---------------- internals ----------------

    private SearchResponse run(String text, ParsedQuery pq, double lat, double lng, Integer radiusM,
                               String city, SearchChannel channel, Language lang) {
        String cityKey = cityOrDefault(city);
        List<String> terms = pq.terms().isEmpty() ? List.of("") : pq.terms();

        Map<Long, NearbyStoreResult> byStore = new LinkedHashMap<>();
        for (String term : terms) {
            for (NearbyStoreResult r : catalog.findNearby(lat, lng, radiusM, term, pq.category(), cityKey)) {
                byStore.merge(r.storeId(), r, SearchService::mergeProducts);
            }
        }

        List<NearbyStoreResult> results = byStore.values().stream()
                .sorted(Comparator.comparing(NearbyStoreResult::openNow).reversed()
                        .thenComparingInt(NearbyStoreResult::distanceMeters))
                .toList();
        boolean found = !results.isEmpty();

        // Log + event. A logging failure must NEVER fail the user's search.
        try {
            searchLogger.log(cityKey, pq, found, results.size(), lat, lng, channel);
        } catch (Exception e) {
            log.warn("Could not log search '{}': {}", pq.normalizedText(), e.getMessage());
        }

        String say = (lang == null) ? null
                : (found ? spoken.found(results.get(0), lang) : spoken.notFound(lang));

        return new SearchResponse(text, true, pq.terms(), pq.category(), found, !found, say, results);
    }

    private String cityOrDefault(String city) {
        return (city == null || city.isBlank()) ? catalog.defaultCity() : city.trim().toLowerCase();
    }

    private static NearbyStoreResult mergeProducts(NearbyStoreResult a, NearbyStoreResult b) {
        Map<Long, ProductResponse> products = new LinkedHashMap<>();
        a.products().forEach(p -> products.put(p.id(), p));
        b.products().forEach(p -> products.putIfAbsent(p.id(), p));
        return new NearbyStoreResult(a.storeId(), a.storeName(), a.storeCategory(), a.lat(), a.lng(),
                a.distanceMeters(), a.openNow(), List.copyOf(products.values()));
    }
}