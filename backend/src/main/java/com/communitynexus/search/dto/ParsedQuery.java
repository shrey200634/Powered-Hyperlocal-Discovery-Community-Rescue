package com.communitynexus.search.dto;

import com.communitynexus.inventory.ProductCategory;

import java.util.List;

public record ParsedQuery(
        String normalizedText,
        List<String> terms,          // e.g. [paracetamol]
        ProductCategory category     // e.g. MEDICINE, or null
) {
    public boolean isEmpty() {
        return terms.isEmpty() && category == null;
    }
}