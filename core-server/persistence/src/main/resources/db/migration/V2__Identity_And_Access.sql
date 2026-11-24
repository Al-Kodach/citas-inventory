CREATE TABLE IF NOT EXISTS users (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name                    VARCHAR(100) NOT NULL,
    username                VARCHAR(100) UNIQUE NOT NULL,
    phone                   VARCHAR(64) UNIQUE NOT NULL,
    is_phone_verified       BOOLEAN NOT NULL DEFAULT FALSE,
    email                   VARCHAR(255) UNIQUE,
    password_hash           TEXT NOT NULL,
    user_type               VARCHAR(50) NOT NULL,
    mfa_enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    role                    VARCHAR(50) NOT NULL,
    capabilities            TEXT,
    suspended_reason        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    workspace_id            UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    device_id               VARCHAR(255) NOT NULL,
    token_hash              VARCHAR(225) NOT NULL DEFAULT '',
    family_id               UUID NOT NULL DEFAULT uuid_generate_v4(),
    last_ip                 VARCHAR(255),
    user_agent              VARCHAR(255),
    platform                VARCHAR(255),
    app_version             VARCHAR(255),
    is_revoked              BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at              TIMESTAMPTZ,
    revoke_reason           VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS platform_admins (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                    VARCHAR(100) NOT NULL,
    username                VARCHAR(100) UNIQUE NOT NULL,
    phone                   VARCHAR(64) UNIQUE NOT NULL,
    email                   VARCHAR(255) UNIQUE NOT NULL,
    password_hash           TEXT NOT NULL,
    role                    VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    active_hardware_id      VARCHAR(255),
    mfa_enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_workspace_bindings (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    assigned_role           VARCHAR(50) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                 UUID NOT NULL,
    code_hash               VARCHAR(255) NOT NULL,
    purpose                 VARCHAR(50) NOT NULL,
    expires_at              TIMESTAMPTZ NOT NULL,
    is_used                 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
