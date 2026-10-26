package com.footballhub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.footballhub.common.exception.ApiException;
import com.footballhub.config.AppProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Real signature and claim checks, with a local RSA key standing in for Google's. */
class GoogleTokenVerifierTest {

    private static KeyPair keys;
    private static KeyPair otherKeys;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        otherKeys = generator.generateKeyPair();
    }

    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(new AppProperties.Google("my-client"),
            NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build());

    @Test
    void acceptsAValidTokenWithTheRightNonce() throws Exception {
        var identity = verifier.verify(token(claims().build(), keys), "n-123");
        assertThat(identity.subject()).isEqualTo("google-sub-1");
        assertThat(identity.email()).isEqualTo("fan@gmail.com");
        assertThat(identity.name()).isEqualTo("A Fan");
    }

    @Test
    void rejectsWrongNonceAudienceIssuerExpiryOrSignature() throws Exception {
        assertInvalid(token(claims().build(), keys), "other-nonce");
        assertInvalid(token(claims().build(), keys), null);
        assertInvalid(token(claims().audience("someone-else").build(), keys), "n-123");
        assertInvalid(token(claims().issuer("https://evil.example").build(), keys), "n-123");
        assertInvalid(token(claims().expirationTime(Date.from(Instant.now().minusSeconds(3600))).build(), keys), "n-123");
        assertInvalid(token(claims().build(), otherKeys), "n-123");
        assertInvalid("not-a-jwt", "n-123");
    }

    @Test
    void rejectsAccountsWhoseEmailGoogleHasNotVerified() throws Exception {
        String unverified = token(claims().claim("email_verified", false).build(), keys);
        assertThatThrownBy(() -> verifier.verify(unverified, "n-123"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("GOOGLE_EMAIL_UNVERIFIED");
    }

    private void assertInvalid(String idToken, String nonce) {
        assertThatThrownBy(() -> verifier.verify(idToken, nonce))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("GOOGLE_TOKEN_INVALID");
    }

    private static JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(List.of("my-client"))
                .subject("google-sub-1")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .claim("email", "Fan@Gmail.com")
                .claim("email_verified", true)
                .claim("name", "A Fan")
                .claim("nonce", "n-123");
    }

    private static String token(JWTClaimsSet claims, KeyPair signingKeys) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(signingKeys.getPrivate()));
        return jwt.serialize();
    }
}
