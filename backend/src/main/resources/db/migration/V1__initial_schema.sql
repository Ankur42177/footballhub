-- Initial schema, as specified in section 6.4 of the implementation plan.

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'MODERATOR', 'ADMIN'))
);

CREATE TABLE clubs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    league VARCHAR(80) NOT NULL DEFAULT 'Premier League',
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE blogs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users(id),
    club_id BIGINT REFERENCES clubs(id),
    title VARCHAR(180) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    excerpt VARCHAR(400),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    vote_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    CONSTRAINT blogs_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT blogs_vote_count_check CHECK (vote_count >= 0)
);

CREATE TABLE comments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    blog_id BIGINT NOT NULL REFERENCES blogs(id),
    author_id BIGINT NOT NULL REFERENCES users(id),
    parent_id BIGINT REFERENCES comments(id),
    content VARCHAR(3000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT comments_status_check
        CHECK (status IN ('VISIBLE', 'HIDDEN', 'REMOVED')),
    CONSTRAINT comments_not_self_parent
        CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE TABLE blog_votes (
    user_id BIGINT NOT NULL REFERENCES users(id),
    blog_id BIGINT NOT NULL REFERENCES blogs(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, blog_id)
);

CREATE TABLE bookmarks (
    user_id BIGINT NOT NULL REFERENCES users(id),
    blog_id BIGINT NOT NULL REFERENCES blogs(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, blog_id)
);

CREATE TABLE user_clubs (
    user_id BIGINT NOT NULL REFERENCES users(id),
    club_id BIGINT NOT NULL REFERENCES clubs(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, club_id)
);

CREATE TABLE content_reports (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    blog_id BIGINT REFERENCES blogs(id),
    comment_id BIGINT REFERENCES comments(id),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT reports_target_check CHECK (
        (blog_id IS NOT NULL AND comment_id IS NULL)
        OR (blog_id IS NULL AND comment_id IS NOT NULL)
    ),
    CONSTRAINT reports_status_check
        CHECK (status IN ('OPEN', 'REVIEWING', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_blogs_published
    ON blogs (published_at DESC, id DESC)
    WHERE status = 'PUBLISHED';

CREATE INDEX idx_blogs_club
    ON blogs (club_id, published_at DESC)
    WHERE status = 'PUBLISHED';

CREATE INDEX idx_blogs_author
    ON blogs (author_id, created_at DESC);

CREATE INDEX idx_comments_blog
    ON comments (blog_id, created_at ASC);

CREATE INDEX idx_comments_parent
    ON comments (parent_id);

CREATE INDEX idx_votes_blog ON blog_votes (blog_id);
CREATE INDEX idx_reports_status
    ON content_reports (status, created_at DESC);
