-- V8: Chat enhancements — delete/edit messages, reply, reactions, pin, archive, pinned chats

-- === Messages: soft delete ===
ALTER TABLE messages ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN deleted_at TIMESTAMPTZ;

-- === Messages: edit ===
ALTER TABLE messages ADD COLUMN edited BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN edited_at TIMESTAMPTZ;

-- === Messages: reply ===
ALTER TABLE messages ADD COLUMN reply_to_id BIGINT;
ALTER TABLE messages ADD CONSTRAINT fk_msg_reply FOREIGN KEY (reply_to_id) REFERENCES messages(id) ON DELETE SET NULL;
CREATE INDEX idx_msg_reply ON messages(reply_to_id);

-- === Chat participants: archive / pin / soft-delete per user ===
ALTER TABLE chat_participants ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE chat_participants ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE chat_participants ADD COLUMN pinned_at TIMESTAMPTZ;
ALTER TABLE chat_participants ADD COLUMN deleted_at TIMESTAMPTZ;

-- === Message reactions (many-to-many: user × message × emoji) ===
CREATE TABLE message_reactions (
    id          BIGSERIAL   PRIMARY KEY,
    message_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    emoji       VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT uq_reaction         UNIQUE (message_id, user_id, emoji)
);
CREATE INDEX idx_reaction_msg ON message_reactions(message_id);

-- === Pinned messages (per chat, who pinned and when) ===
CREATE TABLE pinned_messages (
    id          BIGSERIAL   PRIMARY KEY,
    chat_id     BIGINT      NOT NULL,
    message_id  BIGINT      NOT NULL,
    pinned_by   BIGINT      NOT NULL,
    pinned_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pin_chat    FOREIGN KEY (chat_id)    REFERENCES chats(id)    ON DELETE CASCADE,
    CONSTRAINT fk_pin_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_pin_user    FOREIGN KEY (pinned_by)  REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT uq_pinned_msg  UNIQUE (chat_id, message_id)
);
