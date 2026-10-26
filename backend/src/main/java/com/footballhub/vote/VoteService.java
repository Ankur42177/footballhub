package com.footballhub.vote;

import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogService;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.User;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upvotes (section 9.7). The (user_id, blog_id) primary key makes a duplicate vote a
 * no-op, and the cached vote_count only moves when a row was actually inserted or
 * deleted, in the same transaction, so concurrent clicks cannot drift the count.
 *
 * Visitors who aren't signed in can upvote too: they're identified by a random visitor ID
 * (see VisitorCookies) and anonymous_votes enforces one vote per visitor per article in the
 * same way. Both kinds of vote count towards vote_count.
 */
@Service
@Transactional
public class VoteService {

    public record VoteResponse(Long blogId, int voteCount, boolean voted) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final BlogService blogService;
    private final CurrentUser currentUser;

    public VoteService(NamedParameterJdbcTemplate jdbc, BlogService blogService, CurrentUser currentUser) {
        this.jdbc = jdbc;
        this.blogService = blogService;
        this.currentUser = currentUser;
    }

    public VoteResponse vote(Long blogId, AppUserDetails principal) {
        User user = currentUser.require(principal);
        Blog blog = blogService.requirePubliclyVisible(blogId);
        MapSqlParameterSource params = params(user.getId(), blog.getId());
        int inserted = jdbc.update("""
                INSERT INTO blog_votes (user_id, blog_id) VALUES (:userId, :blogId)
                ON CONFLICT (user_id, blog_id) DO NOTHING
                """, params);
        if (inserted == 1) {
            jdbc.update("UPDATE blogs SET vote_count = vote_count + 1 WHERE id = :blogId", params);
        }
        return new VoteResponse(blog.getId(), currentCount(params), true);
    }

    public VoteResponse unvote(Long blogId, AppUserDetails principal) {
        User user = currentUser.require(principal);
        Blog blog = blogService.requirePubliclyVisible(blogId);
        MapSqlParameterSource params = params(user.getId(), blog.getId());
        int deleted = jdbc.update("DELETE FROM blog_votes WHERE user_id = :userId AND blog_id = :blogId", params);
        if (deleted == 1) {
            jdbc.update("UPDATE blogs SET vote_count = vote_count - 1 WHERE id = :blogId", params);
        }
        return new VoteResponse(blog.getId(), currentCount(params), false);
    }

    public VoteResponse voteAsVisitor(Long blogId, UUID visitorId) {
        Blog blog = blogService.requirePubliclyVisible(blogId);
        MapSqlParameterSource params = visitorParams(visitorId, blog.getId());
        int inserted = jdbc.update("""
                INSERT INTO anonymous_votes (visitor_id, blog_id) VALUES (:visitorId, :blogId)
                ON CONFLICT (visitor_id, blog_id) DO NOTHING
                """, params);
        if (inserted == 1) {
            jdbc.update("UPDATE blogs SET vote_count = vote_count + 1 WHERE id = :blogId", params);
        }
        return new VoteResponse(blog.getId(), currentCount(params), true);
    }

    public VoteResponse unvoteAsVisitor(Long blogId, Optional<UUID> visitorId) {
        Blog blog = blogService.requirePubliclyVisible(blogId);
        if (visitorId.isEmpty()) {
            return new VoteResponse(blog.getId(), currentCount(visitorParams(null, blog.getId())), false);
        }
        MapSqlParameterSource params = visitorParams(visitorId.get(), blog.getId());
        int deleted = jdbc.update(
                "DELETE FROM anonymous_votes WHERE visitor_id = :visitorId AND blog_id = :blogId", params);
        if (deleted == 1) {
            jdbc.update("UPDATE blogs SET vote_count = vote_count - 1 WHERE id = :blogId", params);
        }
        return new VoteResponse(blog.getId(), currentCount(params), false);
    }

    /** The live count and whether this viewer (signed in or not) has upvoted. */
    @Transactional(readOnly = true)
    public VoteResponse status(Long blogId, AppUserDetails principal, Optional<UUID> visitorId) {
        Blog blog = blogService.requirePubliclyVisible(blogId);
        boolean voted;
        if (principal != null) {
            voted = exists("SELECT EXISTS (SELECT 1 FROM blog_votes WHERE user_id = :userId AND blog_id = :blogId)",
                    params(principal.getId(), blog.getId()));
        }
        else {
            voted = visitorId.isPresent() && exists(
                    "SELECT EXISTS (SELECT 1 FROM anonymous_votes WHERE visitor_id = :visitorId AND blog_id = :blogId)",
                    visitorParams(visitorId.get(), blog.getId()));
        }
        return new VoteResponse(blog.getId(), currentCount(params(null, blog.getId())), voted);
    }

    private boolean exists(String sql, MapSqlParameterSource params) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, params, Boolean.class));
    }

    private static MapSqlParameterSource visitorParams(UUID visitorId, Long blogId) {
        return new MapSqlParameterSource().addValue("visitorId", visitorId).addValue("blogId", blogId);
    }

    private int currentCount(MapSqlParameterSource params) {
        Integer count = jdbc.queryForObject("SELECT vote_count FROM blogs WHERE id = :blogId", params,
                Integer.class);
        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource params(Long userId, Long blogId) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("blogId", blogId);
    }
}
