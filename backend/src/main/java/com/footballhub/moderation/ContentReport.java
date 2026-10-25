package com.footballhub.moderation;

import java.time.Instant;

import com.footballhub.blog.Blog;
import com.footballhub.comment.Comment;
import com.footballhub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A user report against exactly one blog or one comment (enforced by a CHECK constraint). */
@Entity
@Table(name = "content_reports")
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id")
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ContentReport() {
    }

    public static ContentReport forBlog(User reporter, Blog blog, String reason) {
        ContentReport report = new ContentReport();
        report.reporter = reporter;
        report.blog = blog;
        report.reason = reason;
        return report;
    }

    public static ContentReport forComment(User reporter, Comment comment, String reason) {
        ContentReport report = new ContentReport();
        report.reporter = reporter;
        report.comment = comment;
        report.reason = reason;
        return report;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void resolve(ReportStatus outcome, User moderator, String note) {
        this.status = outcome;
        if (outcome.isOpen()) {
            this.resolvedBy = null;
            this.resolvedAt = null;
        }
        else {
            this.resolvedBy = moderator;
            this.resolvedAt = Instant.now();
        }
        this.resolutionNote = note;
    }

    public Long getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public Blog getBlog() {
        return blog;
    }

    public Comment getComment() {
        return comment;
    }

    public String getReason() {
        return reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
