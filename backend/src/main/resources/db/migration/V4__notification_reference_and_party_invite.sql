-- V4 — Add reference_id to notifications + party_request_id to invites

-- Notifications: link to the related entity (invite id, party id, etc.)
ALTER TABLE notifications ADD COLUMN reference_id BIGINT;

-- Invites: link to a party_request for PARTY_INVITE type
ALTER TABLE invites ADD COLUMN party_request_id BIGINT REFERENCES party_requests (id) ON DELETE CASCADE;

