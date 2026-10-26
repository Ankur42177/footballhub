package com.footballhub.auth;

import java.util.List;
import java.util.Locale;

import com.footballhub.common.exception.ApiException;
import com.footballhub.config.AppProperties;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies Google sign-in ID tokens on the server. The token's signature is checked against
 * Google's published keys, and it must be issued by Google, for this site's client ID, not
 * expired, carry the nonce this browser session was given, and belong to a verified email.
 */
@Component
public class GoogleTokenVerifier {

    public record GoogleIdentity(String subject, String email, String name) {
    }

    private static final String JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final AppProperties.Google google;
    private volatile JwtDecoder decoder;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleTokenVerifier(AppProperties properties) {
        this.google = properties.google();
    }

    /** For tests: a decoder backed by local keys instead of Google's. */
    GoogleTokenVerifier(AppProperties.Google google, NimbusJwtDecoder testDecoder) {
        this.google = google;
        testDecoder.setJwtValidator(validator(google.clientId()));
        this.decoder = testDecoder;
    }

    /** Signature aside: unexpired, issued by Google, and meant for this site's client ID. */
    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtClaimValidator<String>("iss", ISSUERS::contains),
                new JwtClaimValidator<List<String>>("aud", audience -> audience != null && audience.contains(clientId)));
    }

    public boolean enabled() {
        return google.enabled();
    }

    public GoogleIdentity verify(String idToken, String expectedNonce) {
        ApiException invalid = new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID",
                "Google sign-in didn't work. Please try again.");
        if (!enabled()) {
            throw ApiException.notFound("Google sign-in");
        }
        if (idToken == null || idToken.isBlank() || idToken.length() > 4096 || expectedNonce == null) {
            throw invalid;
        }
        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        }
        catch (JwtException ex) {
            throw invalid;
        }
        if (!expectedNonce.equals(jwt.getClaimAsString("nonce"))) {
            throw invalid;
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || !Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_UNVERIFIED",
                    "Google hasn't verified the email address on that account.");
        }
        return new GoogleIdentity(jwt.getSubject(), email.trim().toLowerCase(Locale.ROOT),
                jwt.getClaimAsString("name"));
    }

    private JwtDecoder decoder() {
        JwtDecoder current = decoder;
        if (current == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWKS).build();
                    nimbus.setJwtValidator(validator(google.clientId()));
                    decoder = nimbus;
                }
                current = decoder;
            }
        }
        return current;
    }
}
