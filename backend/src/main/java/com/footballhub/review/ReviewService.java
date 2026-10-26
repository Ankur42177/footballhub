package com.footballhub.review;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.footballhub.auth.TokenService;
import com.footballhub.auth.mail.MailService;
import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogRepository;
import com.footballhub.blog.BlogStatus;
import com.footballhub.user.dto.UserDtos.AuthorSummary;
import com.footballhub.club.dto.ClubDtos.ClubSummary;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.common.exception.ApiException;
import com.footballhub.common.util.Pagination;
import com.footballhub.config.AppProperties;
import com.footballhub.moderation.AuditService;
import com.footballhub.notification.NotificationService;
import com.footballhub.notification.NotificationType;
import com.footballhub.user.Role;
import com.footballhub.user.User;
import com.footballhub.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Editorial review. Only administrators publish directly; everyone else's stories wait here.
 *
 * <p>Each submission gets a fresh 256-bit random token. Only its SHA-256 hash is stored, with
 * an expiry, so a database leak can't be turned into approvals. The raw token goes to the
 * reviewer's inbox once, inside a link whose token sits in the URL fragment (never sent to
 * servers or written to request logs). Using the link, or deciding from the admin page,
 * clears the token, so every link works at most once. Invalid, used and expired links all
 * get the same answer.
 */
