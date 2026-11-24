package com.communitynexus.donation.service;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.auth.Role;
import com.communitynexus.common.exception.NexusException;
import com.communitynexus.common.geo.GeoMath;
import com.communitynexus.common.tx.AfterCommit;
import com.communitynexus.donation.config.DonationProperties;
import com.communitynexus.donation.dto.DonationResponse;
import com.communitynexus.donation.dto.NearbyDonationResponse;
import com.communitynexus.donation.dto.PostDonationRequest;
import com.communitynexus.donation.entity.DonationItem;
import com.communitynexus.donation.entity.DonationStatus;
import com.communitynexus.donation.entity.FoodDonation;
import com.communitynexus.donation.entity.Ngo;
import com.communitynexus.donation.event.DonationAcceptedEvent;
import com.communitynexus.donation.event.DonationExpiredEvent;
import com.communitynexus.donation.event.FoodPostedEvent;
import com.communitynexus.donation.freshness.FreshnessClock;
import com.communitynexus.donation.redis.DonationLock;
import com.communitynexus.donation.repo.FoodDonationRepository;
import com.communitynexus.donation.repo.NgoRepository;
import com.communitynexus.user.UserDirectory;
import com.communitynexus.user.UserSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.communitynexus.donation.FoodGroup;
import com.communitynexus.donation.FoodType;
import com.communitynexus.inventory.event.ShelfReadyToDonateEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(10);
    private static final Duration MAX_AGE = Duration.ofHours(24);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final Set<DonationStatus> OPEN =
            EnumSet.of(DonationStatus.POSTED, DonationStatus.MATCHED);
    private static final Set<DonationStatus> BEFORE_PICKUP =
            EnumSet.of(DonationStatus.POSTED, DonationStatus.MATCHED, DonationStatus.ACCEPTED);
    private static final Set<DonationStatus> COUNTS_FOR_CAPACITY =
            EnumSet.of(DonationStatus.ACCEPTED, DonationStatus.PICKED_UP, DonationStatus.DELIVERED, DonationStatus.CLOSED);

    private final FoodDonationRepository donationRepo;
    private final NgoRepository ngoRepo;
    private final NgoService ngoService;
    private final FreshnessClock freshnessClock;
    private final DonationLock donationLock;
    private final DonationProperties props;
    private final UserDirectory userDirectory;
    private final ApplicationEventPublisher events;

    // =====================================================================
    // Donor
    // =====================================================================

    /** Doc 05: "A kitchen posts surplus in 30 seconds." */
    @Transactional
    public DonationResponse post(Long donorId, PostDonationRequest req) {
        Instant now = Instant.now();
        Instant cookedAt = req.cookedAt().toInstant();

        if (cookedAt.isAfter(now.plus(MAX_FUTURE_SKEW))) {
            throw NexusException.badRequest("COOKED_IN_FUTURE", "Cooked time cannot be in the future");
        }
        if (cookedAt.isBefore(now.minus(MAX_AGE))) {
            throw NexusException.badRequest("TOO_OLD", "Food cooked more than 24 hours ago cannot be donated");
        }

        double dayHigh = req.dayHighC() != null ? req.dayHighC() : props.defaultDayHighC();

        // ---- Freshness Clock ----
        List<FreshnessClock.ItemInput> inputs = req.items().stream()
                .map(i -> new FreshnessClock.ItemInput(i.foodType(), i.packagedExpiry()))
                .toList();
        Instant safeUntil = freshnessClock.safeUntil(cookedAt, inputs, dayHigh);

        // Doc 12: "Nobody is offered unsafe food." Too little time left → don't post at all.
        Duration left = Duration.between(now, safeUntil);
        if (left.compareTo(props.minSafeTimeToPost()) < 0) {
            String msg = left.isNegative()
                    ? "This food is already past its safe time. Please send it to compost or an animal shelter."
                    : "Only " + left.toMinutes() + " minutes of safe time left - too little to rescue safely. "
                    + "Please send it to an animal shelter or compost.";
            throw new NexusException(HttpStatus.UNPROCESSABLE_ENTITY, "TOO_LATE_TO_RESCUE", msg);
        }

        String donorName = userDirectory.find(donorId).map(UserSummary::name).orElse("Donor");
        String city = (req.city() == null || req.city().isBlank()) ? props.defaultCity() : req.city();

        FoodDonation d = FoodDonation.post(donorId, donorName, city, req.lat(), req.lng(),
                req.pickupNote(), cookedAt, safeUntil, dayHigh);
        req.items().forEach(i -> d.addItem(i.name(), i.foodGroup(), i.foodType(), i.servings(),
                i.weightKg(), i.packagedExpiry()));
        donationRepo.save(d);

        events.publishEvent(new FoodPostedEvent(d.getId(), donorId, d.getCity(), d.getLat(), d.getLng(),
                d.getTotalServings(), servingsByGroup(d), safeUntil, now));

        log.info("Donation {} posted by {}: {} servings, safe until {} ({} min left)",
                d.getId(), donorName, d.getTotalServings(), safeUntil, d.minutesLeft());
        return DonationResponse.from(d);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> mine(Long donorId) {
        return donationRepo.findByDonorIdOrderByIdDesc(donorId).stream()
                .map(DonationResponse::from)
                .toList();
    }

    @Transactional
    public DonationResponse cancel(Long donorId, Long donationId) {
        FoodDonation d = load(donationId);
        if (!d.isPostedBy(donorId)) {
            throw forbidden();
        }
        d.cancelByDonor();
        return DonationResponse.from(d);
    }

    // =====================================================================
    // NGO
    // =====================================================================

    /** Doc C1: GET /donations/nearby. Nearest first (AI ranking replaces this in Step 12). */
    @Transactional(readOnly = true)
    public List<NearbyDonationResponse> nearbyForNgo(Long ngoAccountId) {
        Ngo ngo = ngoService.loadMine(ngoAccountId);

        return donationRepo.findByCityAndStatusInAndSafeUntilAfter(ngo.getCity(), OPEN, Instant.now()).stream()
                .map(d -> Map.entry(d, GeoMath.distanceMeters(ngo.getLat(), ngo.getLng(), d.getLat(), d.getLng())))
                .filter(e -> e.getValue() <= props.nearbyRadiusM())
                .sorted(Map.Entry.comparingByValue())
                .map(e -> NearbyDonationResponse.from(e.getKey(), e.getValue(), servingsByGroup(e.getKey())))
                .toList();
    }
    /**
     * The BRIDGE (doc 04 + C1): unsold Second-Life items become a food donation.
     * Idempotent: the outbox may deliver the event twice — source_offer_id is checked (and UNIQUE in the DB).
     */
    @Transactional
    public void createFromShelf(ShelfReadyToDonateEvent e) {
        if (donationRepo.existsBySourceOfferId(e.offerId())) {
            log.info("Shelf offer {} already became a donation - skipping", e.offerId());
            return;
        }

        Instant now = Instant.now();
        // Shop items are packaged: safe until the end of the printed date (doc C5, no heat factor)
        Instant safeUntil = freshnessClock.safeUntil(now,
                List.of(new FreshnessClock.ItemInput(FoodType.PACKAGED, e.expiryDate())),
                props.defaultDayHighC());

        if (Duration.between(now, safeUntil).compareTo(props.minSafeTimeToPost()) < 0) {
            log.info("Shelf offer {}: {} x {} too close to expiry for rescue -> return to soil",
                    e.offerId(), e.quantity(), e.productName());
            return;
        }

        FoodDonation d = FoodDonation.fromShelf(e.storeOwnerId(), e.storeName(), e.city(), e.lat(), e.lng(),
                safeUntil, props.defaultDayHighC(), e.offerId());
        // Pilot assumption: 1 unit ≈ 1 serving (a bread packet, a milk pouch). Tune with the partner NGO.
        d.addItem(e.productName(), toFoodGroup(e.category()), FoodType.PACKAGED, e.quantity(), null, e.expiryDate());
        donationRepo.save(d);

        events.publishEvent(new FoodPostedEvent(d.getId(), d.getDonorId(), d.getCity(), d.getLat(), d.getLng(),
                d.getTotalServings(), servingsByGroup(d), safeUntil, now));

        log.info("BRIDGE: Second-Life offer {} -> donation {} ({} x {} from {}, safe until {})",
                e.offerId(), d.getId(), e.quantity(), e.productName(), e.storeName(), safeUntil);
    }

    /** Shop category → Thali food group. */
    private static FoodGroup toFoodGroup(String category) {
        return switch (category) {
            case "DAIRY" -> FoodGroup.DAIRY_FRUIT;   // milk, curd, paneer
            default -> FoodGroup.GRAIN;              // BAKERY, GROCERY: bread, atta, rice
        };
    }

    /**
     * Doc C4 step 5: "NGO calls accept. Donation takes lock:donation:{id}, publishes donation.accepted."
     * Order: open & safe (R4)? → room in the shelter today? → first to accept (Redis)? → accept.
     */
    @Transactional
    public DonationResponse accept(Long ngoAccountId, Long donationId) {
        Ngo ngo = ngoService.loadMine(ngoAccountId);
        FoodDonation d = load(donationId);

        d.checkCanBeAccepted();          // open? not cancelled? still safe? → right message if not

        // Capacity: don't send a shelter more food than it can serve today
        Instant startOfToday = LocalDate.now(IST).atStartOfDay(IST).toInstant();
        long usedToday = donationRepo.servingsAcceptedSince(ngo.getId(), startOfToday, COUNTS_FOR_CAPACITY);
        long roomLeft = ngo.getCapacityPerDay() - usedToday;
        if (d.getTotalServings() > roomLeft) {
            throw NexusException.conflict("OVER_CAPACITY",
                    "You have room for " + Math.max(0, roomLeft) + " more servings today; this donation has "
                            + d.getTotalServings() + ". Update your capacity if you can take more.");
        }

        // First NGO wins (Redis SET NX). If our DB save later fails, give the lock back.
        if (!donationLock.tryAcquire(donationId, ngo.getId(), props.acceptLockTtl())) {
            throw NexusException.conflict("ALREADY_ACCEPTED", "Another NGO accepted this food a moment ago");
        }
        AfterCommit.onRollback("release donation lock " + donationId, () -> donationLock.release(donationId));

        d.accept(ngo.getId());           // re-checks; @Version also guards the row

        events.publishEvent(new DonationAcceptedEvent(
                d.getId(), d.getDonorId(), ngo.getId(), ngo.getAccountId(),
                d.getLat(), d.getLng(), ngo.getLat(), ngo.getLng(),
                d.getTotalServings(), d.getSafeUntil(), Instant.now()));

        log.info("Donation {} accepted by NGO {} '{}' ({} servings, {} min left)",
                d.getId(), ngo.getId(), ngo.getName(), d.getTotalServings(), d.minutesLeft());
        return DonationResponse.from(d);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> acceptedByMe(Long ngoAccountId) {
        Ngo ngo = ngoService.loadMine(ngoAccountId);
        return donationRepo.findByNgoIdOrderByIdDesc(ngo.getId()).stream()
                .map(DonationResponse::from)
                .toList();
    }

    // =====================================================================
    // Anyone involved
    // =====================================================================

    /** Donor who posted it, the NGO that accepted it, or admin. */
    @Transactional(readOnly = true)
    public DonationResponse get(AuthUser me, Long donationId) {
        FoodDonation d = load(donationId);
        if (d.isPostedBy(me.accountId()) || me.role() == Role.ADMIN) {
            return DonationResponse.from(d);
        }
        if (me.role() == Role.NGO) {
            Ngo ngo = ngoRepo.findByAccountId(me.accountId()).orElse(null);
            if (ngo != null && d.isAcceptedBy(ngo.getId())) {
                return DonationResponse.from(d);
            }
        }
        throw forbidden();
    }

    // =====================================================================
    // Freshness Clock enforcement (called by DonationExpirySweeper)
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Long> findIdsPastSafeTime() {
        return donationRepo.findIdsPastSafeTime(BEFORE_PICKUP, Instant.now());
    }

    /** Doc 06: "When the clock runs out, the post disappears." Idempotent. */
    @Transactional
    public boolean expireIfPastSafeTime(Long donationId) {
        FoodDonation d = donationRepo.findById(donationId).orElse(null);
        if (d == null || !d.expireIfPastSafeTime()) {
            return false;
        }
        AfterCommit.run("release donation lock " + donationId, () -> donationLock.release(donationId));
        events.publishEvent(new DonationExpiredEvent(
                d.getId(), d.getDonorId(), d.getNgoId(), d.getTotalServings(), Instant.now()));
        log.info("Donation {} EXPIRED - Freshness Clock ran out ({} servings)", d.getId(), d.getTotalServings());
        return true;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private FoodDonation load(Long id) {
        return donationRepo.findWithItemsById(id)
                .orElseThrow(() -> NexusException.notFound("Donation"));
    }

    private static Map<String, Integer> servingsByGroup(FoodDonation d) {
        return d.getItems().stream().collect(Collectors.groupingBy(
                i -> i.getFoodGroup().name(), TreeMap::new, Collectors.summingInt(DonationItem::getServings)));
    }

    private static NexusException forbidden() {
        return new NexusException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not part of this donation");
    }
}