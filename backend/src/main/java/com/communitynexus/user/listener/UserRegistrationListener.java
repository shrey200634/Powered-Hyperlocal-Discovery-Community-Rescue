package com.communitynexus.user.listener;

import com.communitynexus.auth.event.UserRegisteredEvent;
import com.communitynexus.user.entity.UserProfile;
import com.communitynexus.user.entity.Verification;
import com.communitynexus.user.repo.UserProfileRepository;
import com.communitynexus.user.repo.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Doc C1: "user.registered creates an empty profile for the chosen role."
 *
 * @ApplicationModuleListener =
 *   runs AFTER the signup transaction commits (never sees a rolled-back signup)
 *   + on a background thread (signup response doesn't wait)
 *   + in its own new transaction
 *   + tracked in event_publication (outbox) → retried on restart if it failed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final UserProfileRepository profileRepo;
    private final VerificationRepository verificationRepo;

    @ApplicationModuleListener
    public void on(UserRegisteredEvent event) {
        // Idempotent: a retried event must not create a second profile (doc C3:
        // "Consumers ignore an event id they have already handled")
        if (profileRepo.existsByAccountId(event.accountId())) {
            log.info("Profile for account {} already exists - skipping", event.accountId());
            return;
        }

        UserProfile profile = profileRepo.save(UserProfile.create(
                event.accountId(), event.name(), event.role(), event.language(), event.phone()));

        if (event.role().requiresVerification()) {
            verificationRepo.save(Verification.pending(profile));
        }

        log.info("Profile {} created for account {} ({})", profile.getId(), event.accountId(), event.role());
    }
}