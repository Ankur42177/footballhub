package com.footballhub.blog;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Batch lookups that decorate a page of blogs without N+1 queries. */
@Repository
public class BlogStatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public BlogStatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, Long> visibleCommentCounts(Collection<Long> blogIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (blogIds.isEmpty()) {
            return counts;
        }
        jdbc.query("""
                SELECT blog_id, COUNT(*) AS n FROM comments
                WHERE blog_id IN (:ids) AND status = 'VISIBLE'
                GROUP BY blog_id
                """, new MapSqlParameterSource("ids", blogIds), rs -> {
            counts.put(rs.getLong("blog_id"), rs.getLong("n"));
        });
        return counts;
    }

    public Set<Long> votedBy(Long userId, Collection<Long> blogIds) {
        return idsFor("SELECT blog_id FROM blog_votes WHERE user_id = :userId AND blog_id IN (:ids)", userId,
                blogIds);
    }

    public Set<Long> bookmarkedBy(Long userId, Collection<Long> blogIds) {
        return idsFor("SELECT blog_id FROM bookmarks WHERE user_id = :userId AND blog_id IN (:ids)", userId,
                blogIds);
    }

    private Set<Long> idsFor(String sql, Long userId, Collection<Long> blogIds) {
        if (userId == null || blogIds.isEmpty()) {
            return Set.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId)
                .addValue("ids", blogIds);
        return new HashSet<>(jdbc.queryForList(sql, params, Long.class));
    }
}