@Service
@Transactional
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int EMAIL_CONTENT_LIMIT = 8000;

    public record ReviewPreview(Long id, String title, String excerpt, String content, AuthorSummary author,
            ClubSummary club, Instant submittedAt, Instant linkExpiresAt) {
    }

    public record PendingReview(Long id, String title, String slug, String excerpt, AuthorSummary author,
            ClubSummary club, Instant submittedAt) {
    }

    public record ReviewDecision(Long id, String slug, BlogStatus status) {
    }

    private final BlogRepository blogs;
    private final UserRepository users;
    private final MailService mail;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AppProperties properties;

    public ReviewService(BlogRepository blogs, UserRepository users, MailService mail,
            NotificationService notifications, AuditService audit, AppProperties properties) {
        this.blogs = blogs;
        this.users = users;
        this.mail = mail;
        this.notifications = notifications;
        this.audit = audit;
        this.properties = properties;
    }

    /** Puts a non-admin author's story in the review queue and emails the reviewer. */
    public void submit(Blog blog) {
        User author = blog.getAuthor();
        if (blog.getStatus() != BlogStatus.PENDING_REVIEW) {
            long waiting = blogs.countByAuthorIdAndStatus(author.getId(), BlogStatus.PENDING_REVIEW);
            int limit = properties.review().maxPending();
            if (waiting >= limit) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_PENDING",
                        "You already have " + limit + " stories waiting for review. Please wait for a decision.");
            }
        }
        String token = newToken();
        blog.submitForReview(TokenService.hash(token),
                Instant.now().plus(Duration.ofDays(properties.review().linkValidDays())));
        blogs.saveAndFlush(blog);
        String to = properties.review().email();
        if (to.isEmpty()) {
            log.info("Story {} is waiting for review; no review email is configured", blog.getId());
            return;
        }
        String subject = "Review request: " + blog.getTitle();
        String body = reviewEmail(blog, token);
        afterCommit(() -> mail.send(to, subject, body));
    }

    @Transactional(readOnly = true)
    public ReviewPreview preview(String token) {
        Blog blog = byToken(token);
        return new ReviewPreview(blog.getId(), blog.getTitle(), blog.getExcerpt(), blog.getContent(),
                AuthorSummary.from(blog.getAuthor()), ClubSummary.from(blog.getClub()), blog.getSubmittedAt(),
                blog.getReviewTokenExpiresAt());
    }

    public ReviewDecision decideByLink(String token, boolean approve, String note) {
        Blog blog = byToken(token);
        User editor = properties.adminEmails().stream().map(users::findByEmail)
                .flatMap(java.util.Optional::stream).filter(user -> user.getRole() == Role.ADMIN)
                .findFirst().orElse(null);
        return decide(blog, approve, note, editor, "email link");
    }

    public ReviewDecision decideAsAdmin(Long blogId, boolean approve, String note, User admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only administrators can review stories.");
        }
        Blog blog = blogs.findById(blogId).orElseThrow(() -> ApiException.notFound("Article"));
        if (blog.getStatus() != BlogStatus.PENDING_REVIEW) {
            throw ApiException.conflict("NOT_PENDING", "This story isn't waiting for review.");
        }
        return decide(blog, approve, note, admin, "admin page");
    }

    @Transactional(readOnly = true)
    public PageResponse<PendingReview> pending(Integer page, Integer size) {
        Page<Blog> result = blogs.findByStatus(BlogStatus.PENDING_REVIEW,
                Pagination.of(page, size, 20, Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id"))));
        return PageResponse.of(result, result.getContent().stream()
                .map(blog -> new PendingReview(blog.getId(), blog.getTitle(), blog.getSlug(), blog.getExcerpt(),
                        AuthorSummary.from(blog.getAuthor()), ClubSummary.from(blog.getClub()), blog.getSubmittedAt()))
                .toList());
    }

    private ReviewDecision decide(Blog blog, boolean approve, String note, User editor, String via) {
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (approve) {
            blog.approve();
            notifications.notify(blog.getAuthor(), editor, NotificationType.MODERATION,
                    "Your story \"" + blog.getTitle() + "\" was approved and is now live.", "/blogs/" + blog.getSlug());
        }
        else {
            blog.reject(cleanNote);
            notifications.notify(blog.getAuthor(), editor, NotificationType.MODERATION,
                    "Your story \"" + blog.getTitle() + "\" wasn't approved"
                            + (cleanNote == null ? "." : ": " + cleanNote),
                    "/blogs/" + blog.getSlug());
        }
        blogs.saveAndFlush(blog);
        if (editor != null) {
            audit.record(editor, approve ? "BLOG_APPROVED" : "BLOG_REJECTED", "BLOG", blog.getId(), "via " + via);
        }
        return new ReviewDecision(blog.getId(), blog.getSlug(), blog.getStatus());
    }

    private Blog byToken(String token) {
        ApiException invalid = ApiException.badRequest("INVALID_REVIEW_LINK",
                "This review link is invalid, has already been used or has expired.");
        if (token == null || token.isBlank() || token.length() > 200) {
            throw invalid;
        }
        Blog blog = blogs.findByReviewTokenHash(TokenService.hash(token.trim())).orElseThrow(() -> invalid);
        if (blog.getStatus() != BlogStatus.PENDING_REVIEW || blog.getReviewTokenExpiresAt() == null
                || blog.getReviewTokenExpiresAt().isBefore(Instant.now())) {
            throw invalid;
        }
        return blog;
    }

    private String reviewEmail(Blog blog, String token) {
        String site = properties.frontendUrl();
        String content = blog.getContent();
        if (content.length() > EMAIL_CONTENT_LIMIT) {
            content = content.substring(0, EMAIL_CONTENT_LIMIT) + "\n\n[...] (open the review page to read the rest)";
        }
        return """
                A story is waiting for your review on FootballHub.

                Title:   %s
                Author:  %s (@%s)
                Club:    %s

                Summary:
                %s

                ---------------------------------------------------------------
                %s
                ---------------------------------------------------------------

                Approve or reject it here. The link works once and expires in %d days:
                %s/review#token=%s

                You can also review it on the admin page: %s/admin?tab=reviews
                If you weren't expecting this email, you can ignore it.
                """.formatted(blog.getTitle(), blog.getAuthor().getDisplayName(), blog.getAuthor().getUsername(),
                blog.getClub() == null ? "None" : blog.getClub().getName(),
                blog.getExcerpt() == null ? "(none)" : blog.getExcerpt(), content,
                properties.review().linkValidDays(), site, token, site);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        }
        else {
            action.run();
        }
    }
}
