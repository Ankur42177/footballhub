package com.footballhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.footballhub.auth.GoogleTokenVerifier;
import com.footballhub.auth.GoogleTokenVerifier.GoogleIdentity;
import com.footballhub.common.exception.ApiException;
import com.footballhub.user.User;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Google sign-in end to end, with Google's token check replaced (it's covered with real
 * signatures in GoogleTokenVerifierTest). Also covers the optional "confirm your email
 * before logging in" rule, switched on for this class.
 */
@TestPropertySource(properties = "app.require-verified-email-to-login=true")
class GoogleSignInIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private GoogleTokenVerifier google;

    /** The browser's session cookie (Spring Session keeps sessions in the database). */
    private jakarta.servlet.http.Cookie sessionCookie;

    @BeforeEach
    void googleIsConfigured() {
        when(google.enabled()).thenReturn(true);
        when(google.verify(any(), any())).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Google sign-in didn't work."));
    }

    @Test
    void newGoogleUserGetsAVerifiedMemberAccount() throws Exception {
        String nonce = start();
        doReturn(new GoogleIdentity("g-1", "new.fan@gmail.com", "New Fan")).when(google).verify("good-token", nonce);

        googleLogin("good-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_fan"))
                .andExpect(jsonPath("$.displayName").value("New Fan"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.emailVerified").value(true));
        assertThat(userRepository.findByGoogleSubject("g-1")).isPresent();
    }

    @Test
    void existingAccountWithTheSameEmailIsLinkedAndAdminEmailsGetAdmin() throws Exception {
        User existing = userRepository.saveAndFlush(
                new User("chief", "chief@example.test", passwordEncoder.encode(PASSWORD), "The Chief"));
        String nonce = start();
        doReturn(new GoogleIdentity("g-2", "chief@example.test", "Chief")).when(google).verify("good-token", nonce);

        googleLogin("good-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        assertThat(userRepository.findById(existing.getId()).orElseThrow().getGoogleSubject()).isEqualTo("g-2");
    }

    @Test
    void theNonceMustComeFromThisSessionAndIsSingleUse() throws Exception {
        String nonce = start();
        doReturn(new GoogleIdentity("g-3", "a.b@gmail.com", "AB")).when(google).verify("good-token", nonce);

        // Another browser (no session) can't use the token.
        mvc.perform(post("/api/v1/auth/google").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\": \"good-token\"}"))
                .andExpect(status().isUnauthorized());

        googleLogin("good-token").andExpect(status().isOk());
        // The nonce was consumed, so replaying the same token fails.
        googleLogin("good-token").andExpect(status().isUnauthorized());
    }

    @Test
    void suspendedAccountsCannotSignInWithGoogle() throws Exception {
        User banned = new User("banned", "banned@gmail.com", passwordEncoder.encode(PASSWORD), "Banned");
        banned.setEnabled(false);
        userRepository.saveAndFlush(banned);
        String nonce = start();
        doReturn(new GoogleIdentity("g-4", "banned@gmail.com", "Banned")).when(google).verify("good-token", nonce);

        googleLogin("good-token")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void unverifiedEmailsCannotLogInWithAPasswordWhenTheRuleIsOn() throws Exception {
        userRepository.saveAndFlush(new User("newbie", "newbie@example.com", passwordEncoder.encode(PASSWORD), "Newbie"));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"newbie\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        User admin = new User("boss", "boss@example.com", passwordEncoder.encode(PASSWORD), "Boss");
        admin.setRole(com.footballhub.user.Role.ADMIN);
        userRepository.saveAndFlush(admin);
        // Administrators are exempt, so the site owner can't be locked out.
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"boss\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    private String start() throws Exception {
        var response = mvc.perform(get("/api/v1/auth/google/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("test-client-id"))
                .andReturn().getResponse();
        sessionCookie = response.getCookie("SESSION");
        assertThat(sessionCookie).as("session cookie").isNotNull();
        return JsonPath.read(response.getContentAsString(), "$.nonce");
    }

    private org.springframework.test.web.servlet.ResultActions googleLogin(String credential) throws Exception {
        return mvc.perform(post("/api/v1/auth/google").cookie(sessionCookie).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"credential\": \"" + credential + "\"}"));
    }
}
