package com.communitynexus.search.service;

import com.communitynexus.inventory.ProductCategory;
import com.communitynexus.search.dto.ParsedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
@RequiredArgsConstructor
public class QueryParser {

    private static final int MAX_TERMS = 3;

    private static final Set<String> STOP_WORDS = Set.of(
            // Hindi / Hinglish filler
            "mujhe", "muje", "chahiye", "chaiye", "ki", "ka", "ke", "ko", "hai", "hain", "kahan", "kaha",
            "milegi", "milega", "mil", "paas", "pass", "mein", "me", "koi", "kuch", "de", "do", "dikhao",
            "wala", "wali", "wale", "aur", "ya", "bhi", "please", "plz",
            // English filler
            "i", "need", "want", "a", "an", "the", "some", "for", "of", "near", "nearby", "any", "get", "buy", "find"
    );

    private final PhraseMap phraseMap;

    public ParsedQuery parse(String rawText) {
        String text = normalize(rawText);
        String padded = " " + text + " ";

        List<String> terms = new ArrayList<>();
        ProductCategory category = null;

        // 2. known phrases, longest first; remove each match so "sar dard" isn't matched again as "dard"
        for (PhraseMap.Entry e : phraseMap.entries()) {
            String needle = " " + e.phrase() + " ";
            if (padded.contains(needle)) {
                terms.addAll(e.terms());
                if (category == null) category = e.category();
                padded = padded.replace(needle, " ");
            }
        }

        // 3 + 4. leftover words that aren't filler
        for (String word : padded.trim().split("\\s+")) {
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                terms.add(word);
            }
        }

        List<String> unique = terms.stream().filter(t -> !t.isBlank()).distinct().limit(MAX_TERMS).toList();
        return new ParsedQuery(text, unique, category);
    }

    static String normalize(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")   // keep letters (any language) and digits
                .replaceAll("\\s+", " ")
                .trim();
    }
}