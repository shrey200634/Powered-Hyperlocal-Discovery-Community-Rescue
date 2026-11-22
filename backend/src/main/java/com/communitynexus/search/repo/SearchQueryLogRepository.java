package com.communitynexus.search.repo;

import com.communitynexus.search.dto.UnmetNeedRow;
import com.communitynexus.search.entity.SearchQueryLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SearchQueryLogRepository extends JpaRepository<SearchQueryLog, Long> {

    @Query("""
            select new com.communitynexus.search.dto.UnmetNeedRow(
                       l.text, count(l), max(l.createdAt), avg(l.lat), avg(l.lng))
            from SearchQueryLog l
            where l.found = false
              and l.city = :city
              and l.createdAt >= :since
            group by l.text
            order by count(l) desc
            """)
    List<UnmetNeedRow> topUnmet(@Param("city") String city,
                                @Param("since") Instant since,
                                Pageable page);
}