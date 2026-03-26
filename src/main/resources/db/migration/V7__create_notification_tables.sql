-- Phase 2: Notification system for line movement and injury subscriptions.
CREATE TABLE subscription (
    id         TEXT NOT NULL PRIMARY KEY,
    type       TEXT NOT NULL,
    sport      TEXT,
    threshold  REAL,
    filters    TEXT,
    active     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL
);

CREATE TABLE alert (
    id              TEXT NOT NULL PRIMARY KEY,
    subscription_id TEXT REFERENCES subscription(id),
    type            TEXT NOT NULL,
    title           TEXT NOT NULL,
    message         TEXT NOT NULL,
    read            INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT NOT NULL
);

CREATE INDEX idx_subscription_type ON subscription(type);
CREATE INDEX idx_alert_read ON alert(read);
CREATE INDEX idx_alert_subscription_id ON alert(subscription_id);
