package com.footballhub;

import com.footballhub.security.AppUserDetails;
import com.footballhub.user.Role;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlogIntegrationTest extends IntegrationTestSupport {

    @Test
    void guestCannotCreateBlog() throws Exception {
        mvc.perform(post("/api/v1/blogs").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Guest post", "content": "Body"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void draftIsPrivateUntilPublished() throws Exception {
        AppUserDetails author = createUser("pep");
        String draft = createBlog(author, "Inverted full-backs explained", false);
        String slug = slugOf(draft);
        long id = idOf(draft);
        assertThat(slug).isEqualTo("inverted-full-backs-explained");

        mvc.perform(get("/api/v1/blogs/" + slug)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/blogs/" + slug).with(user(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.canEdit").value(true));

        mvc.perform(post("/api/v1/blogs/" + id + "/publish").with(user(author)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mvc.perform(get("/api/v1/blogs/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.username").value("pep"))
                .andExpect(jsonPath("$.club.slug").value("manchester-city"))
                .andExpect(jsonPath("$.canEdit").value(false));
    }

    @Test
    void userCannotOpenAnotherUsersDraft() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails other = createUser("other");
        String slug = slugOf(createBlog(author, "Secret draft", false));
        mvc.perform(get("/api/v1/blogs/" + slug).with(user(other))).andExpect(status().isNotFound());
    }

    @Test
    void moderatorCanPreviewDrafts() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails moderator = createUser("mod", Role.MODERATOR);
        String slug = slugOf(createBlog(author, "Needs review", false));
        mvc.perform(get("/api/v1/blogs/" + slug).with(user(moderator))).andExpect(status().isOk());
    }

    @Test
    void userCannotEditOrDeleteAnotherUsersBlog() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails intruder = createUser("intruder");
        long id = idOf(createBlog(author, "My analysis", true));

        mvc.perform(put("/api/v1/blogs/" + id).with(user(intruder)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Hijacked", "content": "Changed"}
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/blogs/" + id).with(user(intruder)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditAnyBlogAndTheActionIsAudited() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails admin = createUser("boss", Role.ADMIN);
        long id = idOf(createBlog(author, "Original title", true));

        mvc.perform(put("/api/v1/blogs/" + id).with(user(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Corrected title", "content": "Fixed a typo"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Corrected title"))
                .andExpect(jsonPath("$.slug").value("original-title"));
        Integer audits = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = 'BLOG_EDITED'",
                Integer.class);
        assertThat(audits).isEqualTo(1);
    }

    @Test
    void duplicateTitlesGetUniqueSlugs() throws Exception {
        AppUserDetails author = createUser("author");
        String first = slugOf(createBlog(author, "Derby day", true));
        String second = slugOf(createBlog(author, "Derby day", true));
        assertThat(first).isEqualTo("derby-day");
        assertThat(second).startsWith("derby-day-").isNotEqualTo(first);
    }

    @Test
    void unknownClubIsRejected() throws Exception {
        AppUserDetails author = createUser("author");
        mvc.perform(post("/api/v1/blogs").with(user(author)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Lost", "content": "Body", "clubId": 99999}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CLUB"));
    }

    @Test
    void feedFiltersByClubAndSearchAndHidesDrafts() throws Exception {
        AppUserDetails author = createUser("author");
        createBlog(author, "Pressing traps", true);
        createBlog(author, "Unpublished idea", false);

        mvc.perform(get("/api/v1/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Pressing traps"));
        mvc.perform(get("/api/v1/blogs").param("club", "manchester-city"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/blogs").param("club", "arsenal"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/blogs").param("q", "TRAPS"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/blogs").param("q", "100%_"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/blogs").param("sort", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authorDashboardIncludesDrafts() throws Exception {
        AppUserDetails author = createUser("author");
        createBlog(author, "Draft one", false);
        createBlog(author, "Live one", true);
        mvc.perform(get("/api/v1/users/me/blogs").with(user(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/users/me/blogs").param("status", "draft").with(user(author)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deletingABlogRemovesItsCommunityData() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        long id = idOf(createBlog(author, "Short-lived", true));
        mvc.perform(post("/api/v1/blogs/" + id + "/vote").with(user(fan)).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/blogs/" + id + "/bookmark").with(user(fan)).with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/blogs/" + id).with(user(author)).with(csrf()))
                .andExpect(status().isNoContent());
        Integer leftovers = jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM blog_votes) + (SELECT COUNT(*) FROM bookmarks)", Integer.class);
        assertThat(leftovers).isZero();
    }
}
