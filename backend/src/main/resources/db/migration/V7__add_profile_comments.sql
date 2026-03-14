CREATE TABLE profile_comments (
    id          BIGSERIAL    PRIMARY KEY,
    profile_user_id BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_comments_profile ON profile_comments(profile_user_id, created_at DESC);

