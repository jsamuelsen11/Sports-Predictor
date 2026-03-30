-- Monte Carlo simulation results
CREATE TABLE IF NOT EXISTS simulation_result (
    id          TEXT NOT NULL PRIMARY KEY,
    event_id    TEXT NOT NULL,
    sport       TEXT NOT NULL,
    team1_id    TEXT NOT NULL,
    team2_id    TEXT NOT NULL,
    num_simulations INTEGER NOT NULL,
    result_data TEXT NOT NULL,
    created_at  TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_simulation_result_event_id ON simulation_result(event_id);
CREATE INDEX IF NOT EXISTS idx_simulation_result_sport    ON simulation_result(sport);

-- Odds monitoring subscriptions
CREATE TABLE IF NOT EXISTS odds_monitor (
    id               TEXT NOT NULL PRIMARY KEY,
    event_id         TEXT NOT NULL,
    interval_minutes INTEGER NOT NULL,
    duration_hours   INTEGER NOT NULL,
    active           INTEGER NOT NULL DEFAULT 1,
    started_at       TEXT NOT NULL,
    expires_at       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_odds_monitor_event_id ON odds_monitor(event_id);
CREATE INDEX IF NOT EXISTS idx_odds_monitor_active   ON odds_monitor(active);

-- Closing line snapshots for CLV calculation
CREATE TABLE IF NOT EXISTS closing_odds (
    id               TEXT NOT NULL PRIMARY KEY,
    event_id         TEXT NOT NULL,
    sport            TEXT NOT NULL,
    market           TEXT NOT NULL,
    closing_odds_data TEXT NOT NULL,
    game_start_time  TEXT NOT NULL,
    captured_at      TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_closing_odds_event_id ON closing_odds(event_id);
CREATE INDEX IF NOT EXISTS idx_closing_odds_sport    ON closing_odds(sport);

-- Live betting and cash-out support on bet table
ALTER TABLE bet ADD COLUMN cash_out_amount REAL;
ALTER TABLE bet ADD COLUMN cashed_out_at TEXT;
ALTER TABLE bet ADD COLUMN is_live INTEGER DEFAULT 0;

-- SGP correlation grouping on bet_leg table
ALTER TABLE bet_leg ADD COLUMN correlation_group TEXT;
