-- Editorial review. Stories from anyone except administrators wait for approval before they
-- are published. The reviewer approves by a single-use email link (only its SHA-256 hash is
-- stored, with an expiry) or from the admin page.
ALTER TABLE blogs DROP CONSTRAINT blogs_status_check;
ALTER TABLE blogs ADD CONSTRAINT blogs_status_check
    CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED'));

ALTER TABLE blogs ADD COLUMN submitted_at TIMESTAMPTZ;
ALTER TABLE blogs ADD COLUMN review_token_hash VARCHAR(64);
ALTER TABLE blogs ADD COLUMN review_token_expires_at TIMESTAMPTZ;
ALTER TABLE blogs ADD COLUMN review_note VARCHAR(500);

CREATE UNIQUE INDEX idx_blogs_review_token ON blogs (review_token_hash) WHERE review_token_hash IS NOT NULL;
CREATE INDEX idx_blogs_pending_review ON blogs (submitted_at) WHERE status = 'PENDING_REVIEW';
