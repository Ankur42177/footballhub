package com.footballhub;

import com.footballhub.security.AppUserDetails;
import com.footballhub.user.Role;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModerationIntegrationTest extends IntegrationTestSupport {

    @Test
    void regularUsersCannotReachTheModerationQueue() throws Exception {
        AppUserDetails fan = createUser("fan");
        mvc.perform(get("/api/v1/admin/reports").with(user(fan))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/reports")).andExpect(status().isUnauthorized());
    }

    @Test
    void moderatorResolvesReportHidesContentAndActionIsAudited() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails reporter = createUser("reporter");
        AppUserDetails moderator = createUser("mod", Role.MODERATOR);
        String blog = createBlog(author, "Off-topic rant", true);
        long blogId = idOf(blog);

        String report = mvc.perform(post("/api/v1/reports").with(user(reporter)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"blogId": %d, "reason": "Not about football"}
                        """.formatted(blogId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long reportId = idOf(report);

        mvc.perform(get("/api/v1/admin/reports").with(user(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].target.type").value("BLOG"))
                .andExpect(jsonPath("$.content[0].reporter").value("reporter"));

        mvc.perform(post("/api/v1/admin/reports/" + reportId + "/resolve").with(user(moderator)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "RESOLVED", "note": "Hidden as off-topic", "hideContent": true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value("mod"))
                .andExpect(jsonPath("$.target.blogHidden").value(true));

        mvc.perform(get("/api/v1/blogs/" + slugOf(blog))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/blogs/" + slugOf(blog)).with(user(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(true));

        Integer audits = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action IN ('REPORT_RESOLVED', 'BLOG_HIDDEN')",
                Integer.class);
        assertThat(audits).isEqualTo(2);
    }

    @Test
    void duplicateOpenReportIsRejected() throws Exception {
        AppUserDetails author = createUser("author");
        AppUserDetails reporter = createUser("reporter");
        long blogId = idOf(createBlog(author, "Contested", true));
        String body = """
                {"blogId": %d, "reason": "Spam"}
                """.formatted(blogId);
        mvc.perform(post("/api/v1/reports").with(user(reporter)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/reports").with(user(reporter)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REPORTED"));
    }

    @Test
    void reportMustTargetExactlyOneItem() throws Exception {
        AppUserDetails reporter = createUser("reporter");
        mvc.perform(post("/api/v1/reports").with(user(reporter)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason": "No target"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TARGET"));
    }

    @Test
    void onlyAdminsCanChangeRoles() throws Exception {
        AppUserDetails moderator = createUser("mod", Role.MODERATOR);
        AppUserDetails admin = createUser("boss", Role.ADMIN);
        AppUserDetails fan = createUser("fan");
        long fanId = fan.getId();

        mvc.perform(patch("/api/v1/admin/users/" + fanId + "/role").with(user(moderator)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"role": "MODERATOR"}
                        """))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/admin/users/" + fanId + "/role").with(user(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"role": "MODERATOR"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));

        mvc.perform(patch("/api/v1/admin/users/" + admin.getId() + "/role").with(user(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"role": "USER"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_CHANGE"));
    }

    @Test
    void suspendedUsersCannotPostEvenWithAnExistingSession() throws Exception {
        AppUserDetails admin = createUser("boss", Role.ADMIN);
        AppUserDetails troll = createUser("troll");
        mvc.perform(patch("/api/v1/admin/users/" + troll.getId() + "/status").with(user(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"enabled": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(post("/api/v1/blogs").with(user(troll)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Still here", "content": "Body"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }
}
