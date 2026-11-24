CREATE TABLE IF NOT EXISTS processed_sync_events (
    event_id VARCHAR(128) PRIMARY KEY,
    workspace_id UUID NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    http_status INT NOT NULL,
    response_body TEXT,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CLOCK_TIMESTAMP()
);

CREATE INDEX IF NOT EXISTS idx_processed_sync_events_processed_at ON processed_sync_events (processed_at);
