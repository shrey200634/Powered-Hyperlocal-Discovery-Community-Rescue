package com.communitynexus.donation.service;

import com.communitynexus.common.exception.NexusException;
import com.communitynexus.donation.config.DonationProperties;
import com.communitynexus.donation.dto.DonationResponse;
import com.communitynexus.donation.dto.PostDonationRequest;
import com.communitynexus.donation.entity.DonationItem;
import com.communitynexus.donation.entity.FoodDonation;
import com.communitynexus.donation.event.FoodPostedEvent;
import com.communitynexus.donation.freshness.FreshnessClock;
import com.communitynexus.donation.repo.FoodDonationRepository;
import com.communitynexus.user.UserDirectory;
import com.communitynexus.user.UserSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(10);
    private static final Duration MAX_AGE = Duration.ofHours(24);

    private final FoodDonationRepository donationRepo;
    private final FreshnessClock freshnessClock;
    private final DonationProperties props;
    private final UserDirectory userDirectory;
    private final ApplicationEventPublisher events;

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

        Map<String, Integer> byGroup = d.getItems().stream()
                .collect(Collectors.groupingBy(i -> i.getFoodGroup().name(),
                        Collectors.summingInt(DonationItem::getServings)));

        events.publishEvent(new FoodPostedEvent(d.getId(), donorId, d.getCity(), d.getLat(), d.getLng(),
                d.getTotalServings(), byGroup, safeUntil, now));

        log.info("Donation {} posted by {}: {} servings, safe until {} ({} min left)",
                d.getId(), donorName, d.getTotalServings(), safeUntil, d.minutesLeft());
        return DonationResponse.from(d);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> mine(Long donorId) {
        return donationRepo.findByDonorIdOrderByIdDesc(donorId).stream().map(DonationResponse::from).toList();
    }

    @Transactional
    public DonationResponse cancel(Long donorId, Long donationId) {
        FoodDonation d = donationRepo.findWithItemsById(donationId)
                .orElseThrow(() -> NexusException.notFound("Donation"));
        if (!d.isPostedBy(donorId)) {
            throw new NexusException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This donation is not yours");
        }
        d.cancelByDonor();
        return DonationResponse.from(d);
    }
}