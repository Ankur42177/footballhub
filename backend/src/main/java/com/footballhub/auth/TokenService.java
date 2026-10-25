package com.footballhub.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.footballhub.common.exception.ApiException;
import com.footballhub.user.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues and redeems single-use, expiring tokens for email verification and password reset. */
@Service
@Transactional
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserTokenRepository tokens;

    public TokenService(UserTokenRepository tokens) {
        this.tokens = tokens;
    }

    /** Returns the raw token. It is shown to the user once (by email) and never stored. */
    public String issue(User user, TokenType type, Duration timeToLive) {
        Instant now = Instant.now();
        tokens.invalidateOutstanding(user.getId(), type, now);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new UserToken(user, hash(raw), type, now.plus(timeToLive)));
        return raw;
    }

    public User consume(String rawToken, TokenType type) {
        ApiException invalid = ApiException.badRequest("INVALID_TOKEN",
                "This link is invalid or has expired. Please request a new one.");
        if (rawToken == null || rawToken.isBlank()) {
            throw invalid;
        }
        UserToken token = tokens.findByTokenHashAndType(hash(rawToken.trim()), type).orElseThrow(() -> invalid);
        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw invalid;
        }
        if (tokens.markUsed(token.getId(), now) != 1) {
            throw invalid;
        }
        return token.getUser();
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
