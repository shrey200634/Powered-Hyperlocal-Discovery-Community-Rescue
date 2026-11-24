package com.communitynexus.donation.listener;

import com.communitynexus.donation.service.DonationService;
import com.communitynexus.inventory.event.ShelfReadyToDonateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ShelfDonationListener {

    private final DonationService donationService;

    @ApplicationModuleListener
    public void on(ShelfReadyToDonateEvent event) {
        donationService.createFromShelf(event);
    }
}