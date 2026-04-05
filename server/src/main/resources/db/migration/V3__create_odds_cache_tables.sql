CREATE TABLE odds_snapshot (
    id          TEXT NOT NULL PRIMARY KEY,
    event_id    TEXT NOT NULL,
    sport       TEXT NOT NULL,
    bookmaker   TEXT NOT NULL,
    market      TEXT NOT NULL,
    odds_data   TEXT NOT NULL,
    captured_at TEXT NOT NULL
);

CREATE INDEX idx_odds_snapshot_event_id ON odds_snapshot(event_id);
CREATE INDEX idx_odds_snapshot_sport ON odds_snapshot(sport);
CREATE INDEX idx_odds_snapshot_captured_at ON odds_snapshot(captured_at);
