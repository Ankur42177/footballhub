package com.footballhub;

import com.footballhub.auth.TokenService;
import com.footballhub.auth.TokenType;
import com.footballhub.security.AppUserDetails;
import com.footballhub.user.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TokenService tokenService;

    @Test
    void registerValidUserCreatesAccountAndSignsIn() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "Kevin_D", "email": "Kevin@Example.com",
                         "password": "%s", "displayName": "Kevin"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("kevin_d"))
                .andExpect(jsonPath("$.email").value("kevin@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andReturn();

        Cookie session = sessionCookie(result);
        mvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("kevin_d"));

        String hash = userRepository.findByUsername("kevin_d").map(User::getPasswordHash).orElseThrow();
        assertThat(hash).startsWith("{argon2}");
    }

    @Test
    void registerDuplicateEmailIsRejected() throws Exception {
        createUser("first");
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "second", "email": "first@example.com",
                         "password": "%s", "displayName": "Second"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void registerValidatesFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "a b", "email": "not-an-email", "password": "short", "displayName": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        createUser("striker");
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "striker", "password": "wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithEmailThenLogoutEndsTheSession() throws Exception {
        createUser("keeper");
        MvcResult login = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "KEEPER@example.com", "password": "%s"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("keeper"))
                .andReturn();
        Cookie session = sessionCookie(login);

        mvc.perform(get("/api/v1/users/me").cookie(session)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").cookie(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotReadProfileEndpoint() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        AppUserDetails author = createUser("author");
        mvc.perform(post("/api/v1/blogs").with(user(author))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "No token", "content": "Body"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordResetTokenIsSingleUse() throws Exception {
        createUser("winger");
        User user = userRepository.findByUsername("winger").orElseThrow();
        String token = tokenService.issue(user, TokenType.RESET_PASSWORD, java.time.Duration.ofHours(1));

        String body = """
                {"token": "%s", "newPassword": "brand-new-password"}
                """.formatted(token);
        mvc.perform(post("/api/v1/auth/reset-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/reset-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "winger", "password": "brand-new-password"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPasswordDoesNotRevealWhetherAnAccountExists() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nobody@example.com"}
                        """))
                .andExpect(status().isAccepted());
    }

    @Test
    void emailVerificationMarksAccountVerified() throws Exception {
        createUser("captain");
        User user = userRepository.findByUsername("captain").orElseThrow();
        String token = tokenService.issue(user, TokenType.VERIFY_EMAIL, java.time.Duration.ofHours(1));
        mvc.perform(post("/api/v1/auth/verify-email").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(token)))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findByUsername("captain").orElseThrow().isEmailVerified()).isTrue();
    }

    /** The Spring Session cookie issued by the response (the last one wins if it was rotated). */
    private static Cookie sessionCookie(MvcResult result) {
        Cookie session = null;
        for (Cookie cookie : result.getResponse().getCookies()) {
            if ("SESSION".equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
                session = cookie;
            }
        }
        assertThat(session).as("SESSION cookie").isNotNull();
        return new Cookie("SESSION", session.getValue());
    }
}
