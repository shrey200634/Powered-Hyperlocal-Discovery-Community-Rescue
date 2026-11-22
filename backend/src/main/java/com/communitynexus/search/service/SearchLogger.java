package com.communitynexus.search.service;

import com.communitynexus.search.dto.ParsedQuery;
import com.communitynexus.search.entity.SearchChannel;
import com.communitynexus.search.entity.SearchQueryLog;
import com.communitynexus.search.event.SearchQueriedEvent;
import com.communitynexus.search.repo.SearchQueryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Component
@RequiredArgsConstructor
public class SearchLogger {

    private final SearchQueryLogRepository repo;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String city, ParsedQuery pq, boolean found, int resultCount,
                    double lat, double lng, SearchChannel channel) {

        SearchQueryLog row = repo.save(SearchQueryLog.create(
                city, pq.normalizedText(), String.join(",", pq.terms()),
                pq.category(), found, resultCount, channel, lat, lng));

        events.publishEvent(new SearchQueriedEvent(
                row.getId(), city, row.getText(), pq.terms(),
                pq.category() == null ? null : pq.category().name(),
                found, row.getLat(), row.getLng(), channel.name(), Instant.now()));
    }
}