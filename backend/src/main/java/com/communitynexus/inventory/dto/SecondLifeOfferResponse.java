package com.communitynexus.inventory.dto;

import com.communitynexus.inventory.entity.SecondLifeOffer;
import com.communitynexus.inventory.entity.SecondLifeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SecondLifeOfferResponse(
        Long offerId,
        Long productId,
        String productName,
        int quantity,
        BigDecimal originalPrice,
        BigDecimal savePrice,
        LocalDate expiryDate,
        SecondLifeStatus status,
        Instant donateAfter
) {
    public static SecondLifeOfferResponse from(SecondLifeOffer o) {
        var p = o.getProduct();
        return new SecondLifeOfferResponse(o.getId(), p.getId(), p.getName(), p.getQuantity(),
                o.getOriginalPrice(), o.getSavePrice(), p.getExpiryDate(), o.getStatus(), o.getDonateAfter());
    }
}