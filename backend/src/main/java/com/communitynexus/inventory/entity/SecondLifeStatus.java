package com.communitynexus.inventory.entity;

public enum SecondLifeStatus {
    SAVE_IT_PRICE,   // shown to citizens at the marked-down price
    SOLD,            // all sold — nothing wasted
    DONATED,         // handed to Food Rescue (Step 7)
    CLOSED           // ended any other way
}