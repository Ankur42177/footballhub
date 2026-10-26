package com.footballhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import com.footballhub.security.AppUserDetails;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Visitors who aren't signed in can upvote once per article, tracked by a visitor cookie. */
class AnonymousVoteIntegrationTest extends IntegrationTestSupport {

    @Test
    void guestCanUpvoteAndGetsAVisitorCookie() throws Exception {
        long blogId = publishedBlog();
        MvcResult result = mvc.perform(post("/api/v1/blogs/" + blogId + "/vote").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(1))
                .andExpect(jsonPath("$.voted").value(true))
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("FH_VISITOR=").contains("HttpOnly").contains("SameSite=Lax");
    }

    @Test
    void sameVisitorVotingTwiceCountsOnce() throws Exception {
        long blogId = publishedBlog();
        Cookie visitor = firstVote(blogId);
        mvc.perform(post("/api/v1/blogs/" + blogId + "/vote").with(csrf()).cookie(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(1));
    }

    @Test
    void differentVisitorsAndSignedInUsersAllCount() throws Exception {
        long blogId = publishedBlog();
        firstVote(blogId);
        firstVote(blogId);
        AppUserDetails fan = createUser("fan");
        mvc.perform(post("/api/v1/blogs/" + blogId + "/vote").with(user(fan)).with(csrf()))
                .andExpect(jsonPath("$.voteCount").value(3));
    }

    @Test
    void guestCanRemoveTheirVoteAndStatusReflectsIt() throws Exception {
        long blogId = publishedBlog();
        Cookie visitor = firstVote(blogId);
        mvc.perform(get("/api/v1/blogs/" + blogId + "/vote").cookie(visitor))
                .andExpect(jsonPath("$.voted").value(true))
                .andExpect(jsonPath("$.voteCount").value(1));

        mvc.perform(delete("/api/v1/blogs/" + blogId + "/vote").with(csrf()).cookie(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(0))
                .andExpect(jsonPath("$.voted").value(false));

        // A visitor with no cookie can't remove anyone's vote.
        Cookie other = firstVote(blogId);
        mvc.perform(delete("/api/v1/blogs/" + blogId + "/vote").with(csrf()))
                .andExpect(jsonPath("$.voteCount").value(1));
        mvc.perform(get("/api/v1/blogs/" + blogId + "/vote").cookie(other))
                .andExpect(jsonPath("$.voted").value(true));
    }

    @Test
    void guestsCannotVoteOnDraftsOrWithoutACsrfToken() throws Exception {
        AppUserDetails author = createUser("author");
        long draftId = idOf(createBlog(author, "Secret draft", false));
        mvc.perform(post("/api/v1/blogs/" + draftId + "/vote").with(csrf()))
                .andExpect(status().isNotFound());

        long blogId = publishedBlog();
        mvc.perform(post("/api/v1/blogs/" + blogId + "/vote"))
                .andExpect(status().isForbidden());
    }

    private long publishedBlog() throws Exception {
        AppUserDetails author = createUser("writer" + System.nanoTime() % 100000);
        return idOf(createBlog(author, "Open vote " + System.nanoTime(), true));
    }

    private Cookie firstVote(long blogId) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/blogs/" + blogId + "/vote").with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        String value = result.getResponse().getHeader("Set-Cookie").split(";")[0].split("=", 2)[1];
        return new Cookie("FH_VISITOR", value);
    }
}
