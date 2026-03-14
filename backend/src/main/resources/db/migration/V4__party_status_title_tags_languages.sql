-- ============================================================
-- V4 — Party lifecycle: status, title, tags, languages array
-- ============================================================

-- 1. Add status column
ALTER TABLE party_requests
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN';

-- 2. Migrate existing data: is_open=true → OPEN, is_open=false → CANCELLED
UPDATE party_requests SET status = 'OPEN'      WHERE is_open = TRUE;
UPDATE party_requests SET status = 'CANCELLED' WHERE is_open = FALSE;

-- 3. Add title column
ALTER TABLE party_requests
    ADD COLUMN title VARCHAR(150);

-- 4. Convert language from single value to array
ALTER TABLE party_requests
    ADD COLUMN languages VARCHAR(50)[];

UPDATE party_requests
SET languages = CASE
    WHEN language IS NOT NULL AND language != '' THEN ARRAY[language]::VARCHAR(50)[]
    ELSE ARRAY[]::VARCHAR(50)[]
END;

ALTER TABLE party_requests DROP COLUMN language;

-- 5. Add tags array column
ALTER TABLE party_requests
    ADD COLUMN tags VARCHAR(30)[];

-- 6. Add region column
ALTER TABLE party_requests
    ADD COLUMN region VARCHAR(30);

-- 7. Index on status for fast filtering
CREATE INDEX idx_party_requests_status ON party_requests (status);

