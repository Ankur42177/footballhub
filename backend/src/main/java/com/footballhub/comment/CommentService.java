package com.footballhub.comment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogRepository;
import com.footballhub.blog.BlogService;
import com.footballhub.comment.dto.CommentDtos.CommentResponse;
import com.footballhub.comment.dto.CommentDtos.CreateCommentRequest;
import com.footballhub.comment.dto.CommentDtos.UpdateCommentRequest;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.common.exception.ApiException;
import com.footballhub.common.util.Pagination;
import com.footballhub.moderation.AuditService;
import com.footballhub.notification.NotificationService;
import com.footballhub.notification.NotificationType;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.User;
import com.footballhub.user.dto.UserDtos.AuthorSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comments and replies (section 9.8). */
@Service
@Transactional
public class CommentService {

    private static final Duration EDIT_GRACE = Duration.ofSeconds(5);

    private final CommentRepository comments;
    private final BlogRepository blogs;
    private final BlogService blogService;
    private final CurrentUser currentUser;
    private final NotificationService notifications;
    private final AuditService auditService;

    public CommentService(CommentRepository comments, BlogRepository blogs, BlogService blogService,
            CurrentUser currentUser, NotificationService notifications, AuditService auditService) {
        this.comments = comments;
        this.blogs = blogs;
        this.blogService = blogService;
        this.currentUser = currentUser;
        this.notifications = notifications;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(Long blogId, String sort, Integer page, Integer size,
            AppUserDetails viewer) {
        Blog blog = blogs.findById(blogId).orElseThrow(() -> ApiException.notFound("Article"));
        boolean privileged = viewer != null
                && (viewer.getId().equals(blog.getAuthor().getId()) || viewer.getRole().isStaff());
        if (!blog.isPubliclyVisible() && !privileged) {
            throw ApiException.notFound("Article");
        }
        Sort order = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<Comment> roots = comments.findThreadRoots(blogId, CommentStatus.VISIBLE,
                Pagination.of(page, size, 20, order));

        Map<Long, List<CommentResponse>> repliesByRoot = new LinkedHashMap<>();
        List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        if (!rootIds.isEmpty()) {
            for (Comment reply : comments.findRepliesFor(rootIds, CommentStatus.VISIBLE)) {
                repliesByRoot.computeIfAbsent(reply.getParentId(), id -> new ArrayList<>())
                        .add(toResponse(reply, viewer, List.of()));
            }
        }
        List<CommentResponse> content = roots.getContent().stream()
                .map(root -> toResponse(root, viewer, repliesByRoot.getOrDefault(root.getId(), List.of())))
                .toList();
        return PageResponse.of(roots, content);
    }

    public CommentResponse create(Long blogId, CreateCommentRequest request, AppUserDetails principal) {
        User author = currentUser.require(principal);
        Blog blog = blogService.requirePubliclyVisible(blogId);
        Long parentId = null;
        Comment parent = null;
        if (request.parentId() != null) {
            parent = comments.findById(request.parentId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_PARENT", "That comment does not exist."));
            if (!parent.getBlog().getId().equals(blog.getId())) {
                throw ApiException.badRequest("INVALID_PARENT", "Replies must belong to the same article.");
            }
            if (!parent.isVisible()) {
                throw ApiException.badRequest("INVALID_PARENT", "That comment is no longer available.");
            }
            // Keep threads one level deep: replies to replies attach to the thread root.
            parentId = parent.getParentId() != null ? parent.getParentId() : parent.getId();
        }
        Comment comment = comments.saveAndFlush(new Comment(blog, author, parentId, request.content().trim()));

        String link = "/blogs/" + blog.getSlug() + "#comment-" + comment.getId();
        if (parent != null) {
            notifications.notify(parent.getAuthor(), author, NotificationType.REPLY_TO_COMMENT,
                    author.getDisplayName() + " replied to your comment on \"" + blog.getTitle() + "\"", link);
        }
        if (parent == null || !parent.getAuthor().getId().equals(blog.getAuthor().getId())) {
            notifications.notify(blog.getAuthor(), author, NotificationType.COMMENT_ON_BLOG,
                    author.getDisplayName() + " commented on \"" + blog.getTitle() + "\"", link);
        }
        return toResponse(comment, principal, List.of());
    }

    public CommentResponse update(Long commentId, UpdateCommentRequest request, AppUserDetails principal) {
        User actor = currentUser.require(principal);
        Comment comment = comments.findById(commentId).orElseThrow(() -> ApiException.notFound("Comment"));
        if (!comment.getAuthor().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only edit your own comments.");
        }
        if (!comment.isVisible()) {
            throw ApiException.conflict("COMMENT_UNAVAILABLE", "This comment can no longer be edited.");
        }
        comment.setContent(request.content().trim());
        comments.saveAndFlush(comment);
        return toResponse(comment, principal, List.of());
    }

    /** Soft delete so replies keep their context. Authors and moderators may remove comments. */
    public void delete(Long commentId, AppUserDetails principal) {
        User actor = currentUser.require(principal);
        Comment comment = comments.findById(commentId).orElseThrow(() -> ApiException.notFound("Comment"));
        boolean isAuthor = comment.getAuthor().getId().equals(actor.getId());
        if (!isAuthor && !actor.getRole().isStaff()) {
            throw ApiException.forbidden("You can only delete your own comments.");
        }
        if (comment.getStatus() == CommentStatus.REMOVED) {
            return;
        }
        comment.setStatus(CommentStatus.REMOVED);
        if (!isAuthor) {
            auditService.record(actor, "COMMENT_REMOVED", "COMMENT", comment.getId(), null);
        }
    }

    private CommentResponse toResponse(Comment comment, AppUserDetails viewer, List<CommentResponse> replies) {
        boolean visible = comment.isVisible();
        boolean isAuthor = viewer != null && viewer.getId().equals(comment.getAuthor().getId());
        boolean isStaff = viewer != null && viewer.getRole().isStaff();
        boolean edited = comment.getUpdatedAt() != null && comment.getCreatedAt() != null
                && comment.getUpdatedAt().isAfter(comment.getCreatedAt().plus(EDIT_GRACE));
        return new CommentResponse(comment.getId(), comment.getBlog().getId(), comment.getParentId(),
                visible ? AuthorSummary.from(comment.getAuthor()) : null,
                visible ? comment.getContent() : null,
                comment.getStatus(), visible && edited, comment.getCreatedAt(), comment.getUpdatedAt(),
                visible && isAuthor, visible && (isAuthor || isStaff), replies);
    }
}
