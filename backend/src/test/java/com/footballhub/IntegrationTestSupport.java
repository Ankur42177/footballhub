package com.footballhub;

import com.footballhub.security.AppUserDetails;
import com.footballhub.user.Role;
import com.footballhub.user.User;
import com.footballhub.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared setup: one PostgreSQL container, a clean database per test, and request helpers. */
@SpringBootTest(properties = { "app.rate-limit.enabled=false", "app.seed-demo-data=false" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "correct-horse-42";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE users, blogs, comments, blog_votes, bookmarks, user_clubs, content_reports,
                         notifications, audit_logs, user_tokens, spring_session, spring_session_attributes
                RESTART IDENTITY CASCADE
                """);
    }

    protected AppUserDetails createUser(String username) {
        return createUser(username, Role.USER);
    }

    protected AppUserDetails createUser(String username, Role role) {
        User user = new User(username, username + "@example.com", passwordEncoder.encode(PASSWORD), username);
        user.setRole(role);
        userRepository.save(user);
        return AppUserDetails.from(user);
    }

    protected long clubId(String slug) {
        Long id = jdbc.queryForObject("SELECT id FROM clubs WHERE slug = ?", Long.class, slug);
        return id == null ? 0 : id;
    }

    /** Creates a blog through the API and returns the JSON response body. */
    protected String createBlog(AppUserDetails author, String title, boolean publish) throws Exception {
        String body = """
                {"title": "%s", "excerpt": "Short summary", "content": "A long enough body about tactics.",
                 "clubId": %d, "publish": %b}
                """.formatted(title, clubId("manchester-city"), publish);
        MvcResult result = mvc.perform(post("/api/v1/blogs").with(user(author)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    protected static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected static String slugOf(String json) {
        return JsonPath.read(json, "$.slug");
    }
}
