-- Add updated_at column to workspace_resources if missing
ALTER TABLE workspace_resources ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
