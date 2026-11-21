package com.communitynexus.inventory.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.config.InventoryProperties;
import com.communitynexus.inventory.dto.FlagRequest;
import com.communitynexus.inventory.dto.NearExpiryItem;
import com.communitynexus.inventory.dto.SecondLifeOfferResponse;
import com.communitynexus.inventory.entity.Product;
import com.communitynexus.inventory.entity.SecondLifeOffer;
import com.communitynexus.inventory.entity.SecondLifeStatus;
import com.communitynexus.inventory.entity.Store;
import com.communitynexus.inventory.event.ShelfFlaggedEvent;
import com.communitynexus.inventory.repo.ProductRepository;
import com.communitynexus.inventory.repo.SecondLifeOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecondLifeService {

    private final StoreService storeService;
    private final ProductRepository productRepo;
    private final SecondLifeOfferRepository offerRepo;
    private final InventoryProperties props;
    private final ApplicationEventPublisher events;

    /** Shopkeeper's morning list: what is about to expire in my shop? */
    @Transactional(readOnly = true)
    public List<NearExpiryItem> nearExpiry(Long ownerId, Long storeId) {
        storeService.loadOwned(ownerId, storeId);
        LocalDate today = today();
        LocalDate until = today.plusDays(props.secondLife().flagWithinDays());

        return productRepo.findNearExpiry(storeId, today, until).stream()
                .map(p -> new NearExpiryItem(
                        p.getId(), p.getName(), p.getQuantity(), p.getPrice(), p.getExpiryDate(),
                        ChronoUnit.DAYS.between(today, p.getExpiryDate()),
                        suggestedPrice(p.getPrice()),
                        offerRepo.existsByProduct_IdAndStatus(p.getId(), SecondLifeStatus.SAVE_IT_PRICE)))
                .toList();
    }

    /** The one tap: move this product to the Second-Life Shelf. Doc C1: POST /products/{id}/second-life */
    @Transactional
    public SecondLifeOfferResponse flag(Long ownerId, Long productId, FlagRequest req) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> NexusException.notFound("Product"));
        Store store = product.getStore();
        if (!store.isOwnedBy(ownerId)) {
            throw new NexusException(HttpStatus.FORBIDDEN, "NOT_YOUR_STORE", "This product belongs to another shop");
        }

        LocalDate today = today();
        if (product.getExpiryDate() == null
                || product.getExpiryDate().isAfter(today.plusDays(props.secondLife().flagWithinDays()))) {
            throw NexusException.badRequest("NOT_NEAR_EXPIRY", "Only items close to their expiry date can go on the shelf");
        }
        if (product.getQuantity() <= 0) {
            throw NexusException.badRequest("OUT_OF_STOCK", "Nothing left to put on the shelf");
        }
        if (offerRepo.existsByProduct_IdAndStatus(productId, SecondLifeStatus.SAVE_IT_PRICE)) {
            throw NexusException.conflict("ALREADY_ON_SHELF", "This item is already at save-it price");
        }

        BigDecimal savePrice = (req != null && req.savePrice() != null) ? req.savePrice() : suggestedPrice(product.getPrice());
        SecondLifeOffer offer = offerRepo.save(SecondLifeOffer.create(product, savePrice, donateAfter(product.getExpiryDate())));

        store.confirmStock();
        events.publishEvent(new ShelfFlaggedEvent(
                offer.getId(), store.getId(), product.getId(), product.getName(), product.getCategory().name(),
                product.getQuantity(), product.getExpiryDate(), savePrice, offer.getDonateAfter(), Instant.now()));

        log.info("Product {} moved to Second-Life Shelf at {} (donate after {})", productId, savePrice, offer.getDonateAfter());
        return SecondLifeOfferResponse.from(offer);
    }

    // ---------- helpers ----------

    /** 30% off, rounded to whole rupees — shopkeepers don't sell bread for ₹27.65. */
    private BigDecimal suggestedPrice(BigDecimal price) {
        BigDecimal keep = BigDecimal.valueOf(100 - props.secondLife().defaultDiscountPct());
        BigDecimal suggested = price.multiply(keep).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return suggested.signum() > 0 ? suggested : BigDecimal.ONE;
    }

    /** 5 pm on the day BEFORE expiry. If that moment has already passed, donate from now. */
    private Instant donateAfter(LocalDate expiryDate) {
        ZonedDateTime planned = expiryDate.minusDays(1)
                .atTime(props.secondLife().donateAt())
                .atZone(props.timeZone());
        Instant now = Instant.now();
        return planned.toInstant().isBefore(now) ? now : planned.toInstant();
    }

    private LocalDate today() {
        return LocalDate.now(props.timeZone());
    }
}