package com.communitynexus.search.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.inventory.ProductCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search_query_log", indexes = {
        @Index(name = "idx_search_log_found_time", columnList = "found, created_at"),
        @Index(name = "idx_search_log_city", columnList = "city")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchQueryLog extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String city;

    /** Normalised text, e.g. "mujhe bukhar ki dawa chahiye" */
    @Column(nullable = false, length = 200)
    private String text;

    /** What we understood, e.g. "paracetamol" */
    @Column(length = 200)
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductCategory category;

    @Column(nullable = false)
    private boolean found;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SearchChannel channel;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    public static SearchQueryLog create(String city, String text, String terms, ProductCategory category,
                                        boolean found, int resultCount, SearchChannel channel,
                                        double lat, double lng) {
        SearchQueryLog l = new SearchQueryLog();
        l.city = city;
        l.text = cut(text, 200);
        l.terms = cut(terms, 200);
        l.category = category;
        l.found = found;
        l.resultCount = resultCount;
        l.channel = channel;
        l.lat = round3(lat);
        l.lng = round3(lng);
        return l;
    }

    /** 3 decimal places ≈ 110 m. */
    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static String cut(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }
}