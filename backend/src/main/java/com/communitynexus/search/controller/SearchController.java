package com.communitynexus.search.controller;

import com.communitynexus.search.dto.SearchResponse;
import com.communitynexus.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q,
                                 @RequestParam double lat,
                                 @RequestParam double lng,
                                 @RequestParam(required = false) Integer radius,
                                 @RequestParam(required = false) String city) {
        return searchService.search(q, lat, lng, radius, city);
    }
}