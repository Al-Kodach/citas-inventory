CREATE TABLE IF NOT EXISTS sms_audit_ledger (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id            UUID NOT NULL,
    recipient_phone         VARCHAR(64) NOT NULL,
    message                 TEXT NOT NULL,
    cost                    INTEGER NOT NULL,
    status                  VARCHAR(50) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS background_jobs (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_type                VARCHAR(255) NOT NULL,
    payload                 TEXT,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority                INTEGER NOT NULL DEFAULT 0,
    correlation_id          VARCHAR(255),
    attempts                INTEGER NOT NULL DEFAULT 0,
    max_attempts            INTEGER NOT NULL DEFAULT 3,
    last_error              TEXT,
    last_attempt_at         TIMESTAMPTZ DEFAULT NULL,
    scheduled_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS background_job_registry (
    job_name                VARCHAR(64) PRIMARY KEY,
    interval_minutes        INTEGER NOT NULL DEFAULT 30,
    is_enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    last_executed_at        TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO background_job_registry (job_name, interval_minutes, is_enabled) VALUES
('TELEMETRY_MAINTENANCE', 30, true),
('EXPIRY_ALERT_PROCESSOR', 60, true),
('SESSION_CLEANUP', 60, true),
('SYNC_EVENT_CLEANUP', 1440, true)
ON CONFLICT (job_name) DO NOTHING;
