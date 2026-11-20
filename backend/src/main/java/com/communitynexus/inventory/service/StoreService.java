package com.communitynexus.inventory.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.config.InventoryProperties;
import com.communitynexus.inventory.dto.CreateStoreRequest;
import com.communitynexus.inventory.dto.StoreResponse;
import com.communitynexus.inventory.entity.Store;
import com.communitynexus.inventory.geo.StoreGeoIndex;
import com.communitynexus.inventory.repo.StoreRepository;
import com.communitynexus.user.UserDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepo;
    private final StoreGeoIndex geoIndex;
    private final UserDirectory userDirectory;      // User module's PUBLIC API — not its repository
    private final InventoryProperties props;

    @Transactional
    public StoreResponse create(Long ownerAccountId, CreateStoreRequest req) {
        // Doc C1: "An NGO, shop or volunteer is invisible to others until verified = true."
        if (!userDirectory.isVerified(ownerAccountId)) {
            throw new NexusException(HttpStatus.FORBIDDEN, "SHOPKEEPER_NOT_VERIFIED",
                    "Your shop can go live once the Nexus team verifies you");
        }

        Store store = storeRepo.save(Store.create(
                ownerAccountId, req.name(), req.category(), req.city(), req.lat(), req.lng(),
                req.addressLine(), req.opensAt(), req.closesAt()));

        geoIndex.addAfterCommit(store);
        log.info("Store {} '{}' created by account {}", store.getId(), store.getName(), ownerAccountId);
        return toResponse(store);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> myStores(Long ownerAccountId) {
        return storeRepo.findByOwnerIdOrderByIdAsc(ownerAccountId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse get(Long storeId) {
        return toResponse(load(storeId));
    }

    /** Doc C1: POST /stores/{id}/confirm-stock — the daily one-tap "my stock is correct". */
    @Transactional
    public StoreResponse confirmStock(Long ownerAccountId, Long storeId) {
        Store store = loadOwned(ownerAccountId, storeId);
        store.confirmStock();
        return toResponse(store);
    }

    // ---------- helpers ----------

    Store load(Long storeId) {
        return storeRepo.findById(storeId).orElseThrow(() -> NexusException.notFound("Store"));
    }

    /** Doc B5 ownership check: "a shop edits only its own stock". */
    Store loadOwned(Long accountId, Long storeId) {
        Store store = load(storeId);
        if (!store.isOwnedBy(accountId)) {
            throw new NexusException(HttpStatus.FORBIDDEN, "NOT_YOUR_STORE", "This store belongs to someone else");
        }
        return store;
    }

    private StoreResponse toResponse(Store s) {
        return StoreResponse.from(s, props.staleAfter(), props.timeZone());
    }
}