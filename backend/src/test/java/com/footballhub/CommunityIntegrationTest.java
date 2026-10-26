package com.footballhub;

import com.footballhub.security.AppUserDetails;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityIntegrationTest extends IntegrationTestSupport {

    @Test
    void sameUserVotingTwiceCountsOnce() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        long id = idOf(createBlog(author, "Vote for me", true));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/blogs/" + id + "/vote").with(user(fan)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteCount").value(1))
                    .andExpect(jsonPath("$.voted").value(true));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM blog_votes", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/v1/blogs/vote-for-me").with(user(fan)))
                .andExpect(jsonPath("$.voteCount").value(1))
                .andExpect(jsonPath("$.votedByMe").value(true));
    }

    @Test
    void removingAVoteOnlyDecrementsOnce() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        long id = idOf(createBlog(author, "Changed my mind", true));
        mvc.perform(post("/api/v1/blogs/" + id + "/vote").with(user(fan)).with(csrf())).andExpect(status().isOk());

        for (int i = 0; i < 2; i++) {
            mvc.perform(delete("/api/v1/blogs/" + id + "/vote").with(user(fan)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteCount").value(0))
                    .andExpect(jsonPath("$.voted").value(false));
        }
    }

    @Test
    void cannotVoteOnADraft() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        long id = idOf(createBlog(author, "Not yet", false));
        mvc.perform(post("/api/v1/blogs/" + id + "/vote").with(user(fan)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentsAndRepliesFormOneLevelThreadsAndNotifyAuthors() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        AppUserDetails rival = createUser("rival");
        long blogId = idOf(createBlog(author, "Talking point", true));

        long rootId = idOf(comment(fan, blogId, "Great read", null));
        long replyId = idOf(comment(rival, blogId, "Disagree", rootId));
        String nested = comment(author, blogId, "Thanks both", replyId);
        assertThat(((Number) com.jayway.jsonpath.JsonPath.read(nested, "$.parentId")).longValue())
                .isEqualTo(rootId);

        mvc.perform(get("/api/v1/blogs/" + blogId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Great read"))
                .andExpect(jsonPath("$.content[0].replies.length()").value(2));

        // fan: rival replied to them. author: two comments from others (their own reply is not notified).
        assertThat(notificationsFor("fan")).isEqualTo(1);
        assertThat(notificationsFor("author")).isEqualTo(2);
        assertThat(notificationsFor("rival")).isEqualTo(1);
    }

    private Integer notificationsFor(String username) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications n JOIN users u ON u.id = n.user_id WHERE u.username = ?"
                        + " AND n.type IN ('COMMENT_ON_BLOG', 'REPLY_TO_COMMENT')",
                Integer.class, username);
    }

    @Test
    void replyReferencingACommentFromAnotherBlogIsRejected() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        long firstBlog = idOf(createBlog(author, "First", true));
        long secondBlog = idOf(createBlog(author, "Second", true));
        long foreignComment = idOf(comment(fan, firstBlog, "On the first blog", null));

        mvc.perform(post("/api/v1/blogs/" + secondBlog + "/comments").with(user(fan)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content": "Sneaky reply", "parentId": %d}
                        """.formatted(foreignComment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARENT"));
    }

    @Test
    void onlyTheAuthorCanEditAndDeletedCommentsBecomePlaceholders() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        AppUserDetails other = createUser("other");
        long blogId = idOf(createBlog(author, "Edit rules", true));
        long rootId = idOf(comment(fan, blogId, "Original", null));
        comment(other, blogId, "A reply", rootId);

        mvc.perform(put("/api/v1/comments/" + rootId).with(user(other)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"content": "Edited by someone else"}
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/comments/" + rootId).with(user(fan)).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/blogs/" + blogId + "/comments"))
                .andExpect(jsonPath("$.content[0].status").value("REMOVED"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist())
                .andExpect(jsonPath("$.content[0].replies.length()").value(1));
    }

    @Test
    void duplicateBookmarkIsStoredOnce() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails fan = createUser("fan");
        long id = idOf(createBlog(author, "Save this", true));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/blogs/" + id + "/bookmark").with(user(fan)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bookmarks", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/v1/users/me/bookmarks").with(user(fan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].bookmarkedByMe").value(true));
    }

    @Test
    void followingAClubIsIdempotent() throws Exception {
        AppUserDetails fan = createUser("fan");
        long clubId = clubId("arsenal");
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/clubs/" + clubId + "/follow").with(user(fan)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.following").value(true))
                    .andExpect(jsonPath("$.followerCount").value(1));
        }
        mvc.perform(get("/api/v1/users/me/clubs").with(user(fan)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("arsenal"));
        mvc.perform(delete("/api/v1/clubs/" + clubId + "/follow").with(user(fan)).with(csrf()))
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void clubsArePublicAndSeeded() throws Exception {
        mvc.perform(get("/api/v1/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
        mvc.perform(get("/api/v1/clubs/liverpool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Liverpool"));
    }

    private String comment(AppUserDetails author, long blogId, String text, Long parentId) throws Exception {
        String parent = parentId == null ? "null" : parentId.toString();
        MvcResult result = mvc.perform(post("/api/v1/blogs/" + blogId + "/comments").with(user(author))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content": "%s", "parentId": %s}
                        """.formatted(text, parent)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }
}
