package com.footballhub.blog.dto;

import java.time.Instant;

import com.footballhub.blog.BlogStatus;
import com.footballhub.club.dto.ClubDtos.ClubSummary;
import com.footballhub.user.dto.UserDtos.AuthorSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BlogDtos {

    /** Long enough for in-depth analysis, small enough to keep pages fast. */
    public static final int MAX_CONTENT_LENGTH = 50_000;

    private BlogDtos() {
    }

    public record CreateBlogRequest(
            @NotBlank @Size(max = 180) String title,
            @Size(max = 400) String excerpt,
            @NotBlank @Size(max = MAX_CONTENT_LENGTH) String content,
            Long clubId,
            boolean publish) {
    }

    public record UpdateBlogRequest(
            @NotBlank @Size(max = 180) String title,
            @Size(max = 400) String excerpt,
            @NotBlank @Size(max = MAX_CONTENT_LENGTH) String content,
            Long clubId) {
    }

    /** Card-sized representation used in feeds and lists (no body). */
    public record BlogSummaryResponse(Long id, String title, String slug, String excerpt, AuthorSummary author,
            ClubSummary club, BlogStatus status, boolean hidden, int voteCount, long commentCount,
            int readingMinutes, Instant publishedAt, Instant createdAt, Instant updatedAt, boolean votedByMe,
            boolean bookmarkedByMe) {
    }

    /** Full article. {@code canEdit} is a UI hint only; the server enforces ownership. */
    public record BlogDetailResponse(Long id, String title, String slug, String excerpt, String content,
            AuthorSummary author, ClubSummary club, BlogStatus status, boolean hidden, int voteCount,
            long commentCount, int readingMinutes, Instant publishedAt, Instant createdAt, Instant updatedAt,
            boolean votedByMe, boolean bookmarkedByMe, boolean canEdit) {
    }
}
