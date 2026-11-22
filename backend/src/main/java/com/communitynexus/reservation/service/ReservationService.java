package com.communitynexus.reservation.service;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.auth.Role;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.inventory.InventoryCatalog;
import com.communitynexus.inventory.ReservedItem;
import com.communitynexus.reservation.config.ReservationProperties;
import com.communitynexus.reservation.dto.CreateReservationRequest;
import com.communitynexus.reservation.dto.ReservationResponse;
import com.communitynexus.reservation.entity.Reservation;
import com.communitynexus.reservation.entity.ReservationSource;
import com.communitynexus.reservation.entity.ReservationStatus;
import com.communitynexus.reservation.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservationRepository repo;
    private final InventoryCatalog catalog;
    private final ReservationProperties props;

    // ---------------- customer ----------------

    @Transactional
    public ReservationResponse create(Long userId, CreateReservationRequest req) {
        int qty = req.quantity() == null ? 1 : req.quantity();

        if (repo.countByUserIdAndStatusIn(userId, ACTIVE) >= props.maxActivePerUser()) {
            throw new NexusException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_HOLDS",
                    "You already have " + props.maxActivePerUser() + " active holds. Collect or cancel one first");
        }

        // Atomic stock decrement inside THIS transaction (throws OUT_OF_STOCK if gone)
        ReservedItem item = catalog.reserve(req.productId(), qty);

        Reservation r = repo.save(Reservation.hold(userId, item, qty, ReservationSource.SEARCH,
                Instant.now().plus(props.holdDuration()), newPickupCode()));

        log.info("Reservation {} created: user {} holds {} x product {} at store {} until {}",
                r.getId(), userId, qty, item.productId(), item.storeId(), r.getHoldExpiresAt());
        return ReservationResponse.forCustomer(r);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(Long userId) {
        return repo.findByUserIdOrderByIdDesc(userId).stream().map(ReservationResponse::forCustomer).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(AuthUser me, Long id) {
        Reservation r = load(id);
        if (r.isOwnedBy(me.accountId())) return ReservationResponse.forCustomer(r);
        if (r.isForShopOwner(me.accountId()) || me.role() == Role.ADMIN) return ReservationResponse.forShop(r);
        throw forbidden();
    }

    @Transactional
    public ReservationResponse cancel(Long userId, Long id) {
        Reservation r = load(id);
        if (!r.isOwnedBy(userId)) throw forbidden();

        r.cancel();
        catalog.release(r.getProductId(), r.getQuantity());     // back on the shelf
        return ReservationResponse.forCustomer(r);
    }

    // ---------------- shopkeeper ----------------

    @Transactional(readOnly = true)
    public List<ReservationResponse> activeForStore(Long ownerId, Long storeId) {
        return repo.findByStoreIdAndStatusInOrderByHoldExpiresAtAsc(storeId, ACTIVE).stream()
                .filter(r -> r.isForShopOwner(ownerId))          // only YOUR store's holds
                .map(ReservationResponse::forShop)
                .toList();
    }

    @Transactional
    public ReservationResponse confirmByShop(Long ownerId, Long id) {
        Reservation r = load(id);
        if (!r.isForShopOwner(ownerId)) throw forbidden();
        r.confirmByShop();
        return ReservationResponse.forShop(r);
    }

    /** Doc C4: "At the counter the shop calls confirm-pickup." */
    @Transactional(noRollbackFor = NexusException.class)
    public ReservationResponse confirmPickup(Long ownerId, Long id, String code) {
        Reservation r = load(id);
        if (!r.isForShopOwner(ownerId)) throw forbidden();
        r.pickUp(code);
        log.info("Reservation {} picked up", id);
        return ReservationResponse.forShop(r);
    }

    // ---------------- expiry (used by the sweeper now, by the Redis listener in 6B) ----------------

    /** Expires ONE reservation if it is still active. Safe to call twice (idempotent). */
    @Transactional
    public boolean expireIfDue(Long id) {
        Reservation r = repo.findById(id).orElse(null);
        if (r == null || !r.getStatus().isActive() || !r.isPastHold()) {
            return false;
        }
        r.expire();
        catalog.release(r.getProductId(), r.getQuantity());
        log.info("Reservation {} expired, {} unit(s) of product {} back on the shelf", id, r.getQuantity(), r.getProductId());
        return true;
    }

    @Transactional(readOnly = true)
    public List<Long> findDueIds() {
        return repo.findByStatusInAndHoldExpiresAtBefore(ACTIVE, Instant.now()).stream().map(Reservation::getId).toList();
    }

    // ---------------- helpers ----------------

    private Reservation load(Long id) {
        return repo.findById(id).orElseThrow(() -> NexusException.notFound("Reservation"));
    }

    private static NexusException forbidden() {
        return new NexusException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This reservation is not yours");
    }

    private static String newPickupCode() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }
}