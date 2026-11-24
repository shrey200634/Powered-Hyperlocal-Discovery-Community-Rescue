package com.communitynexus.inventory.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.ProductCategory;
import com.communitynexus.inventory.config.InventoryProperties;
import com.communitynexus.inventory.dto.FlagRequest;
import com.communitynexus.inventory.dto.NearExpiryItem;
import com.communitynexus.inventory.dto.SecondLifeOfferResponse;
import com.communitynexus.inventory.entity.Product;
import com.communitynexus.inventory.entity.SecondLifeOffer;
import com.communitynexus.inventory.entity.SecondLifeStatus;
import com.communitynexus.inventory.entity.Store;
import com.communitynexus.inventory.event.ShelfFlaggedEvent;
import com.communitynexus.inventory.event.ShelfReadyToDonateEvent;
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

    // =====================================================================
    // Ladder steps 1–2: Sell → Save-it price (shopkeeper, one tap)
    // =====================================================================

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

    /** Doc C1: POST /products/{id}/second-life — the one tap. */
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

    // =====================================================================
    // Ladder steps 3–4: Donate → Return to soil (automatic, ShelfDonationScheduler)
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Long> findOfferIdsDueForDonation() {
        return offerRepo.findIdsDueForDonation(
                SecondLifeStatus.SAVE_IT_PRICE, ProductCategory.foodCategories(), Instant.now());
    }

    /**
     * Moves ONE offer down the ladder if its donate time has come. Idempotent.
     * @return true if units were handed to Food Rescue.
     */
    @Transactional
    public boolean moveDownLadderIfDue(Long offerId) {
        SecondLifeOffer offer = offerRepo.findById(offerId).orElse(null);
        if (offer == null
                || offer.getStatus() != SecondLifeStatus.SAVE_IT_PRICE
                || offer.getDonateAfter().isAfter(Instant.now())) {
            return false;
        }

        Product product = offer.getProduct();
        Store store = product.getStore();
        int unsold = product.getQuantity();

        // Everything sold at save-it price → nothing wasted, nothing to donate
        if (unsold <= 0) {
            offer.markSold();
            log.info("Second-Life offer {}: all sold, nothing to donate", offerId);
            return false;
        }

        // Step 4: already past expiry → not food rescue; off the shelf to animal shelter / compost
        if (product.isExpired(today())) {
            product.setQuantity(0);
            offer.closeAsWaste();
            log.info("Second-Life offer {}: {} x {} expired before donation -> return to soil (animal shelter / compost)",
                    offerId, unsold, product.getName());
            return false;
        }

        // Step 3: take the unsold units off the shelf and hand them to Food Rescue
        product.setQuantity(0);
        offer.markDonated();

        events.publishEvent(new ShelfReadyToDonateEvent(
                offer.getId(), store.getId(), store.getName(), store.getOwnerId(), store.getCity(),
                store.getLat(), store.getLng(), product.getId(), product.getName(), product.getCategory().name(),
                unsold, product.getExpiryDate(), Instant.now()));

        log.info("Second-Life offer {}: {} unsold x {} moved off the shelf -> Food Rescue",
                offerId, unsold, product.getName());
        return true;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** 30% off, rounded to whole rupees. */
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