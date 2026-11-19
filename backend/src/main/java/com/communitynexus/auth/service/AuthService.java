package com.communitynexus.auth.service;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.auth.config.JwtProperties;
import com.communitynexus.auth.dto.*;
import com.communitynexus.auth.entity.Account;
import com.communitynexus.auth.entity.RefreshToken;
import com.communitynexus.auth.event.UserRegisteredEvent;
import com.communitynexus.auth.repo.AccountRepository;
import com.communitynexus.auth.repo.RefreshTokenRepository;
import com.communitynexus.auth.security.JwtService;
import com.communitynexus.common.exception.NexusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpService otpService;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;
    private final AccountRepository accountRepo;
    private final RefreshTokenRepository refreshRepo;
    private final ApplicationEventPublisher events;

    // ---------------- OTP ----------------

    public OtpSendResponse sendOtp(String rawPhone) {
        long ttl = otpService.send(normalizePhone(rawPhone));
        return new OtpSendResponse("OTP sent", ttl);
    }

    @Transactional(noRollbackFor = NexusException.class)
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest req) {
        String phone = normalizePhone(req.phone());
        otpService.verify(phone, req.otp());

        return accountRepo.findByPhone(phone)
                .map(account -> {
                    ensureActive(account);
                    account.markLoggedIn();
                    return OtpVerifyResponse.existingUser(issueTokens(account));
                })
                .orElseGet(() -> OtpVerifyResponse.newUser(jwtService.createSignupToken(phone)));
    }

    // ---------------- SIGNUP ----------------

    @Transactional
    public TokenResponse signup(SignupRequest req) {
        String phone = jwtService.parseSignupToken(req.signupToken());   // proves OTP was verified

        if (!req.role().isSelfSignupAllowed()) {
            throw NexusException.badRequest("ROLE_NOT_ALLOWED", "This role cannot be chosen at signup");
        }
        if (accountRepo.existsByPhone(phone)) {
            throw NexusException.conflict("ACCOUNT_EXISTS", "This phone is already registered. Please log in");
        }

        Account account = accountRepo.save(Account.create(phone, req.role(), req.language()));
        account.markLoggedIn();

        // Tell the rest of the app. User module (Step 3) will create the profile with the name.
        events.publishEvent(new UserRegisteredEvent(
                account.getId(), account.getRole(), phone, account.getLanguage(),
                req.name().trim(), Instant.now()));

        log.info("New account {} registered as {}", account.getId(), account.getRole());
        return issueTokens(account);
    }

    // ---------------- REFRESH / LOGOUT ----------------

    /**
     * Rotation: every refresh kills the old refresh token and gives a new one.
     * Reuse detection: if a REVOKED token is used again, someone may have stolen it →
     * we revoke ALL of that account's tokens (forces login on every device).
     */
    @Transactional(noRollbackFor = NexusException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshRepo.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(AuthService::invalidRefresh);

        Account account = stored.getAccount();

        if (stored.isRevoked()) {
            refreshRepo.revokeAllForAccount(account.getId());
            log.warn("Refresh token reuse detected for account {} — all sessions revoked", account.getId());
            throw invalidRefresh();
        }
        if (!stored.isUsable()) {
            throw invalidRefresh();
        }
        ensureActive(account);

        stored.revoke();
        return issueTokens(account);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshRepo.findByTokenHash(sha256(rawRefreshToken)).ifPresent(RefreshToken::revoke);
        // Always "success" — don't tell a caller whether a token existed
    }

    // ---------------- helpers ----------------

    private TokenResponse issueTokens(Account account) {
        String accessToken = jwtService.createAccessToken(account.getId(), account.getRole());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshRepo.save(RefreshToken.create(account, sha256(rawRefresh),
                Instant.now().plus(jwtProps.refreshTtl())));

        return new TokenResponse(accessToken, rawRefresh, jwtService.accessTtlSeconds(),
                account.getId(), account.getRole());
    }

    private static void ensureActive(Account account) {
        if (!account.isActive()) {
            throw new NexusException(HttpStatus.FORBIDDEN, "ACCOUNT_BLOCKED",
                    "Your account is paused. Please contact the Nexus team");
        }
    }

    private static NexusException invalidRefresh() {
        return new NexusException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID", "Session expired. Please log in again");
    }

    /** "9876543210" or "+919876543210" → "+919876543210" (one format in the DB). */
    static String normalizePhone(String phone) {
        String p = phone.trim();
        return p.startsWith("+91") ? p : "+91" + p;
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);   // 64 hex chars → fits token_hash(64)
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // used by /auth/me
    public AuthUser me(AuthUser user) {
        return user;
    }
}