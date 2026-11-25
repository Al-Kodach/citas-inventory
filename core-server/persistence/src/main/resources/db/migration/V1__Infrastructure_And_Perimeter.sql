CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Infrastructure & SIEM Logs
CREATE TABLE IF NOT EXISTS identity_access_logs (
    id                      BIGSERIAL PRIMARY KEY,
    request_id              VARCHAR(50) NOT NULL,
    workspace_id            UUID,
    organization_id         UUID,
    branch_id               UUID,
    device_id               VARCHAR(255),
    product_type            VARCHAR(50) NOT NULL,
    level                   VARCHAR(20) NOT NULL DEFAULT 'INFO',
    user_id                 VARCHAR(50),
    action                  VARCHAR(100) NOT NULL,
    outcome                 VARCHAR(50) NOT NULL,
    failure_reason          TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS endpoint_host_logs (
    id                      BIGSERIAL PRIMARY KEY,
    request_id              VARCHAR(50) NOT NULL,
    workspace_id            UUID,
    organization_id         UUID,
    branch_id               UUID,
    device_id               VARCHAR(255),
    product_type            VARCHAR(50) NOT NULL,
    level                   VARCHAR(20) NOT NULL DEFAULT 'INFO',
    os_name                 VARCHAR(50) NOT NULL,
    architecture            VARCHAR(20) NOT NULL,
    action                  VARCHAR(100) NOT NULL,
    fingerprint_hash        VARCHAR(255) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_audit_logs (
    id                      BIGSERIAL PRIMARY KEY,
    request_id              VARCHAR(50) NOT NULL,
    workspace_id            UUID,
    organization_id         UUID,
    branch_id               UUID,
    device_id               VARCHAR(255),
    product_type            VARCHAR(50) NOT NULL,
    level                   VARCHAR(20) NOT NULL DEFAULT 'INFO',
    actor_id                VARCHAR(50) NOT NULL,
    entity_type             VARCHAR(50) NOT NULL,
    entity_id               VARCHAR(50) NOT NULL,
    action                  VARCHAR(100) NOT NULL,
    summary                 TEXT NOT NULL,
    payload_changes         JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS network_security_logs (
    id                      BIGSERIAL PRIMARY KEY,
    request_id              VARCHAR(50) NOT NULL,
    workspace_id            UUID,
    organization_id         UUID,
    branch_id               UUID,
    device_id               VARCHAR(255),
    product_type            VARCHAR(50) NOT NULL,
    level                   VARCHAR(20) NOT NULL DEFAULT 'INFO',
    client_ip               VARCHAR(64) NOT NULL,
    endpoint_route          VARCHAR(255) NOT NULL,
    trigger_event           VARCHAR(100) NOT NULL,
    blocked_duration_seconds INTEGER,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operational_health_logs (
    id                      BIGSERIAL PRIMARY KEY,
    request_id              VARCHAR(50) NOT NULL,
    workspace_id            UUID,
    organization_id         UUID,
    branch_id               UUID,
    device_id               VARCHAR(255),
    product_type            VARCHAR(50) NOT NULL,
    _level                   VARCHAR(20) NOT NULL DEFAULT 'INFO',
    component               VARCHAR(100) NOT NULL,
    message                 TEXT NOT NULL,
    execution_time_ms       BIGINT,
    stack_trace             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_operational_health_logs_comp ON operational_health_logs(component);

-- Perimeter & Tenants
CREATE TABLE IF NOT EXISTS organizations (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                    VARCHAR(255) NOT NULL,
    email                   VARCHAR(255),
    phone                   VARCHAR(64),
    business_code           VARCHAR(255) UNIQUE NOT NULL,
    owner_id                UUID,
    fleet_dna               VARCHAR(100),
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organization_accounts (
    organization_id         UUID PRIMARY KEY REFERENCES organizations(id) ON DELETE CASCADE,
    total_balance           INTEGER NOT NULL DEFAULT 0,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'GHS',
    total_workstations      INTEGER NOT NULL DEFAULT 0,
    total_active_branches   INTEGER NOT NULL DEFAULT 0,
    total_staff_accounts    INTEGER NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspaces (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name                    VARCHAR(255) NOT NULL,
    email                   VARCHAR(150),
    phone                   TEXT,
    suspended_reason        TEXT,
    address                 JSONB,
    product_type            VARCHAR(255) UNIQUE NOT NULL,
    product_key             VARCHAR(255) UNIQUE NOT NULL,
    business_code           VARCHAR(255) UNIQUE NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    deployment_mode         VARCHAR(50) NOT NULL DEFAULT 'CLOUD_SYNCED',
    version                 INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS branches (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name                    VARCHAR(255) NOT NULL,
    location_code           VARCHAR(50) NOT NULL,
    contact_phone           VARCHAR(64),
    email                   VARCHAR(255),
    website                 VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_accounts (
    workspace_id            UUID PRIMARY KEY REFERENCES workspaces(id) ON DELETE CASCADE,
    deployment_mode         VARCHAR(50) NOT NULL DEFAULT 'CLOUD_SYNCED',
    total_balance           INTEGER NOT NULL DEFAULT 0,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'GHS',
    sms_balance             INTEGER NOT NULL DEFAULT 0,
    payment_provider_customer_id VARCHAR(255),
    default_payment_method_id VARCHAR(255),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_resources (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    package_id              VARCHAR(50) NOT NULL,
    package_tier            VARCHAR(50) NOT NULL,
    package_name            VARCHAR(255) NOT NULL,
    max_users               INTEGER NOT NULL,
    max_device              INTEGER NOT NULL,
    max_branches            INTEGER NOT NULL,
    max_products_limit      INTEGER NOT NULL,
    max_active_batches_limit INTEGER NOT NULL,
    retention_months        INTEGER NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_modules (
    module  VARCHAR(255) NOT NULL,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    PRIMARY KEY (module, workspace_id)
);

CREATE TABLE IF NOT EXISTS workspace_resource_usages (
    workspace_id            UUID PRIMARY KEY REFERENCES workspaces(id) ON DELETE CASCADE,
    product_count_used      INTEGER NOT NULL DEFAULT 0,
    active_batches_count    INTEGER NOT NULL DEFAULT 0,
    user_count              INTEGER NOT NULL DEFAULT 0,
    device_count            INTEGER NOT NULL DEFAULT 0,
    branch_count            INTEGER NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_subscriptions (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    tier                    VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    billing_cycle           VARCHAR(20) NOT NULL,
    line_items_json         TEXT NOT NULL,
    recurring_amount_minor  BIGINT NOT NULL,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'GHS',
    start_date              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date                TIMESTAMPTZ NOT NULL DEFAULT '2100-01-01T00:00:00Z',
    canceled_at             TIMESTAMPTZ,
    version                 INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_transactions (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    amount_minor            BIGINT NOT NULL,
    currency_code           VARCHAR(3) NOT NULL,
    payment_reference       VARCHAR(255),
    transaction_type        VARCHAR(50) NOT NULL,
    description             TEXT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
