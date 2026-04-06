-- Phase 2: Bankroll rules engine for wagering limits and risk management.
CREATE TABLE bankroll_rules (
    id                    TEXT NOT NULL PRIMARY KEY,
    bankroll_id           TEXT NOT NULL,
    max_bet_units         REAL,
    daily_exposure_limit  REAL,
    stop_loss_threshold   REAL,
    max_parlay_legs       INTEGER,
    min_confidence        REAL,
    unit_size             REAL,
    created_at            TEXT NOT NULL,
    updated_at            TEXT NOT NULL,
    FOREIGN KEY (bankroll_id) REFERENCES bankroll(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX idx_bankroll_rules_bankroll_id ON bankroll_rules(bankroll_id);
