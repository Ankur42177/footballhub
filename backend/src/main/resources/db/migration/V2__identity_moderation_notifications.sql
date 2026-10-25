-- Follow-up migration called for in section 6.4: verification/reset tokens, notifications,
-- moderation fields, audit logs, cascading deletes and a cross-blog reply constraint.

-- Users: accounts are active on registration; "enabled = false" now means suspended.
ALTER TABLE users ALTER COLUMN enabled SET DEFAULT TRUE;
ALTER TABLE users ADD COLUMN bio VARCHAR(500);
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Clubs: display metadata for navigation and accents.
ALTER TABLE clubs ADD COLUMN short_name VARCHAR(5);
ALTER TABLE clubs ADD COLUMN primary_color VARCHAR(7);

-- Blogs: moderation flag, independent of the author-controlled status.
ALTER TABLE blogs ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Deleting a blog removes its votes, bookmarks, comments and reports.
ALTER TABLE blog_votes DROP CONSTRAINT blog_votes_blog_id_fkey,
    ADD CONSTRAINT blog_votes_blog_id_fkey FOREIGN KEY (blog_id) REFERENCES blogs(id) ON DELETE CASCADE;
ALTER TABLE bookmarks DROP CONSTRAINT bookmarks_blog_id_fkey,
    ADD CONSTRAINT bookmarks_blog_id_fkey FOREIGN KEY (blog_id) REFERENCES blogs(id) ON DELETE CASCADE;
ALTER TABLE comments DROP CONSTRAINT comments_blog_id_fkey,
    ADD CONSTRAINT comments_blog_id_fkey FOREIGN KEY (blog_id) REFERENCES blogs(id) ON DELETE CASCADE;
ALTER TABLE comments DROP CONSTRAINT comments_parent_id_fkey,
    ADD CONSTRAINT comments_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE;
ALTER TABLE content_reports DROP CONSTRAINT content_reports_blog_id_fkey,
    ADD CONSTRAINT content_reports_blog_id_fkey FOREIGN KEY (blog_id) REFERENCES blogs(id) ON DELETE CASCADE;
ALTER TABLE content_reports DROP CONSTRAINT content_reports_comment_id_fkey,
    ADD CONSTRAINT content_reports_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE;
ALTER TABLE user_clubs DROP CONSTRAINT user_clubs_club_id_fkey,
    ADD CONSTRAINT user_clubs_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE;

-- A reply's parent must belong to the same blog, enforced by the database.
ALTER TABLE comments ADD CONSTRAINT comments_id_blog_unique UNIQUE (id, blog_id);
ALTER TABLE comments ADD CONSTRAINT comments_parent_same_blog_fkey
    FOREIGN KEY (parent_id, blog_id) REFERENCES comments (id, blog_id) ON DELETE CASCADE;

-- Moderation outcome on reports.
ALTER TABLE content_reports ADD COLUMN resolved_by BIGINT REFERENCES users(id);
ALTER TABLE content_reports ADD COLUMN resolved_at TIMESTAMPTZ;
ALTER TABLE content_reports ADD COLUMN resolution_note VARCHAR(500);

-- One open report per user per target.
CREATE UNIQUE INDEX uq_reports_open_blog
    ON content_reports (reporter_id, blog_id)
    WHERE blog_id IS NOT NULL AND status IN ('OPEN', 'REVIEWING');
CREATE UNIQUE INDEX uq_reports_open_comment
    ON content_reports (reporter_id, comment_id)
    WHERE comment_id IS NOT NULL AND status IN ('OPEN', 'REVIEWING');

-- Single-use, expiring tokens for email verification and password reset.
-- Only a SHA-256 hash of each token is stored.
CREATE TABLE user_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_tokens_type_check CHECK (type IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))
);
CREATE INDEX idx_user_tokens_user ON user_tokens (user_id, type);

-- Notifications for replies, comments on your posts, and moderation outcomes.
CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    message VARCHAR(300) NOT NULL,
    link VARCHAR(300),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notifications_type_check
        CHECK (type IN ('COMMENT_ON_BLOG', 'REPLY_TO_COMMENT', 'MODERATION'))
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;

-- Important administrative and moderation actions.
CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT,
    details VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);

CREATE INDEX idx_bookmarks_user ON bookmarks (user_id, created_at DESC);
CREATE INDEX idx_user_clubs_club ON user_clubs (club_id);
