

-- ENUM types

CREATE TYPE invite_status    AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED');
CREATE TYPE notification_type AS ENUM ('FRIEND_REQUEST', 'PARTY_INVITE', 'MESSAGE', 'SYSTEM');
CREATE TYPE suggestion_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');


CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,
    display_name   VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    status         VARCHAR(10)  NOT NULL DEFAULT 'OFFLINE',
    last_seen      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE profiles
(
    user_id    BIGINT       NOT NULL PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name  VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(255),
    bio        TEXT,
    birth_date DATE
);

CREATE TABLE email_verification_tokens
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code         VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    last_sent_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE password_reset_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE games
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    genre           VARCHAR(50),
    release_year    SMALLINT,
    image_url       VARCHAR(500),
    max_party_size  INTEGER      NOT NULL DEFAULT 5
);

CREATE TABLE game_suggestions
(
    id             BIGSERIAL PRIMARY KEY,
    suggested_by   BIGINT           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(100)     NOT NULL,
    genre          VARCHAR(50),
    release_year   SMALLINT,
    image_url      VARCHAR(500),
    max_party_size INTEGER          NOT NULL DEFAULT 5,
    status         suggestion_status NOT NULL DEFAULT 'PENDING',
    admin_comment  TEXT,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    reviewed_at    TIMESTAMPTZ
);

CREATE TABLE user_games
(
    user_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id  BIGINT      NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, game_id)
);

CREATE TABLE friendships
(
    user_id1      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_id2      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    friends_since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id1, user_id2)
);

CREATE TABLE chats
(
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(100),
    is_group   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_participants
(
    chat_id   BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE messages
(
    id        BIGSERIAL PRIMARY KEY,
    chat_id   BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_id BIGINT      NOT NULL REFERENCES users (id),
    content   TEXT        NOT NULL,
    sent_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read      BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_msg_chat   ON messages (chat_id);
CREATE INDEX idx_msg_sender ON messages (sender_id);

CREATE TABLE party_requests
(
    id          BIGSERIAL PRIMARY KEY,
    creator_id  BIGINT  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id     BIGINT  NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    chat_id     BIGINT  REFERENCES chats (id) ON DELETE SET NULL,
    max_members INTEGER NOT NULL,
    is_open     BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    event_time  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE party_members
(
    party_request_id BIGINT      NOT NULL REFERENCES party_requests (id) ON DELETE CASCADE,
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (party_request_id, user_id)
);

CREATE TABLE invites
(
    id           BIGSERIAL PRIMARY KEY,
    sender_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       invite_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMPTZ
);

CREATE TABLE notifications
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       notification_type NOT NULL,
    message    TEXT              NOT NULL,
    is_read    BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

