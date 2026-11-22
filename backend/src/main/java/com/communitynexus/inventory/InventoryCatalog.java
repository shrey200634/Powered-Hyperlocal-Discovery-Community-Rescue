package com.communitynexus.inventory;

import com.communitynexus.inventory.config.InventoryProperties;
import com.communitynexus.inventory.dto.NearbyStoreResult;
import com.communitynexus.inventory.repo.ProductRepository;
import com.communitynexus.inventory.repo.SecondLifeOfferRepository;
import com.communitynexus.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.entity.Product;
import com.communitynexus.inventory.entity.SecondLifeOffer;
import com.communitynexus.inventory.entity.SecondLifeStatus;
import com.communitynexus.inventory.entity.Store;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;


@Service
@RequiredArgsConstructor
public class InventoryCatalog {

    private final ProductService productService;
    private final ProductRepository productRepo;
    private final SecondLifeOfferRepository offerRepo;
    private final InventoryProperties props;




    public String defaultCity() {
        return props.defaultCity();
    }



    public List<NearbyStoreResult> findNearby(double lat, double lng, Integer radiusM,
                                              String term, ProductCategory category, String city) {
        return productService.nearby(lat, lng, radiusM, term, category, city, false);
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public ReservedItem reserve(Long productId, int qty) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> NexusException.notFound("Product"));
        Store store = product.getStore();
        if (!store.isActive() || store.isStale(props.staleAfter())) {
            throw NexusException.badRequest("STORE_UNAVAILABLE", "This shop is not taking holds right now");
        }
        if (product.isExpired(LocalDate.now(props.timeZone()))) {
            throw NexusException.badRequest("PRODUCT_EXPIRED", "This item is past its expiry date");
        }
        if (productRepo.takeStock(productId, qty) == 0) {
            throw NexusException.conflict("OUT_OF_STOCK", "Sorry, this item just ran out");
        }
        BigDecimal price = offerRepo.findByProduct_IdAndStatus(productId, SecondLifeStatus.SAVE_IT_PRICE)
                .map(SecondLifeOffer::getSavePrice)
                .orElse(product.getPrice());
        return new ReservedItem(product.getId(), product.getName(), store.getId(), store.getName(),
                store.getOwnerId(), price);
    }



    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long productId, int qty) {
        productRepo.returnStock(productId, qty);
    }
}