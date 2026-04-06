-- Phase 2: Add columns for round-robin sub-parlays and futures expiration.
ALTER TABLE bet ADD COLUMN parent_bet_id TEXT REFERENCES bet(id) ON DELETE RESTRICT;
ALTER TABLE bet ADD COLUMN expires_at TEXT;

CREATE INDEX idx_bet_parent_bet_id ON bet(parent_bet_id);
CREATE INDEX idx_bet_expires_at ON bet(expires_at);
