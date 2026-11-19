package com.communitynexus.auth.config;

import com.communitynexus.auth.Role;
import com.communitynexus.auth.entity.Account;
import com.communitynexus.auth.event.UserRegisteredEvent;
import com.communitynexus.auth.repo.AccountRepository;
import com.communitynexus.common.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * ADMIN cannot sign up (Role.isSelfSignupAllowed). So at startup we create one admin
 * from ADMIN_PHONE in .env — only if it doesn't exist yet. The admin then logs in with OTP like anyone else.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final AccountRepository accountRepo;
    private final ApplicationEventPublisher events;

    @Value("${nexus.admin.phone:}")
    private String adminPhone;

    @Override
    @Transactional   // account save + event go into the outbox together
    public void run(ApplicationArguments args) {
        if (adminPhone == null || adminPhone.isBlank()) {
            return;
        }
        String phone = adminPhone.trim().startsWith("+91") ? adminPhone.trim() : "+91" + adminPhone.trim();

        if (accountRepo.existsByPhone(phone)) {
            return;
        }

        Account admin = accountRepo.save(Account.create(phone, Role.ADMIN, Language.EN));
        events.publishEvent(new UserRegisteredEvent(
                admin.getId(), Role.ADMIN, phone, Language.EN, "Nexus Admin", Instant.now()));

        log.info("Admin account {} created", admin.getId());
    }
}