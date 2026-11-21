package com.communitynexus.inventory.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.ProductCategory;
import com.communitynexus.inventory.config.InventoryProperties;
import com.communitynexus.inventory.dto.*;
import com.communitynexus.inventory.entity.*;
import com.communitynexus.inventory.event.StockUpdatedEvent;
import com.communitynexus.inventory.geo.StoreDistance;
import com.communitynexus.inventory.geo.StoreGeoIndex;
import com.communitynexus.inventory.repo.ProductRepository;
import com.communitynexus.inventory.repo.SecondLifeOfferRepository;
import com.communitynexus.inventory.repo.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_STORES_FROM_REDIS = 50;

    private final StoreService storeService;          // same package → can use loadOwned()
    private final StoreRepository storeRepo;
    private final ProductRepository productRepo;
    private final StoreGeoIndex geoIndex;
    private final InventoryProperties props;
    private final ApplicationEventPublisher events;
    private final SecondLifeOfferRepository offerRepo;

    // ---------------- shopkeeper side ----------------

    @Transactional
    public ProductResponse addProduct(Long ownerId, Long storeId, CreateProductRequest req) {
        Store store = storeService.loadOwned(ownerId, storeId);

        Product product = productRepo.save(Product.create(
                store, req.name(), req.category(), req.price(), req.quantity(), req.expiryDate()));

        store.confirmStock();   // touching stock = stock is fresh
        publishStockUpdated(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStock(Long ownerId, Long productId, StockUpdateRequest req) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> NexusException.notFound("Product"));

        Store store = product.getStore();
        if (!store.isOwnedBy(ownerId)) {
            throw new NexusException(HttpStatus.FORBIDDEN, "NOT_YOUR_STORE", "This product belongs to another shop");
        }

        if (req.quantity() != null) product.setQuantity(req.quantity());
        if (product.getQuantity() == 0) {
            offerRepo.findByProduct_IdAndStatus(product.getId(), SecondLifeStatus.SAVE_IT_PRICE)
                    .ifPresent(SecondLifeOffer::markSold);
        }
        if (req.price() != null) product.changePrice(req.price());

        store.confirmStock();
        publishStockUpdated(product);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listForStore(Long storeId) {
        storeService.load(storeId);   // 404 if the store doesn't exist
        return productRepo.findByStore_IdOrderByNameAsc(storeId).stream().map(ProductResponse::from).toList();
    }

    // ---------------- citizen side: near me ----------------

    @Transactional(readOnly = true)
    public List<NearbyStoreResult> nearby(double lat, double lng, Integer radiusM, String q,
                                          ProductCategory category, String city, boolean dealsOnly) {
        int radius = clampRadius(radiusM);
        String cityKey = Store.normalizeCity(city == null || city.isBlank() ? props.defaultCity() : city);
        String query = (q == null) ? "" : Product.keyOf(q);

        // 1. Redis: nearby stores
        List<StoreDistance> close = geoIndex.nearby(cityKey, lat, lng, radius, MAX_STORES_FROM_REDIS);
        if (close.isEmpty()) {
            return List.of();
        }
        Map<Long, Double> distanceById = close.stream()
                .collect(Collectors.toMap(StoreDistance::storeId, StoreDistance::meters));

        // 2. MySQL: active, non-stale stores
        Map<Long, Store> stores = storeRepo.findAllById(distanceById.keySet()).stream()
                .filter(Store::isActive)
                .filter(s -> !s.isStale(props.staleAfter()))
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        if (stores.isEmpty()) {
            return List.of();
        }

        // 3. MySQL: matching products
        LocalDate today = LocalDate.now(props.timeZone());
        List<Product> products = productRepo.searchInStores(stores.keySet(), query, category, today);
        if (products.isEmpty()) {
            return List.of();
        }

        // 3b. Which of them are on the Second-Life Shelf? (one query for all)
        Map<Long, BigDecimal> savePrices = offerRepo
                .findByProduct_IdInAndStatus(products.stream().map(Product::getId).toList(), SecondLifeStatus.SAVE_IT_PRICE)
                .stream()
                .collect(Collectors.toMap(o -> o.getProduct().getId(), SecondLifeOffer::getSavePrice, (a, b) -> a));

        if (dealsOnly) {
            products = products.stream().filter(p -> savePrices.containsKey(p.getId())).toList();
        }

        // 4. Group by store, open first, then nearest
        LocalTime now = LocalTime.now(props.timeZone());
        Map<Long, List<ProductResponse>> byStore = products.stream()
                .collect(Collectors.groupingBy(p -> p.getStore().getId(),
                        Collectors.mapping(p -> ProductResponse.from(p, savePrices.get(p.getId())), Collectors.toList())));

        return byStore.entrySet().stream()
                .map(e -> {
                    Store s = stores.get(e.getKey());
                    return new NearbyStoreResult(
                            s.getId(), s.getName(), s.getCategory(), s.getLat(), s.getLng(),
                            (int) Math.round(distanceById.get(s.getId())),
                            s.isOpenAt(now),
                            e.getValue());
                })
                .sorted(Comparator.comparing(NearbyStoreResult::openNow).reversed()
                        .thenComparingInt(NearbyStoreResult::distanceMeters))
                .toList();
    }

    // ---------------- helpers ----------------

    private int clampRadius(Integer radiusM) {
        int r = (radiusM == null) ? props.defaultRadiusM() : radiusM;
        return Math.max(100, Math.min(r, props.maxRadiusM()));
    }

    private void publishStockUpdated(Product p) {
        events.publishEvent(new StockUpdatedEvent(
                p.getStore().getId(), p.getId(), p.getName(), p.getCategory().name(),
                p.getQuantity(), "LISTED", Instant.now()));
    }
}