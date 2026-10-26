-- Upvotes from visitors who aren't signed in. visitor_id is a random UUID kept in an
-- HTTP-only cookie (FH_VISITOR); the primary key allows one vote per visitor per article.
-- blogs.vote_count counts signed-in and anonymous votes together.
CREATE TABLE anonymous_votes (
    visitor_id UUID NOT NULL,
    blog_id BIGINT NOT NULL REFERENCES blogs(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (visitor_id, blog_id)
);

CREATE INDEX idx_anonymous_votes_blog ON anonymous_votes (blog_id);
