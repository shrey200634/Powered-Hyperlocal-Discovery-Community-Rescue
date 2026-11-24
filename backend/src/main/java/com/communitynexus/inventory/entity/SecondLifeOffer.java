package com.communitynexus.inventory.entity;

import com.communitynexus.common.entity.BaseEntity;
import com.communitynexus.common.exception.NexusException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inv_second_life_offers", indexes = {
        @Index(name = "idx_inv_slo_product_status", columnList = "product_id, status"),
        @Index(name = "idx_inv_slo_status_donate", columnList = "status, donate_after")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecondLifeOffer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "save_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal savePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SecondLifeStatus status;

    @Column(name = "flagged_at", nullable = false)
    private Instant flaggedAt;

    /** After this moment, whatever is left goes down the ladder to donation (Step 7). */
    @Column(name = "donate_after", nullable = false)
    private Instant donateAfter;

    public static SecondLifeOffer create(Product product, BigDecimal savePrice, Instant donateAfter) {
        if (savePrice.signum() <= 0 || savePrice.compareTo(product.getPrice()) >= 0) {
            throw NexusException.badRequest("INVALID_SAVE_PRICE",
                    "Save-it price must be more than 0 and less than the normal price");
        }
        SecondLifeOffer o = new SecondLifeOffer();
        o.product = product;
        o.originalPrice = product.getPrice();
        o.savePrice = savePrice;
        o.status = SecondLifeStatus.SAVE_IT_PRICE;
        o.flaggedAt = Instant.now();
        o.donateAfter = donateAfter;
        return o;
    }

    public void markSold() {
        if (status == SecondLifeStatus.SAVE_IT_PRICE) {
            this.status = SecondLifeStatus.SOLD;
        }
    }

    public void markDonated() {
        if (status == SecondLifeStatus.SAVE_IT_PRICE) {
            this.status = SecondLifeStatus.DONATED;
        }
    }
    /** Ladder step 4: expired before anyone took it → animal shelter / compost, never landfill. */
    public void closeAsWaste() {
        if (status == SecondLifeStatus.SAVE_IT_PRICE) {
            this.status = SecondLifeStatus.CLOSED;
        }
    }
}