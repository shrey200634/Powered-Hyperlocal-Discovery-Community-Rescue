package com.communitynexus.donation.dto;

import com.communitynexus.donation.FoodGroup;
import com.communitynexus.donation.FoodType;
import com.communitynexus.donation.entity.DonationItem;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DonationItemResponse(
        String name,
        FoodGroup foodGroup,
        FoodType foodType,
        int servings,
        BigDecimal weightKg,
        LocalDate packagedExpiry
) {
    public static DonationItemResponse from(DonationItem i) {
        return new DonationItemResponse(i.getName(), i.getFoodGroup(), i.getFoodType(),
                i.getServings(), i.getWeightKg(), i.getPackagedExpiry());
    }
}