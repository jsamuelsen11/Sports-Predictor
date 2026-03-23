CREATE TABLE bet (
    id               TEXT NOT NULL PRIMARY KEY,
    bankroll_id      TEXT NOT NULL,
    bet_type         TEXT NOT NULL,
    status           TEXT NOT NULL,
    stake            REAL NOT NULL,
    odds             REAL NOT NULL,
    potential_payout REAL NOT NULL,
    actual_payout    REAL,
    sport            TEXT NOT NULL,
    event_id         TEXT NOT NULL,
    description      TEXT NOT NULL,
    placed_at        TEXT NOT NULL,
    settled_at       TEXT,
    metadata         TEXT,
    FOREIGN KEY (bankroll_id) REFERENCES bankroll(id) ON DELETE RESTRICT
);

CREATE TABLE bet_leg (
    id            TEXT NOT NULL PRIMARY KEY,
    bet_id        TEXT NOT NULL,
    leg_number    INTEGER NOT NULL,
    selection     TEXT NOT NULL,
    odds          REAL NOT NULL,
    status        TEXT NOT NULL,
    event_id      TEXT NOT NULL,
    sport         TEXT NOT NULL,
    result_detail TEXT,
    FOREIGN KEY (bet_id) REFERENCES bet(id) ON DELETE RESTRICT,
    UNIQUE (bet_id, leg_number)
);

CREATE INDEX idx_bet_bankroll_id ON bet(bankroll_id);
CREATE INDEX idx_bet_status ON bet(status);
CREATE INDEX idx_bet_sport ON bet(sport);
CREATE INDEX idx_bet_event_id ON bet(event_id);
CREATE INDEX idx_bet_placed_at ON bet(placed_at);
CREATE INDEX idx_bet_leg_bet_id ON bet_leg(bet_id);
