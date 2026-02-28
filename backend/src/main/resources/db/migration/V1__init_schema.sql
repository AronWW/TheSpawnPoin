
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


CREATE SEQUENCE IF NOT EXISTS chats_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS email_verification_tokens_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS games_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS invites_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS messages_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS notifications_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS party_requests_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS password_reset_tokens_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS users_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE chat_participants
(
    joined_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    chat_id   BIGINT                      NOT NULL,
    user_id   BIGINT                      NOT NULL,
    CONSTRAINT chat_participants_pkey PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE chats
(
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    is_group   BOOLEAN                                 NOT NULL,
    title      VARCHAR(100),
    CONSTRAINT chats_pkey PRIMARY KEY (id)
);

CREATE TABLE email_verification_tokens
(
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    code         VARCHAR(255)                            NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    last_sent_at TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    user_id      BIGINT                                  NOT NULL,
    CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE friendships
(
    friends_since TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id1      BIGINT                      NOT NULL,
    user_id2      BIGINT                      NOT NULL,
    CONSTRAINT friendships_pkey PRIMARY KEY (user_id1, user_id2)
);

CREATE TABLE games
(
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    genre        VARCHAR(50),
    name         VARCHAR(100)                            NOT NULL,
    release_year SMALLINT,
    CONSTRAINT games_pkey PRIMARY KEY (id)
);

CREATE TABLE invites
(
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    responded_at TIMESTAMP WITHOUT TIME ZONE,
    status       VARCHAR(20)                             NOT NULL,
    receiver_id  BIGINT                                  NOT NULL,
    sender_id    BIGINT                                  NOT NULL,
    CONSTRAINT invites_pkey PRIMARY KEY (id)
);

CREATE TABLE messages
(
    id        BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    content   TEXT                                    NOT NULL,
    read      BOOLEAN                                 NOT NULL,
    sent_at   TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    chat_id   BIGINT                                  NOT NULL,
    sender_id BIGINT                                  NOT NULL,
    CONSTRAINT messages_pkey PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    is_read    BOOLEAN                                 NOT NULL,
    message    TEXT                                    NOT NULL,
    type       VARCHAR(30)                             NOT NULL,
    user_id    BIGINT                                  NOT NULL,
    CONSTRAINT notifications_pkey PRIMARY KEY (id)
);

CREATE TABLE party_members
(
    joined_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_request_id BIGINT                      NOT NULL,
    user_id          BIGINT                      NOT NULL,
    CONSTRAINT party_members_pkey PRIMARY KEY (party_request_id, user_id)
);

CREATE TABLE party_requests
(
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    description TEXT,
    event_time  TIMESTAMP WITHOUT TIME ZONE,
    is_open     BOOLEAN                                 NOT NULL,
    max_members INTEGER                                 NOT NULL,
    chat_id     BIGINT,
    creator_id  BIGINT                                  NOT NULL,
    game_id     BIGINT                                  NOT NULL,
    CONSTRAINT party_requests_pkey PRIMARY KEY (id)
);

CREATE TABLE password_reset_tokens
(
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    token      VARCHAR(255)                            NOT NULL,
    used       BOOLEAN                                 NOT NULL,
    user_id    BIGINT                                  NOT NULL,
    CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE profiles
(
    user_id    BIGINT       NOT NULL,
    avatar_url VARCHAR(255),
    bio        TEXT,
    birth_date date,
    full_name  VARCHAR(100) NOT NULL,
    CONSTRAINT profiles_pkey PRIMARY KEY (user_id)
);

CREATE TABLE user_games
(
    added_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    game_id  BIGINT                      NOT NULL,
    user_id  BIGINT                      NOT NULL,
    CONSTRAINT user_games_pkey PRIMARY KEY (game_id, user_id)
);

CREATE TABLE users
(
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    display_name   VARCHAR(100)                            NOT NULL,
    email          VARCHAR(255)                            NOT NULL,
    email_verified BOOLEAN                                 NOT NULL,
    last_seen      TIMESTAMP WITHOUT TIME ZONE,
    password       VARCHAR(255)                            NOT NULL,
    status         VARCHAR(255)                            NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE party_requests
    ADD CONSTRAINT uk6x8tyg3l357ye2dwsrtw8ryrh UNIQUE (chat_id);

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT uk71lqwbwtklmljk3qlsugr1mig UNIQUE (token);

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT ukla2ts67g4oh2sreayswhox1i6 UNIQUE (user_id);

ALTER TABLE email_verification_tokens
    ADD CONSTRAINT uks3mje1c85ftmp2uld6dt1bffs UNIQUE (user_id);

ALTER TABLE profiles
    ADD CONSTRAINT fk410q61iev7klncmpqfuo85ivh FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE messages
    ADD CONSTRAINT fk4ui4nnwntodh6wjvck53dbk9m FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_msg_sender ON messages (sender_id);

ALTER TABLE messages
    ADD CONSTRAINT fk64w44ngcpqp99ptcb9werdfmb FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE;

CREATE INDEX idx_msg_chat ON messages (chat_id);

ALTER TABLE party_members
    ADD CONSTRAINT fk6jjanh87sd545vceexteud25l FOREIGN KEY (party_request_id) REFERENCES party_requests (id) ON DELETE CASCADE;

ALTER TABLE party_requests
    ADD CONSTRAINT fk743h1vpi72rk34mtbb2hcn2fa FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE SET NULL;

ALTER TABLE invites
    ADD CONSTRAINT fk75woausg2ayjkiig7yri35acp FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE chat_participants
    ADD CONSTRAINT fkbhdyxo0ndtbs1t49l28y21rkw FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE party_requests
    ADD CONSTRAINT fke1pcrrbuow9rv36xl5fyn6e0l FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_games
    ADD CONSTRAINT fkei8splhi2h9lj8qr4qribaohi FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE;

ALTER TABLE email_verification_tokens
    ADD CONSTRAINT fki1c4mmamlb8keqt74k4lrtwhc FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE friendships
    ADD CONSTRAINT fkjvdxo4hjiy15e3r0pqekrlxsd FOREIGN KEY (user_id1) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT fkk3ndxg5xp6v7wd4gjyusp15gq FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE user_games
    ADD CONSTRAINT fkk85sd2rb5dpiuekcoyucllpm4 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE friendships
    ADD CONSTRAINT fkkvv442rnfccd243t2drh5jl2i FOREIGN KEY (user_id2) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE invites
    ADD CONSTRAINT fkmlowdf92p0f9ngb43q3i12q6t FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE chat_participants
    ADD CONSTRAINT fkn4feij8janlba38q59kl2ebgg FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE;

ALTER TABLE party_members
    ADD CONSTRAINT fkolpgeuo237h78okianm86vn9c FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE party_requests
    ADD CONSTRAINT fks796xif3u87y1y7x0m8l7d99k FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE;
