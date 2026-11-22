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
import com.communitynexus.reservation.event.ReservationCancelledEvent;
import com.communitynexus.reservation.event.ReservationCreatedEvent;
import com.communitynexus.reservation.event.ReservationExpiredEvent;
import com.communitynexus.reservation.event.ReservationPickedUpEvent;
import com.communitynexus.reservation.redis.HoldTimer;
import com.communitynexus.reservation.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<ReservationStatus> ACTIVE =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservationRepository repo;
    private final InventoryCatalog catalog;
    private final ReservationProperties props;
    private final HoldTimer holdTimer;
    private final ApplicationEventPublisher events;

    // =====================================================================
    // Customer (citizen / senior)
    // =====================================================================

    @Transactional
    public ReservationResponse create(Long userId, CreateReservationRequest req) {
        int qty = req.quantity() == null ? 1 : req.quantity();

        if (repo.countByUserIdAndStatusIn(userId, ACTIVE) >= props.maxActivePerUser()) {
            throw new NexusException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_HOLDS",
                    "You already have " + props.maxActivePerUser() + " active holds. Collect or cancel one first");
        }

        // Atomic stock decrement inside THIS transaction (throws OUT_OF_STOCK if gone)
        ReservedItem item = catalog.reserve(req.productId(), qty);

        Reservation r = repo.save(Reservation.hold(
                userId, item, qty, ReservationSource.SEARCH,
                Instant.now().plus(props.holdDuration()), newPickupCode()));

        // Redis timer starts only after the DB commit succeeds
        holdTimer.startAfterCommit(r.getId(), props.holdDuration());

        events.publishEvent(new ReservationCreatedEvent(
                r.getId(), userId, r.getStoreId(), r.getStoreOwnerId(), r.getProductId(), r.getProductName(),
                r.getQuantity(), r.getHoldExpiresAt(), false, Instant.now()));

        log.info("Reservation {} created: user {} holds {} x product {} at store {} until {}",
                r.getId(), userId, qty, item.productId(), item.storeId(), r.getHoldExpiresAt());
        return ReservationResponse.forCustomer(r);
    }

    /** "My Orders" */
    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(Long userId) {
        return repo.findByUserIdOrderByIdDesc(userId).stream()
                .map(ReservationResponse::forCustomer)
                .toList();
    }

    /** Customer sees the pickup code; the shop and admin do not. */
    @Transactional(readOnly = true)
    public ReservationResponse get(AuthUser me, Long id) {
        Reservation r = load(id);
        if (r.isOwnedBy(me.accountId())) {
            return ReservationResponse.forCustomer(r);
        }
        if (r.isForShopOwner(me.accountId()) || me.role() == Role.ADMIN) {
            return ReservationResponse.forShop(r);
        }
        throw forbidden();
    }

    @Transactional
    public ReservationResponse cancel(Long userId, Long id) {
        Reservation r = load(id);
        if (!r.isOwnedBy(userId)) {
            throw forbidden();
        }

        r.cancel();
        catalog.release(r.getProductId(), r.getQuantity());     // back on the shelf
        holdTimer.stopAfterCommit(r.getId());                    // no expiry message later

        events.publishEvent(new ReservationCancelledEvent(
                r.getId(), r.getUserId(), r.getStoreId(), r.getProductId(), r.getQuantity(), Instant.now()));

        log.info("Reservation {} cancelled by user {}", id, userId);
        return ReservationResponse.forCustomer(r);
    }

    // =====================================================================
    // Shopkeeper
    // =====================================================================

    @Transactional(readOnly = true)
    public List<ReservationResponse> activeForStore(Long ownerId, Long storeId) {
        return repo.findByStoreIdAndStatusInOrderByHoldExpiresAtAsc(storeId, ACTIVE).stream()
                .filter(r -> r.isForShopOwner(ownerId))
                .map(ReservationResponse::forShop)
                .toList();
    }

    /** Doc C4 step 6: "The shop gets an alert and confirms." */
    @Transactional
    public ReservationResponse confirmByShop(Long ownerId, Long id) {
        Reservation r = load(id);
        if (!r.isForShopOwner(ownerId)) {
            throw forbidden();
        }
        r.confirmByShop();
        return ReservationResponse.forShop(r);
    }

    /** Doc C4 step 7: "At the counter the shop calls confirm-pickup." */
    @Transactional(noRollbackFor = NexusException.class)
    public ReservationResponse confirmPickup(Long ownerId, Long id, String code) {
        Reservation r = load(id);
        if (!r.isForShopOwner(ownerId)) {
            throw forbidden();
        }

        r.pickUp(code);                                          // throws on wrong code / expired
        holdTimer.stopAfterCommit(r.getId());

        events.publishEvent(new ReservationPickedUpEvent(
                r.getId(), r.getUserId(), r.getStoreId(), r.getProductId(), r.getQuantity(), Instant.now()));

        log.info("Reservation {} picked up", id);
        return ReservationResponse.forShop(r);
    }

    // =====================================================================
    // Expiry — called by the Redis listener (on time) and the sweeper (safety net)
    // =====================================================================

    /**
     * Expires ONE reservation if it is still active and its time is up.
     *
     * @param tolerance how early we accept an expiry (Redis and app clocks can differ slightly).
     *                  Redis listener passes 5 seconds; the sweeper passes Duration.ZERO.
     * @return true if this call expired it; false if it was already closed or not due yet.
     *
     * Idempotent: Redis and the sweeper may both call this for the same hold — only the first acts.
     */
    @Transactional
    public boolean expireIfDue(Long id, Duration tolerance) {
        Reservation r = repo.findById(id).orElse(null);
        if (r == null || !r.getStatus().isActive()) {
            return false;
        }
        if (r.getHoldExpiresAt().isAfter(Instant.now().plus(tolerance))) {
            return false;      // not yet due (e.g. someone deleted the Redis key by hand)
        }

        r.expire();
        catalog.release(r.getProductId(), r.getQuantity());     // back on the shelf
        holdTimer.stopAfterCommit(r.getId());                    // usually already gone; harmless

        events.publishEvent(new ReservationExpiredEvent(
                r.getId(), r.getUserId(), r.getStoreId(), r.getProductId(), r.getQuantity(), Instant.now()));

        log.info("Reservation {} expired, {} unit(s) of product {} back on the shelf",
                id, r.getQuantity(), r.getProductId());
        return true;
    }

    /** For the sweeper: ids of active holds whose time is up. */
    @Transactional(readOnly = true)
    public List<Long> findDueIds() {
        return repo.findByStatusInAndHoldExpiresAtBefore(ACTIVE, Instant.now()).stream()
                .map(Reservation::getId)
                .toList();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

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