package com.footballhub.moderation.dto;

import java.time.Instant;

import com.footballhub.comment.CommentStatus;
import com.footballhub.moderation.ReportStatus;
import com.footballhub.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ModerationDtos {

    private ModerationDtos() {
    }

    /** Exactly one of blogId or commentId must be set. */
    public record CreateReportRequest(Long blogId, Long commentId, @NotBlank @Size(max = 500) String reason) {
    }

    public record ReportCreatedResponse(Long id, ReportStatus status) {
    }

    public record ResolveReportRequest(@NotNull ReportStatus status, @Size(max = 500) String note,
            boolean hideContent) {
    }

    public record ReportTarget(String type, Long blogId, String blogSlug, String blogTitle, boolean blogHidden,
            Long commentId, String commentExcerpt, CommentStatus commentStatus, String contentAuthor) {
    }

    public record ReportResponse(Long id, ReportStatus status, String reason, String reporter, Instant createdAt,
            ReportTarget target, String resolvedBy, Instant resolvedAt, String resolutionNote) {
    }

    public record ChangeRoleRequest(@NotNull Role role) {
    }

    public record ChangeStatusRequest(@NotNull Boolean enabled) {
    }

    public record AdminUserResponse(Long id, String username, String email, String displayName, Role role,
            boolean enabled, boolean emailVerified, Instant createdAt) {
    }

    public record AuditLogResponse(Long id, String actor, String action, String targetType, Long targetId,
            String details, Instant createdAt) {
    }

    public record ModerationResult(Long id, String type, boolean hidden) {
    }
}
