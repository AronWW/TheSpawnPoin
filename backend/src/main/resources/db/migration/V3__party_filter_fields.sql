-- V3 — Add filter fields to party_requests

ALTER TABLE party_requests
    ADD COLUMN platform    VARCHAR(20)[],
    ADD COLUMN language    VARCHAR(50),
    ADD COLUMN skill_level VARCHAR(20),
    ADD COLUMN play_style  VARCHAR(20);

