-- ============================================================
-- V5 — Party timing: started_at, completed_at for stats
-- ============================================================

ALTER TABLE party_requests
    ADD COLUMN started_at      TIMESTAMPTZ,
    ADD COLUMN completed_at    TIMESTAMPTZ,
    ADD COLUMN auto_completed  BOOLEAN NOT NULL DEFAULT FALSE;

-- Back-fill existing IN_GAME parties: approximate started_at as created_at + 5 min
UPDATE party_requests
SET started_at = created_at + INTERVAL '5 minutes'
WHERE status = 'IN_GAME' AND started_at IS NULL;

-- Back-fill existing COMPLETED parties: approximate both timestamps
UPDATE party_requests
SET started_at   = COALESCE(started_at, created_at + INTERVAL '5 minutes'),
    completed_at = COALESCE(completed_at, created_at + INTERVAL '1 hour')
WHERE status = 'COMPLETED';

