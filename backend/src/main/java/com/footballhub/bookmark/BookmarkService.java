package com.footballhub.bookmark;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogMapper;
import com.footballhub.blog.BlogRepository;
import com.footballhub.blog.BlogService;
import com.footballhub.blog.dto.BlogDtos.BlogSummaryResponse;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.common.util.Pagination;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.User;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookmarkService {

    public record BookmarkResponse(Long blogId, boolean bookmarked) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final BlogService blogService;
    private final BlogRepository blogs;
    private final BlogMapper mapper;
    private final CurrentUser currentUser;

    public BookmarkService(NamedParameterJdbcTemplate jdbc, BlogService blogService, BlogRepository blogs,
            BlogMapper mapper, CurrentUser currentUser) {
        this.jdbc = jdbc;
        this.blogService = blogService;
        this.blogs = blogs;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    public BookmarkResponse add(Long blogId, AppUserDetails principal) {
        User user = currentUser.require(principal);
        Blog blog = blogService.requirePubliclyVisible(blogId);
        jdbc.update("""
                INSERT INTO bookmarks (user_id, blog_id) VALUES (:userId, :blogId)
                ON CONFLICT (user_id, blog_id) DO NOTHING
                """, params(user.getId(), blog.getId()));
        return new BookmarkResponse(blog.getId(), true);
    }

    /** Removing works even if the post has since been hidden or archived. */
    public BookmarkResponse remove(Long blogId, AppUserDetails principal) {
        User user = currentUser.require(principal);
        jdbc.update("DELETE FROM bookmarks WHERE user_id = :userId AND blog_id = :blogId",
                params(user.getId(), blogId));
        return new BookmarkResponse(blogId, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogSummaryResponse> list(AppUserDetails principal, Integer page, Integer size) {
        User user = currentUser.require(principal);
        int pageNumber = Pagination.page(page);
        int pageSize = Pagination.size(size, 12);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", user.getId())
                .addValue("limit", pageSize)
                .addValue("offset", (long) pageNumber * pageSize);

        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM bookmarks bm JOIN blogs b ON b.id = bm.blog_id
                WHERE bm.user_id = :userId AND b.status = 'PUBLISHED' AND b.hidden = FALSE
                """, params, Long.class);
        List<Long> ids = jdbc.queryForList("""
                SELECT bm.blog_id FROM bookmarks bm JOIN blogs b ON b.id = bm.blog_id
                WHERE bm.user_id = :userId AND b.status = 'PUBLISHED' AND b.hidden = FALSE
                ORDER BY bm.created_at DESC, bm.blog_id DESC
                LIMIT :limit OFFSET :offset
                """, params, Long.class);

        Map<Long, Integer> position = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            position.put(ids.get(i), i);
        }
        List<Blog> ordered = blogs.findAllById(ids).stream()
                .sorted(Comparator.comparingInt(blog -> position.get(blog.getId())))
                .toList();
        return PageResponse.of(mapper.toSummaries(ordered, user.getId()), pageNumber, pageSize,
                total == null ? 0 : total);
    }

    private static MapSqlParameterSource params(Long userId, Long blogId) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("blogId", blogId);
    }
}
