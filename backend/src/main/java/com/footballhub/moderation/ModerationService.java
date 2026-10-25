package com.footballhub.moderation;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogRepository;
import com.footballhub.comment.Comment;
import com.footballhub.comment.CommentRepository;
import com.footballhub.comment.CommentStatus;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.common.exception.ApiException;
import com.footballhub.common.util.Pagination;
import com.footballhub.moderation.dto.ModerationDtos.AdminUserResponse;
import com.footballhub.moderation.dto.ModerationDtos.AuditLogResponse;
import com.footballhub.moderation.dto.ModerationDtos.CreateReportRequest;
import com.footballhub.moderation.dto.ModerationDtos.ModerationResult;
import com.footballhub.moderation.dto.ModerationDtos.ReportCreatedResponse;
import com.footballhub.moderation.dto.ModerationDtos.ReportResponse;
import com.footballhub.moderation.dto.ModerationDtos.ReportTarget;
import com.footballhub.moderation.dto.ModerationDtos.ResolveReportRequest;
import com.footballhub.notification.NotificationService;
import com.footballhub.notification.NotificationType;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.Role;
import com.footballhub.user.User;
import com.footballhub.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reports, moderation actions and user administration. Every staff action re-checks the
 * actor's role from the database and is written to the audit log.
 */
@Service
@Transactional
public class ModerationService {

    private static final Set<ReportStatus> OPEN_STATUSES = EnumSet.of(ReportStatus.OPEN, ReportStatus.REVIEWING);

