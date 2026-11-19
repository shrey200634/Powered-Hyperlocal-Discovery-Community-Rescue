package com.communitynexus.auth.security;

import com.communitynexus.auth.AuthUser;
import com.communitynexus.auth.Role;
import com.communitynexus.auth.config.JwtProperties;
import com.communitynexus.common.exception.NexusException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;


@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_SIGNUP = "SIGNUP";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        // HMAC-SHA key from the base64 secret in .env (must be at least 32 bytes)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    // ---------- create ----------

    public String createAccessToken(Long accountId, Role role) {
        return build(String.valueOf(accountId),
                Map.of(CLAIM_TYPE, TYPE_ACCESS, CLAIM_ROLE, role.name()),
                props.accessTtl());
    }

    public String createSignupToken(String phone) {
        return build(phone, Map.of(CLAIM_TYPE, TYPE_SIGNUP), props.signupTtl());
    }

    public long accessTtlSeconds() {
        return props.accessTtl().toSeconds();
    }

    // ---------- read ----------

    /** Empty if the token is missing, tampered, expired, or not an ACCESS token. */
    public Optional<AuthUser> parseAccessToken(String token) {
        try {
            Claims c = parse(token);
            if (!TYPE_ACCESS.equals(c.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            Long accountId = Long.valueOf(c.getSubject());
            Role role = Role.valueOf(c.get(CLAIM_ROLE, String.class));
            return Optional.of(new AuthUser(accountId, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Returns the phone inside a valid SIGNUP token, otherwise throws 400. */
    public String parseSignupToken(String token) {
        try {
            Claims c = parse(token);
            if (!TYPE_SIGNUP.equals(c.get(CLAIM_TYPE, String.class))) {
                throw new JwtException("wrong token type");
            }
            return c.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw NexusException.badRequest("SIGNUP_TOKEN_INVALID",
                    "Signup session expired. Please verify your phone again.");
        }
    }

    // ---------- helpers ----------

    private String build(String subject, Map<String, Object> claims, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Verifies the signature AND expiry; throws JwtException if either fails. */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}