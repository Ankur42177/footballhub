package com.footballhub.vote;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Identifies a visitor who isn't signed in, so they can upvote once per article. The ID is
 * random (not derived from anything personal) and lives in an HTTP-only cookie for a year.
 */
@Component
public class VisitorCookies {

    static final String NAME = "FH_VISITOR";
    private static final Duration MAX_AGE = Duration.ofDays(365);

    private final boolean secure;

    public VisitorCookies(@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public Optional<UUID> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()))
                .findFirst()
                .flatMap(cookie -> parse(cookie.getValue()));
    }

    public UUID readOrCreate(HttpServletRequest request, HttpServletResponse response) {
        return read(request).orElseGet(() -> {
            UUID id = UUID.randomUUID();
            ResponseCookie cookie = ResponseCookie.from(NAME, id.toString())
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(MAX_AGE)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return id;
        });
    }

    private static Optional<UUID> parse(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        }
        catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