    private final ContentReportRepository reports;
    private final BlogRepository blogs;
    private final CommentRepository comments;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public ModerationService(ContentReportRepository reports, BlogRepository blogs, CommentRepository comments,
            UserRepository users, AuditLogRepository auditLogs, AuditService auditService,
            NotificationService notifications, CurrentUser currentUser) {
        this.reports = reports;
        this.blogs = blogs;
        this.comments = comments;
        this.users = users;
        this.auditLogs = auditLogs;
        this.auditService = auditService;
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    // ---- Reports (any signed-in user) ----

    public ReportCreatedResponse createReport(CreateReportRequest request, AppUserDetails principal) {
        User reporter = currentUser.require(principal);
        boolean hasBlog = request.blogId() != null;
        boolean hasComment = request.commentId() != null;
        if (hasBlog == hasComment) {
            throw ApiException.badRequest("INVALID_TARGET", "Report either an article or a comment.");
        }
        String reason = request.reason().trim();
        ContentReport report;
        if (hasBlog) {
            Blog blog = blogs.findById(request.blogId())
                    .filter(Blog::isPubliclyVisible)
                    .orElseThrow(() -> ApiException.notFound("Article"));
            if (reports.existsByReporterIdAndBlogIdAndStatusIn(reporter.getId(), blog.getId(), OPEN_STATUSES)) {
                throw ApiException.conflict("ALREADY_REPORTED", "You have already reported this article.");
            }
            report = ContentReport.forBlog(reporter, blog, reason);
        }
        else {
            Comment comment = comments.findById(request.commentId())
                    .filter(Comment::isVisible)
                    .orElseThrow(() -> ApiException.notFound("Comment"));
            if (reports.existsByReporterIdAndCommentIdAndStatusIn(reporter.getId(), comment.getId(),
                    OPEN_STATUSES)) {
                throw ApiException.conflict("ALREADY_REPORTED", "You have already reported this comment.");
            }
            report = ContentReport.forComment(reporter, comment, reason);
        }
        reports.saveAndFlush(report);
        return new ReportCreatedResponse(report.getId(), report.getStatus());
    }

    // ---- Moderator queue ----

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listReports(String status, Integer page, Integer size,
            AppUserDetails principal) {
        currentUser.requireStaff(principal);
        Set<ReportStatus> statuses = parseStatusFilter(status);
        Page<ContentReport> result = reports.findByStatusIn(statuses,
                Pagination.of(page, size, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result, result.getContent().stream().map(ModerationService::toResponse).toList());
    }

    public ReportResponse resolveReport(Long reportId, ResolveReportRequest request, AppUserDetails principal) {
        User moderator = currentUser.requireStaff(principal);
        ContentReport report = reports.findById(reportId).orElseThrow(() -> ApiException.notFound("Report"));
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (request.hideContent()) {
            if (report.getBlog() != null) {
                setBlogHidden(report.getBlog(), true, moderator);
            }
            else if (report.getComment() != null) {
                setCommentHidden(report.getComment(), true, moderator);
            }
        }
        report.resolve(request.status(), moderator, note);
        auditService.record(moderator, "REPORT_" + request.status().name(), "REPORT", report.getId(),
                note == null ? null : "Note: " + note);
        return toResponse(report);
    }

    public ModerationResult hideBlog(Long blogId, boolean hidden, AppUserDetails principal) {
        User moderator = currentUser.requireStaff(principal);
        Blog blog = blogs.findById(blogId).orElseThrow(() -> ApiException.notFound("Article"));
        setBlogHidden(blog, hidden, moderator);
        return new ModerationResult(blog.getId(), "BLOG", blog.isHidden());
    }

    public ModerationResult hideComment(Long commentId, boolean hidden, AppUserDetails principal) {
        User moderator = currentUser.requireStaff(principal);
        Comment comment = comments.findById(commentId).orElseThrow(() -> ApiException.notFound("Comment"));
        setCommentHidden(comment, hidden, moderator);
        return new ModerationResult(comment.getId(), "COMMENT", comment.getStatus() == CommentStatus.HIDDEN);
    }

    // ---- Administration (admins only) ----

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(String query, Integer page, Integer size,
            AppUserDetails principal) {
        currentUser.requireAdmin(principal);
        var pageable = Pagination.of(page, size, 25, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<User> result = (query == null || query.isBlank())
                ? users.findAll(pageable)
                : users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query.trim(), query.trim(),
                        pageable);
        return PageResponse.of(result, result.getContent().stream().map(ModerationService::toAdminUser).toList());
    }

    public AdminUserResponse changeRole(Long userId, Role role, AppUserDetails principal) {
        User admin = currentUser.requireAdmin(principal);
        User target = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (target.getId().equals(admin.getId())) {
            throw ApiException.badRequest("SELF_CHANGE", "You cannot change your own role.");
        }
        Role previous = target.getRole();
        target.setRole(role);
        auditService.record(admin, "ROLE_CHANGED", "USER", target.getId(),
                target.getUsername() + ": " + previous + " -> " + role);
        return toAdminUser(target);
    }

    public AdminUserResponse changeStatus(Long userId, boolean enabled, AppUserDetails principal) {
        User admin = currentUser.requireAdmin(principal);
        User target = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (target.getId().equals(admin.getId())) {
            throw ApiException.badRequest("SELF_CHANGE", "You cannot suspend your own account.");
        }
        target.setEnabled(enabled);
        auditService.record(admin, enabled ? "USER_REINSTATED" : "USER_SUSPENDED", "USER", target.getId(),
                target.getUsername());
        return toAdminUser(target);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> auditLog(Integer page, Integer size, AppUserDetails principal) {
        currentUser.requireAdmin(principal);
        Page<AuditLog> result = auditLogs.findAll(
                Pagination.of(page, size, 25, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        List<AuditLogResponse> content = result.getContent().stream()
                .map(log -> new AuditLogResponse(log.getId(), log.getActor().getUsername(), log.getAction(),
                        log.getTargetType(), log.getTargetId(), log.getDetails(), log.getCreatedAt()))
                .toList();
        return PageResponse.of(result, content);
    }

    // ---- Helpers ----

    private void setBlogHidden(Blog blog, boolean hidden, User moderator) {
        if (blog.isHidden() == hidden) {
            return;
        }
        blog.setHidden(hidden);
        auditService.record(moderator, hidden ? "BLOG_HIDDEN" : "BLOG_UNHIDDEN", "BLOG", blog.getId(),
                "Article \"" + blog.getTitle() + "\"");
        if (hidden) {
            notifications.notify(blog.getAuthor(), moderator, NotificationType.MODERATION,
                    "Your article \"" + blog.getTitle() + "\" was hidden by a moderator.", "/dashboard");
        }
    }

    private void setCommentHidden(Comment comment, boolean hidden, User moderator) {
        if (comment.getStatus() == CommentStatus.REMOVED) {
            throw ApiException.conflict("COMMENT_REMOVED", "This comment was already removed.");
        }
        CommentStatus target = hidden ? CommentStatus.HIDDEN : CommentStatus.VISIBLE;
        if (comment.getStatus() == target) {
            return;
        }
        comment.setStatus(target);
        auditService.record(moderator, hidden ? "COMMENT_HIDDEN" : "COMMENT_UNHIDDEN", "COMMENT", comment.getId(),
                null);
        if (hidden) {
            notifications.notify(comment.getAuthor(), moderator, NotificationType.MODERATION,
                    "One of your comments on \"" + comment.getBlog().getTitle() + "\" was hidden by a moderator.",
                    "/blogs/" + comment.getBlog().getSlug());
        }
    }

    private static Set<ReportStatus> parseStatusFilter(String status) {
        if (status == null || status.isBlank() || "open".equalsIgnoreCase(status)) {
            return OPEN_STATUSES;
        }
        if ("all".equalsIgnoreCase(status)) {
            return EnumSet.allOf(ReportStatus.class);
        }
        try {
            return EnumSet.of(ReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("INVALID_STATUS", "Unknown report status.");
        }
    }

    private static ReportResponse toResponse(ContentReport report) {
        ReportTarget target;
        if (report.getBlog() != null) {
            Blog blog = report.getBlog();
            target = new ReportTarget("BLOG", blog.getId(), blog.getSlug(), blog.getTitle(), blog.isHidden(), null,
                    null, null, blog.getAuthor().getUsername());
        }
        else {
            Comment comment = report.getComment();
            Blog blog = comment.getBlog();
            String text = comment.getContent();
            String excerpt = text.length() > 200 ? text.substring(0, 197) + "..." : text;
            target = new ReportTarget("COMMENT", blog.getId(), blog.getSlug(), blog.getTitle(), blog.isHidden(),
                    comment.getId(), excerpt, comment.getStatus(), comment.getAuthor().getUsername());
        }
        return new ReportResponse(report.getId(), report.getStatus(), report.getReason(),
                report.getReporter().getUsername(), report.getCreatedAt(), target,
                report.getResolvedBy() == null ? null : report.getResolvedBy().getUsername(), report.getResolvedAt(),
                report.getResolutionNote());
    }

    private static AdminUserResponse toAdminUser(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.isEnabled(), user.isEmailVerified(), user.getCreatedAt());
    }
}
