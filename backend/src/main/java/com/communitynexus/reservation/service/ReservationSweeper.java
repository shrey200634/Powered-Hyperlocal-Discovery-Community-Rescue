package com.communitynexus.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSweeper {

    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${nexus.reservation.sweep-every-ms}")
    public void sweep() {
        for (Long id : reservationService.findDueIds()) {
            try {
                reservationService.expireIfDue(id);
            } catch (Exception e) {
                log.warn("Sweeper could not expire reservation {}: {}", id, e.getMessage());
            }
        }
    }
}