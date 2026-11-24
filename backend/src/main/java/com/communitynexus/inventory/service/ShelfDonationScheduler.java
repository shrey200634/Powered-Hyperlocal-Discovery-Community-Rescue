package com.communitynexus.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShelfDonationScheduler {

    private final SecondLifeService secondLifeService;

    @Scheduled(fixedDelayString = "${nexus.inventory.second-life.check-every-ms}")
    public void run() {
        for (Long offerId : secondLifeService.findOfferIdsDueForDonation()) {
            try {
                secondLifeService.moveDownLadderIfDue(offerId);
            } catch (Exception e) {
                log.warn("Could not move Second-Life offer {} down the ladder: {}", offerId, e.getMessage());
            }
        }
    }
}