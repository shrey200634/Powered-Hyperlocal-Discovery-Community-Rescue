package com.communitynexus.inventory;

import java.util.EnumSet;
import java.util.Set;

public enum ProductCategory {
    MEDICINE,
    GROCERY,
    DAIRY,
    BAKERY,
    PERSONAL_CARE,
    HOUSEHOLD,
    OTHER;

    private static final Set<ProductCategory> FOOD = EnumSet.of(GROCERY, DAIRY, BAKERY);

    /** Only food can go down the ladder into Food Rescue. */
    public boolean isFood() {
        return FOOD.contains(this);
    }

    public static Set<ProductCategory> foodCategories() {
        return FOOD;
    }
}