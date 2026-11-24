package com.communitynexus.donation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class DonationExpirySweeper {

    private final DonationService donationService;

    @Scheduled(fixedDelayString = "${nexus.donation.sweep-every-ms}")
    public void sweep() {
        for (Long id : donationService.findIdsPastSafeTime()) {
            try {
                donationService.expireIfPastSafeTime(id);
            } catch (Exception e) {
                log.warn("Could not expire donation {}: {}", id, e.getMessage());
            }
        }
    }
}