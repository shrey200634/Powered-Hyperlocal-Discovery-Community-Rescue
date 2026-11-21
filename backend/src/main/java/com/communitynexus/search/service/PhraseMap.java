package com.communitynexus.search.service;

import com.communitynexus.inventory.ProductCategory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class PhraseMap {

    public record Entry(String phrase, List<String> terms, ProductCategory category) {
    }

    private List<Entry> entries = List.of();

    @PostConstruct
    void load() throws IOException {
        List<Entry> list = new ArrayList<>();
        var resource = new ClassPathResource("search/phrases.csv");

        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",", -1);          // -1 keeps empty columns
                if (parts.length < 3) continue;

                String phrase = parts[0].trim().toLowerCase();
                List<String> terms = parts[1].isBlank()
                        ? List.of()
                        : Arrays.stream(parts[1].split("\\|")).map(String::trim).map(String::toLowerCase).toList();
                ProductCategory category = parts[2].isBlank() ? null : ProductCategory.valueOf(parts[2].trim());

                list.add(new Entry(phrase, terms, category));
            }
        }
        // "sar dard" must be tried before "dard"
        list.sort(Comparator.comparingInt((Entry e) -> e.phrase().length()).reversed());
        this.entries = List.copyOf(list);
        log.info("Phrase map loaded: {} phrases", entries.size());
    }

    public List<Entry> entries() {
        return entries;
    }
}