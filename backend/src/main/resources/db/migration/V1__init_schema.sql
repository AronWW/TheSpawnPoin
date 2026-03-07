-- ============================================================
-- V1 — Full schema for TheSpawnPoint
-- ============================================================

-- ------------------------------------------------------------
-- ENUM types
-- ------------------------------------------------------------
CREATE TYPE invite_status     AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED');
CREATE TYPE notification_type AS ENUM ('FRIEND_REQUEST', 'PARTY_INVITE', 'MESSAGE', 'SYSTEM');
CREATE TYPE suggestion_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,
    display_name   VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status         VARCHAR(10)  NOT NULL DEFAULT 'OFFLINE',
    last_seen      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- profiles
-- ------------------------------------------------------------
CREATE TABLE profiles
(
    user_id     BIGINT       NOT NULL PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name   VARCHAR(100) NOT NULL,
    avatar_url  VARCHAR(500),
    bio         TEXT,
    birth_date  DATE,
    platforms   VARCHAR(20)[],
    skill_level VARCHAR(20),
    play_style  VARCHAR(20),
    languages   VARCHAR(50)[],
    country     VARCHAR(100),
    region      VARCHAR(30),
    discord     VARCHAR(100),
    steam       VARCHAR(200),
    twitch      VARCHAR(200),
    xbox        VARCHAR(200),
    playstation VARCHAR(200),
    nintendo    VARCHAR(200),
    CONSTRAINT chk_region CHECK (
        region IS NULL OR region IN (
                                     'EUROPE', 'ASIA', 'NORTH_AMERICA', 'SOUTH_AMERICA',
                                     'OCEANIA', 'AFRICA', 'MIDDLE_EAST'
            )
        )
);

-- ------------------------------------------------------------
-- email_verification_tokens
-- ------------------------------------------------------------
CREATE TABLE email_verification_tokens
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code         VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    last_sent_at TIMESTAMPTZ  NOT NULL
);

-- ------------------------------------------------------------
-- password_reset_tokens
-- ------------------------------------------------------------
CREATE TABLE password_reset_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ------------------------------------------------------------
-- games
-- ------------------------------------------------------------
CREATE TABLE games
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    genre          VARCHAR(50),
    release_year   SMALLINT,
    image_url      VARCHAR(500),
    max_party_size INTEGER      NOT NULL DEFAULT 5
);

-- ------------------------------------------------------------
-- game_suggestions
-- ------------------------------------------------------------
CREATE TABLE game_suggestions
(
    id             BIGSERIAL PRIMARY KEY,
    suggested_by   BIGINT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(100)      NOT NULL,
    genre          VARCHAR(50),
    release_year   SMALLINT,
    image_url      VARCHAR(500),
    max_party_size INTEGER           NOT NULL DEFAULT 5,
    status         suggestion_status NOT NULL DEFAULT 'PENDING',
    admin_comment  TEXT,
    created_at     TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    reviewed_at    TIMESTAMPTZ
);

-- ------------------------------------------------------------
-- user_games
-- ------------------------------------------------------------
CREATE TABLE user_games
(
    id       BIGSERIAL   PRIMARY KEY,
    user_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id  BIGINT      NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, game_id)
);

-- ------------------------------------------------------------
-- friendships
-- ------------------------------------------------------------
CREATE TABLE friendships
(
    id            BIGSERIAL   PRIMARY KEY,
    user_id1      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_id2      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    friends_since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id1, user_id2)
);

-- ------------------------------------------------------------
-- chats
-- ------------------------------------------------------------
CREATE TABLE chats
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(100),
    is_group     BOOLEAN     NOT NULL DEFAULT FALSE,
    party_linked BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- chat_participants
-- ------------------------------------------------------------
CREATE TABLE chat_participants
(
    id        BIGSERIAL   PRIMARY KEY,
    chat_id   BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (chat_id, user_id)
);

-- ------------------------------------------------------------
-- messages
-- ------------------------------------------------------------
CREATE TABLE messages
(
    id        BIGSERIAL PRIMARY KEY,
    chat_id   BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_id BIGINT      REFERENCES users (id),
    content   TEXT        NOT NULL,
    sent_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read      BOOLEAN     NOT NULL DEFAULT FALSE,
    is_system BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_msg_chat   ON messages (chat_id);
CREATE INDEX idx_msg_sender ON messages (sender_id);

-- ------------------------------------------------------------
-- party_requests
-- ------------------------------------------------------------
CREATE TABLE party_requests
(
    id          BIGSERIAL PRIMARY KEY,
    creator_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id     BIGINT      NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    chat_id     BIGINT      UNIQUE REFERENCES chats (id) ON DELETE SET NULL,
    max_members INTEGER     NOT NULL,
    is_open     BOOLEAN     NOT NULL DEFAULT TRUE,
    description TEXT,
    event_time  TIMESTAMPTZ,
    platform    VARCHAR(20)[],
    language    VARCHAR(50),
    skill_level VARCHAR(20),
    play_style  VARCHAR(20),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- party_members
-- ------------------------------------------------------------
CREATE TABLE party_members
(
    id               BIGSERIAL   PRIMARY KEY,
    party_request_id BIGINT      NOT NULL REFERENCES party_requests (id) ON DELETE CASCADE,
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (party_request_id, user_id)
);

-- ------------------------------------------------------------
-- invites
-- ------------------------------------------------------------
CREATE TABLE invites
(
    id              BIGSERIAL     PRIMARY KEY,
    sender_id       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type            VARCHAR(20)   NOT NULL DEFAULT 'FRIEND_REQUEST',
    status          invite_status NOT NULL DEFAULT 'PENDING',
    party_request_id BIGINT       REFERENCES party_requests (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    responded_at    TIMESTAMPTZ
);

-- ------------------------------------------------------------
-- notifications
-- ------------------------------------------------------------
CREATE TABLE notifications
(
    id           BIGSERIAL         PRIMARY KEY,
    user_id      BIGINT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         notification_type NOT NULL,
    message      TEXT              NOT NULL,
    reference_id BIGINT,
    is_read      BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);