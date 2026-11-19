package com.communitynexus.auth.controller;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.auth.dto.*;
import com.communitynexus.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/send")
    public OtpSendResponse sendOtp(@Valid @RequestBody OtpSendRequest req) {
        return authService.sendOtp(req.phone());
    }

    @PostMapping("/otp/verify")
    public OtpVerifyResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return authService.verifyOtp(req);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
    }

    /** Protected: needs "Authorization: Bearer <accessToken>". */
    @GetMapping("/me")
    public AuthUser me(@AuthenticationPrincipal AuthUser me) {
        return me;
    }
}