package com.footballhub;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Browsers send an Origin header on every POST. Requests arrive through the frontend's
 * /api/v1 rewrite, so Origin is the site's URL (app.frontend-url, http://localhost:3000 in
 * tests) while the API runs elsewhere. That origin must be allowed; others must not be.
 */
class CorsOriginIntegrationTest extends IntegrationTestSupport {

    private static final String BODY = """
            {"username": "browserfan", "email": "browserfan@example.com",
             "password": "long-enough-pw", "displayName": "Browser Fan"}
            """;

    @Test
    void registrationFromTheSiteOriginIsAllowed() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("browserfan"));
    }

    @Test
    void loginFromTheSiteOriginReachesTheApi() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "nobody", "password": "wrong-password"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherOriginsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                .header("Origin", "https://evil.example")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
}
