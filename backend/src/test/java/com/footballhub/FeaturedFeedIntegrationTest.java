package com.footballhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.footballhub.security.AppUserDetails;
import com.footballhub.user.Role;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * The homepage's "featured" feed: the featured club's articles (Manchester City by default)
 * come first, then everything else; each group is ordered by upvotes, then newest.
 */
class FeaturedFeedIntegrationTest extends IntegrationTestSupport {

    @Test
    void featuredClubFirstThenEverythingByUpvotes() throws Exception {
        AppUserDetails author = createUser("author", Role.ADMIN);
        publish(author, "Arsenal five", "arsenal", 5);
        publish(author, "City one", "manchester-city", 1);
        publish(author, "General ten", null, 10);
        publish(author, "City three", "manchester-city", 3);

        assertThat(titles("sort=featured&size=10")).containsExactly("City three", "City one", "General ten",
                "Arsenal five");
    }

    @Test
    void featuredOrderingSurvivesPaging() throws Exception {
        AppUserDetails author = createUser("author", Role.ADMIN);
        publish(author, "Arsenal five", "arsenal", 5);
        publish(author, "City one", "manchester-city", 1);
        publish(author, "General ten", null, 10);
        publish(author, "City three", "manchester-city", 3);

        String first = body("sort=featured&size=2&page=0");
        assertThat(JsonPath.<List<String>>read(first, "$.content[*].title")).containsExactly("City three", "City one");
        assertThat(JsonPath.<Integer>read(first, "$.totalElements")).isEqualTo(4);
        assertThat(titles("sort=featured&size=2&page=1")).containsExactly("General ten", "Arsenal five");
    }

    @Test
    void otherSortsAreUnchanged() throws Exception {
        AppUserDetails author = createUser("author", Role.ADMIN);
        publish(author, "City one", "manchester-city", 1);
        publish(author, "General ten", null, 10);
        assertThat(titles("sort=top")).containsExactly("General ten", "City one");
    }

    private void publish(AppUserDetails author, String title, String clubSlug, int votes) throws Exception {
        String club = clubSlug == null ? "null" : String.valueOf(clubId(clubSlug));
        String json = mvc.perform(post("/api/v1/blogs").with(user(author)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "content": "Body text.", "clubId": %s, "publish": true}
                                """.formatted(title, club)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        jdbc.update("UPDATE blogs SET vote_count = ? WHERE id = ?", votes, idOf(json));
    }

    private String body(String query) throws Exception {
        return mvc.perform(get("/api/v1/blogs?" + query)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private List<String> titles(String query) throws Exception {
        return JsonPath.read(body(query), "$.content[*].title");
    }
}
