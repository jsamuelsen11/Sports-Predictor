CREATE TABLE prediction_log (
    id                TEXT NOT NULL PRIMARY KEY,
    event_id          TEXT NOT NULL,
    sport             TEXT NOT NULL,
    prediction_type   TEXT NOT NULL,
    predicted_outcome TEXT NOT NULL,
    confidence        REAL NOT NULL,
    actual_outcome    TEXT,
    key_factors       TEXT NOT NULL,
    created_at        TEXT NOT NULL,
    settled_at        TEXT
);

CREATE INDEX idx_prediction_log_event_id ON prediction_log(event_id);
CREATE INDEX idx_prediction_log_sport ON prediction_log(sport);
CREATE INDEX idx_prediction_log_created_at ON prediction_log(created_at);
