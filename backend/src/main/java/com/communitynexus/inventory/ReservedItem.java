package com.communitynexus.inventory;

import java.math.BigDecimal;

public record ReservedItem(
        Long productId,
        String productName,
        Long storeId,
        String storeName,
        Long storeOwnerId,
        BigDecimal unitPrice      // save-it price if the item is on the Second-Life Shelf
) {
}