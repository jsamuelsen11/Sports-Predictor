CREATE TABLE bankroll (
    id               TEXT NOT NULL PRIMARY KEY,
    name             TEXT NOT NULL,
    starting_balance REAL NOT NULL,
    current_balance  REAL NOT NULL,
    created_at       TEXT NOT NULL,
    archived_at      TEXT
);

CREATE TABLE bankroll_transaction (
    id               TEXT NOT NULL PRIMARY KEY,
    bankroll_id      TEXT NOT NULL,
    type             TEXT NOT NULL,
    amount           REAL NOT NULL,
    balance_after    REAL NOT NULL,
    reference_bet_id TEXT,
    created_at       TEXT NOT NULL,
    FOREIGN KEY (bankroll_id) REFERENCES bankroll(id) ON DELETE RESTRICT
);

CREATE INDEX idx_bankroll_transaction_bankroll_id ON bankroll_transaction(bankroll_id);
CREATE INDEX idx_bankroll_transaction_created_at ON bankroll_transaction(created_at);
