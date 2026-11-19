package com.communitynexus.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Existing user → newUser=false + tokens.
 * New user      → newUser=true  + signupToken (send it to /auth/signup).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpVerifyResponse(
        boolean newUser,
        String signupToken,
        TokenResponse tokens
) {
    public static OtpVerifyResponse existingUser(TokenResponse tokens) {
        return new OtpVerifyResponse(false, null, tokens);
    }

    public static OtpVerifyResponse newUser(String signupToken) {
        return new OtpVerifyResponse(true, signupToken, null);
    }
}