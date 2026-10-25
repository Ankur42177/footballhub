package com.footballhub.blog;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.footballhub.blog.dto.BlogDtos.BlogDetailResponse;
import com.footballhub.blog.dto.BlogDtos.BlogSummaryResponse;
import com.footballhub.club.dto.ClubDtos.ClubSummary;
import com.footballhub.user.Role;
import com.footballhub.user.dto.UserDtos.AuthorSummary;

import org.springframework.stereotype.Component;

/** Entity-to-response conversion. JPA entities never leave the service layer. */
@Component
public class BlogMapper {

    private static final int WORDS_PER_MINUTE = 220;

    private final BlogStatsRepository stats;

    public BlogMapper(BlogStatsRepository stats) {
        this.stats = stats;
    }

    public List<BlogSummaryResponse> toSummaries(List<Blog> blogs, Long viewerId) {
        List<Long> ids = blogs.stream().map(Blog::getId).toList();
        Map<Long, Long> comments = stats.visibleCommentCounts(ids);
        Set<Long> voted = stats.votedBy(viewerId, ids);
        Set<Long> bookmarked = stats.bookmarkedBy(viewerId, ids);
        return blogs.stream().map(blog -> new BlogSummaryResponse(blog.getId(), blog.getTitle(), blog.getSlug(),
                blog.getExcerpt(), AuthorSummary.from(blog.getAuthor()), ClubSummary.from(blog.getClub()),
                blog.getStatus(), blog.isHidden(), blog.getVoteCount(), comments.getOrDefault(blog.getId(), 0L),
                readingMinutes(blog.getContent()), blog.getPublishedAt(), blog.getCreatedAt(),
                blog.getUpdatedAt(), voted.contains(blog.getId()), bookmarked.contains(blog.getId()))).toList();
    }

    public BlogDetailResponse toDetail(Blog blog, Long viewerId, Role viewerRole) {
        List<Long> ids = List.of(blog.getId());
        long comments = stats.visibleCommentCounts(ids).getOrDefault(blog.getId(), 0L);
        boolean voted = stats.votedBy(viewerId, ids).contains(blog.getId());
        boolean bookmarked = stats.bookmarkedBy(viewerId, ids).contains(blog.getId());
        boolean canEdit = viewerId != null
                && (viewerId.equals(blog.getAuthor().getId()) || viewerRole == Role.ADMIN);
        return new BlogDetailResponse(blog.getId(), blog.getTitle(), blog.getSlug(), blog.getExcerpt(),
                blog.getContent(), AuthorSummary.from(blog.getAuthor()), ClubSummary.from(blog.getClub()),
                blog.getStatus(), blog.isHidden(), blog.getVoteCount(), comments, readingMinutes(blog.getContent()),
                blog.getPublishedAt(), blog.getCreatedAt(), blog.getUpdatedAt(), voted, bookmarked, canEdit);
    }

    static int readingMinutes(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
    }
}
