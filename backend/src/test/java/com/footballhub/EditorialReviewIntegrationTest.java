package com.footballhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.footballhub.CapturingMailConfiguration.CapturingMailService;
import com.footballhub.CapturingMailConfiguration.SentMail;
import com.footballhub.security.AppUserDetails;
import com.footballhub.user.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Only administrators publish directly. Everyone else's stories wait for review: the reviewer
 * gets an email with a single-use link, or decides from the admin page.
 */
class EditorialReviewIntegrationTest extends IntegrationTestSupport {

    private static final Pattern LINK_TOKEN = Pattern.compile("/review#token=([A-Za-z0-9_-]+)");

    @Autowired
    private CapturingMailService mail;

    @BeforeEach
    void clearMail() {
        mail.clear();
    }

    @Test
    void adminsPublishDirectlyAndNoReviewEmailIsSent() throws Exception {
        AppUserDetails admin = createUser("boss", Role.ADMIN);
        String json = submit(admin, "Straight to print");
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(json, "$.status")).isEqualTo("PUBLISHED");
        assertThat(mail.sent()).isEmpty();
    }

    @Test
    void otherUsersStoriesWaitForReviewAndStayPrivate() throws Exception {
        AppUserDetails fan = createUser("fan");
        String json = submit(fan, "My big take");
        String slug = slugOf(json);
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(json, "$.status")).isEqualTo("PENDING_REVIEW");

        mvc.perform(get("/api/v1/blogs/" + slug)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/blogs?q=big%20take")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/blogs/" + slug).with(user(fan)))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        SentMail sent = onlyMail();
        assertThat(sent.to()).isEqualTo("reviewer@example.test");
        assertThat(sent.subject()).isEqualTo("Review request: My big take");
        assertThat(sent.body()).contains("My big take", "@fan", "A body worth reviewing.")
                .doesNotContain("fan@example.com");
    }

    @Test
    void reviewLinkApprovesOnceAndPublishes() throws Exception {
        AppUserDetails fan = createUser("fan");
        String slug = slugOf(submit(fan, "Approve me"));
        String token = tokenFrom(onlyMail());

        mvc.perform(post("/api/v1/reviews/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Approve me"))
                .andExpect(jsonPath("$.author.username").value("fan"))
                .andExpect(jsonPath("$.author.email").doesNotExist());

        mvc.perform(post("/api/v1/reviews/approve").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/blogs/" + slug)).andExpect(status().isOk());

        // The link is single-use.
        mvc.perform(post("/api/v1/reviews/approve").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_LINK"));
    }

    @Test
    void reviewLinkCanRejectWithANoteForTheAuthor() throws Exception {
        AppUserDetails fan = createUser("fan");
        String slug = slugOf(submit(fan, "Too spicy"));
        String token = tokenFrom(onlyMail());

        mvc.perform(post("/api/v1/reviews/reject").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"note\": \"Please add sources.\"}"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(get("/api/v1/blogs/" + slug)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/blogs/" + slug).with(user(fan)))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewNote").value("Please add sources."));
    }

    @Test
    void forgedOrMissingTokensAreRejectedTheSameWay() throws Exception {
        for (String token : new String[] { "not-a-real-token", "x".repeat(43) }) {
            mvc.perform(post("/api/v1/reviews/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(tokenBody(token)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REVIEW_LINK"));
        }
        mvc.perform(post("/api/v1/reviews/approve").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminsCanUseTheReviewQueue() throws Exception {
        AppUserDetails fan = createUser("fan");
        AppUserDetails moderator = createUser("mod", Role.MODERATOR);
        long id = idOf(submit(fan, "Queue me"));

        mvc.perform(get("/api/v1/admin/reviews").with(user(fan))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/reviews").with(user(moderator))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/reviews/" + id + "/approve").with(user(moderator)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/blogs/" + id + "/publish").with(user(fan)).with(csrf()))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        mvc.perform(get("/api/v1/admin/reviews").with(user(editor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Queue me"));
        mvc.perform(post("/api/v1/admin/reviews/" + id + "/approve").with(user(editor())).with(csrf()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        // Deciding from the admin page also kills the emailed link.
        mvc.perform(post("/api/v1/reviews/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(tokenFrom(onlyMail()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editingAPublishedStoryAsANonAdminSendsItBackToReview() throws Exception {
        AppUserDetails fan = createUser("fan");
        String json = createBlog(fan, "Live story", true);
        long id = idOf(json);
        mail.clear();

        mvc.perform(put("/api/v1/blogs/" + id).with(user(fan)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Live story\", \"content\": \"Swapped content.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(get("/api/v1/blogs/" + slugOf(json))).andExpect(status().isNotFound());
        assertThat(onlyMail().body()).contains("Swapped content.");

        // While it's waiting, the author can't change what the reviewer was sent.
        mvc.perform(put("/api/v1/blogs/" + id).with(user(fan)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Live story\", \"content\": \"Sneaky edit.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IN_REVIEW"));
    }

    @Test
    void adminsCanEditAndDeleteAnyStory() throws Exception {
        AppUserDetails fan = createUser("fan");
        long id = idOf(createBlog(fan, "Fan piece", true));
        mvc.perform(put("/api/v1/blogs/" + id).with(user(editor())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Fan piece\", \"content\": \"Edited by the editor.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/blogs/" + id)
                        .with(user(editor())).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void authorsCanOnlyHaveAFewStoriesWaiting() throws Exception {
        AppUserDetails fan = createUser("fan");
        submit(fan, "One");
        submit(fan, "Two");
        submit(fan, "Three");
        mvc.perform(post("/api/v1/blogs").with(user(fan)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Four\", \"content\": \"Body.\", \"publish\": true}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_PENDING"));
    }

    private String submit(AppUserDetails author, String title) throws Exception {
        return mvc.perform(post("/api/v1/blogs").with(user(author)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\", \"content\": \"A body worth reviewing.\", \"publish\": true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private SentMail onlyMail() {
        assertThat(mail.sent()).hasSize(1);
        return mail.sent().get(0);
    }

    private static String tokenFrom(SentMail sent) {
        Matcher matcher = LINK_TOKEN.matcher(sent.body());
        assertThat(matcher.find()).as("review link in email").isTrue();
        return matcher.group(1);
    }

    private static String tokenBody(String token) {
        return "{\"token\": \"" + token + "\"}";
    }
}
