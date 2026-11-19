package com.communitynexus.auth.service;

import com.communitynexus.auth.config.OtpProperties;
import com.communitynexus.auth.entity.OtpChallenge;
import com.communitynexus.auth.repo.OtpChallengeRepository;
import com.communitynexus.common.exception.NexusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();   // NOT java.util.Random — that is guessable

    private final OtpChallengeRepository otpRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties props;

    /** Creates a new OTP for this phone. Returns how many seconds it's valid. */
    @Transactional
    public long send(String phone) {
        // Cooldown: stop someone spamming "send OTP" (each SMS costs money later)
        otpRepo.findTopByPhoneAndConsumedFalseOrderByCreatedAtDesc(phone).ifPresent(last -> {
            if (last.getCreatedAt().plus(props.resendCooldown()).isAfter(Instant.now())) {
                throw new NexusException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN",
                        "Please wait a few seconds before asking for a new OTP");
            }
        });

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));   // 000000–999999
        Instant expiresAt = Instant.now().plus(props.ttl());

        otpRepo.save(OtpChallenge.create(phone, passwordEncoder.encode(otp), expiresAt));

        if (props.logToConsole()) {
            log.info("[DEV ONLY] OTP for {} is {}", phone, otp);
        }
        // TODO (Notification step): send by SMS, or voice call for seniors

        return props.ttl().toSeconds();
    }

    /**
     * Throws if wrong/expired. On success, marks the OTP as used.
     *
     * noRollbackFor: when the OTP is WRONG we save attempts+1 and THEN throw.
     * Normally an exception rolls back the transaction — the attempt count would never be saved
     * and the 5-attempt limit would never trigger. This tells Spring: keep the save.
     */
    @Transactional(noRollbackFor = NexusException.class)
    public void verify(String phone, String otp) {
        OtpChallenge challenge = otpRepo.findTopByPhoneAndConsumedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> NexusException.badRequest("OTP_NOT_FOUND", "Please request an OTP first"));

        if (challenge.isExpired()) {
            throw NexusException.badRequest("OTP_EXPIRED", "OTP expired. Please request a new one");
        }
        if (!challenge.hasAttemptsLeft()) {
            throw new NexusException(HttpStatus.TOO_MANY_REQUESTS, "OTP_TOO_MANY_ATTEMPTS",
                    "Too many wrong tries. Please request a new OTP");
        }
        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            challenge.recordFailedAttempt();
            otpRepo.save(challenge);
            throw NexusException.badRequest("OTP_INVALID", "Wrong OTP");
        }

        challenge.consume();
        otpRepo.save(challenge);
    }
}