package com.communitynexus.search.controller;

import com.communitynexus.search.dto.NlSearchRequest;
import com.communitynexus.search.dto.SearchResponse;
import com.communitynexus.search.dto.UnmetNeedRow;
import com.communitynexus.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public SearchResponse search(@RequestParam String q,
                                 @RequestParam double lat,
                                 @RequestParam double lng,
                                 @RequestParam(required = false) Integer radius,
                                 @RequestParam(required = false) String city) {
        return searchService.search(q, lat, lng, radius, city);
    }

    @PostMapping("/nl")
    public SearchResponse voice(@Valid @RequestBody NlSearchRequest req) {
        return searchService.searchVoice(req);
    }

    @GetMapping("/unmet")
    @PreAuthorize("hasAnyRole('ADMIN', 'MUNICIPAL')")
    public List<UnmetNeedRow> unmet(@RequestParam(required = false) String city,
                                    @RequestParam(defaultValue = "7") int days) {
        return searchService.unmet(city, days);
    }
}