-- V3 — Add type column to invites for distinguishing friend requests from party invites

ALTER TABLE invites ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'FRIEND_REQUEST';

