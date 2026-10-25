package com.footballhub.comment.dto;

import java.time.Instant;
import java.util.List;

import com.footballhub.comment.CommentStatus;
import com.footballhub.user.dto.UserDtos.AuthorSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CommentDtos {

    private CommentDtos() {
    }

    public record CreateCommentRequest(@NotBlank @Size(max = 3000) String content, Long parentId) {
    }

    public record UpdateCommentRequest(@NotBlank @Size(max = 3000) String content) {
    }

    /**
     * A comment with its replies. For removed or hidden comments, {@code author} and
     * {@code content} are null and the client shows a placeholder.
     */
    public record CommentResponse(Long id, Long blogId, Long parentId, AuthorSummary author, String content,
            CommentStatus status, boolean edited, Instant createdAt, Instant updatedAt, boolean canEdit,
            boolean canDelete, List<CommentResponse> replies) {
    }
}
