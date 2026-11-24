CREATE TABLE IF NOT EXISTS billing_records (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    amount_minor            BIGINT NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    reference_code          VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    summary                 VARCHAR(100) NOT NULL,
    payment_recipient_name  VARCHAR(255) NOT NULL DEFAULT '',
    payment_target          VARCHAR(255) NOT NULL,
    payment_channel         VARCHAR(20) NOT NULL,
    type                    VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    reason                  VARCHAR(20) NOT NULL DEFAULT 'SUBSCRIPTION',
    origin                  VARCHAR(20) NOT NULL DEFAULT 'USER',
    quote                   TEXT NOT NULL DEFAULT '{}',
    payment_submission      TEXT,
    fulfillment_attempts    INTEGER NOT NULL DEFAULT 0,
    last_fulfillment_error  TEXT,
    fulfilled_at            TIMESTAMPTZ,
    created_by              VARCHAR(255),
    verified_by             VARCHAR(255),
    subscription_version    INTEGER,
    expires_at              TIMESTAMPTZ DEFAULT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_packages (
    id                      VARCHAR(50) PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    tier                    VARCHAR(50) NOT NULL,
    description             TEXT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_package_limits (
    package_id              VARCHAR(50) PRIMARY KEY REFERENCES product_packages(id) ON DELETE CASCADE,
    max_users               INTEGER NOT NULL,
    max_device              INTEGER NOT NULL,
    max_branches            INTEGER NOT NULL,
    max_products_limit      INTEGER NOT NULL,
    max_active_batches_limit INTEGER NOT NULL,
    sms_quota_per_cycle     INTEGER NOT NULL,
    retention_months        INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS product_package_modules (
    package_id              VARCHAR(50) REFERENCES product_packages(id) ON DELETE CASCADE,
    module                  VARCHAR(50) NOT NULL,
    PRIMARY KEY (package_id, module)
);

CREATE TABLE IF NOT EXISTS package_billing_terms (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    package_id              VARCHAR(50) REFERENCES product_packages(id) ON DELETE CASCADE,
    deployment_mode         VARCHAR(50) NOT NULL,
    billing_cycle           VARCHAR(20) NOT NULL,
    price_amount            INTEGER NOT NULL,
    is_default              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS sms_packages (
    id                      VARCHAR(50) PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    units                   INTEGER NOT NULL,
    price_amount            INTEGER NOT NULL,
    currency_code           VARCHAR(10) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1. Application Releases (Citas Workstation, Citread TMS, Citas Mobile POS)
CREATE TABLE IF NOT EXISTS app_releases (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(50) NOT NULL,
    app_name VARCHAR(100) NOT NULL,
    version VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'STABLE',
    changelog TEXT,
    is_latest BOOLEAN NOT NULL DEFAULT true,
    is_published BOOLEAN NOT NULL DEFAULT true,
    is_active_production_release BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_version UNIQUE (app_id, version)
);

-- 2. Specific Binary Artifacts per Release (JVM 17, JVM 21, Android ARM64, etc.)
CREATE TABLE IF NOT EXISTS release_artifacts (
    id BIGSERIAL PRIMARY KEY,
    release_id BIGINT NOT NULL REFERENCES app_releases(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    architecture VARCHAR(50) NOT NULL,
    package_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(150) NOT NULL,
    download_url TEXT NOT NULL,
    file_size_mb NUMERIC(8,2) DEFAULT 0,
    sha256_checksum VARCHAR(64),
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Download Analytics / Audit Log
CREATE TABLE IF NOT EXISTS app_downloads (
    id BIGSERIAL PRIMARY KEY,
    artifact_id BIGINT REFERENCES release_artifacts(id) ON DELETE SET NULL,
    app_id VARCHAR(50) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    architecture VARCHAR(50),
    app_version VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    downloaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_releases_app_id ON app_releases(app_id);
CREATE INDEX IF NOT EXISTS idx_artifacts_release ON release_artifacts(release_id);
CREATE INDEX IF NOT EXISTS idx_downloads_app_platform ON app_downloads(app_id, platform);

-- SEED INITIAL RELEASE RECORD (v2.4.1)
INSERT INTO app_releases (id, app_id, app_name, version, channel, changelog, is_latest, is_published, is_active_production_release)
VALUES
(1, 'citas-workstation', 'Citas Industrial Workstation', 'v2.4.1', 'STABLE', 'Full-featured management console and POS engine. Zero-trust local network mesh binding with Compose Multiplatform.', true, true, true),
(2, 'citas-mobile', 'Citas Mobile POS & Scanner', 'v2.4.1', 'STABLE', 'Ultra-fast mobile terminal with camera barcode scanning, offline mesh sync, and Bluetooth thermal receipt printer support.', true, true, true)
ON CONFLICT (app_id, version) DO NOTHING;

INSERT INTO release_artifacts (id, release_id, platform, architecture, package_type, file_name, download_url, file_size_mb, sha256_checksum)
VALUES
(1, 1, 'LINUX', 'JVM_17', 'DEB', 'citas-desktop-v2.4.1-jvm17.deb', 'https://github.com/Al-Kodach/Citas/releases/download/v2.4.1/citas-desktop-v2.4.1-jvm17.deb', 62.4, '7f89b4a123e980cba76541289fedcba1234567890abcdef1234567890abcdef1'),
(2, 1, 'LINUX', 'JVM_21', 'DEB', 'citas-desktop-v2.4.1-jvm21.deb', 'https://github.com/Al-Kodach/Citas/releases/download/v2.4.1/citas-desktop-v2.4.1-jvm21.deb', 68.1, '8a90c5b234f091dcb87652390afedcb234567891bcdef234567891bcdef2345'),
(3, 1, 'WINDOWS', 'X86_64', 'EXE', 'citas-desktop-v2.4.1-setup.exe', 'https://github.com/Al-Kodach/Citas/releases/download/v2.4.1/citas-desktop-v2.4.1-setup.exe', 74.8, '9b01d6c345a102edc98763401bafedc34567892cdef34567892cdef3456789a'),
(4, 2, 'ANDROID', 'ARM64', 'APK', 'citas-mobile-pos-v2.4.1.apk', 'https://github.com/Al-Kodach/Citas/releases/download/v2.4.1/citas-mobile-pos-v2.4.1.apk', 28.5, '3c19e9d81234567890abcdef1234567890abcdef1234567890abcdef12345678')
ON CONFLICT (id) DO NOTHING;

SELECT setval('app_releases_id_seq', (SELECT COALESCE(MAX(id), 1) FROM app_releases));
SELECT setval('release_artifacts_id_seq', (SELECT COALESCE(MAX(id), 1) FROM release_artifacts));

CREATE TABLE IF NOT EXISTS system_settings (
    id                      VARCHAR(50) PRIMARY KEY DEFAULT 'GLOBAL_SYSTEM',
    momo_collection_number_encrypted TEXT,
    momo_collection_name    VARCHAR(255) NOT NULL,
    support_phone_number_encrypted TEXT,
    maintenance_window_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS branding (
    id                      UUID PRIMARY KEY REFERENCES workspaces(id) ON DELETE CASCADE,
    app_name                VARCHAR(225) NOT NULL,
    logo_url                TEXT DEFAULT NULL,
    palette_style           TEXT DEFAULT NULL,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hardware_registry (
    hardware_id             VARCHAR(255) PRIMARY KEY,
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    security_reason         TEXT,
    fingerprint             VARCHAR(255),
    display_name            VARCHAR(255),
    public_key              TEXT,
    model                   VARCHAR(255),
    os_name                 VARCHAR(100),
    architecture            VARCHAR(50),
    last_ip                 VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workstation_bindings (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hardware_id             VARCHAR(255) NOT NULL REFERENCES hardware_registry(hardware_id) ON DELETE CASCADE,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    workspace_name          VARCHAR(255) NOT NULL,
    display_name            VARCHAR(255),
    last_ip                 VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS marketing_content (
    id                      VARCHAR(50) PRIMARY KEY,
    content_type            VARCHAR(50) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    subtitle                VARCHAR(255),
    description             TEXT,
    media_url               VARCHAR(500),
    author_name             VARCHAR(255),
    author_title            VARCHAR(255),
    order_rank              INTEGER NOT NULL DEFAULT 0,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketing_content_type
    ON marketing_content (content_type, is_active, order_rank ASC);

-- Seed Marketing Content (Features & Testimonials) in Production Migration
INSERT INTO marketing_content (id, content_type, title, description, author_name, author_title, order_rank, is_active) VALUES
('f1', 'FEATURE', 'Pharmacy Platform', 'Complete prescription management, automated expiry alerts, and patient refill tracking.', NULL, NULL, 1, true),
('f2', 'FEATURE', 'Retail & Supermarket', 'High-speed cloud-synced POS, barcode scanning, and multi-branch inventory control.', NULL, NULL, 2, true),
('f3', 'FEATURE', 'Warehouse Logistics', 'Bulk stock movement, pallet tracking, and real-time distribution hub monitoring.', NULL, NULL, 3, true),
('f4', 'FEATURE', 'Organization Hub', 'Centralized control plane for managing multiple industrial business units and workstations.', NULL, NULL, 4, true),
('f5', 'FEATURE', 'Workstation Security', 'Cryptographic hardware signatures ensure only authorized workstations can access sensitive data.', NULL, NULL, 5, true),
('f6', 'FEATURE', 'Offline Resilience', 'Advanced local-first architecture ensures business continuity even during total internet outages.', NULL, NULL, 6, true),
('f7', 'FEATURE', 'Edge Mesh Sync', 'Multi-device local network sync. Keep your fleet synchronized even without a global internet connection.', NULL, NULL, 7, true),

('t1', 'TESTIMONIAL', 'Pharmacy Automation', 'Citas transformed our pharmacy. Tracking prescriptions and batch expiry is now automated and error-free.', 'Dr. Kwesi Appiah', 'Pharmacy Owner', 1, true),
('t2', 'TESTIMONIAL', 'Retail POS Excellence', 'The POS is incredibly intuitive. Our multi-branch retail sync works flawlessly even during peak hours.', 'Sarah Mensah', 'Retail Operations Director', 2, true),
('t3', 'TESTIMONIAL', 'Warehouse Logistics Support', 'The offline capability is a lifesaver for our distribution hubs. We never lose a single stock movement.', 'Michael Owusu', 'Logistics & Warehouse Manager', 3, true),
('t4', 'TESTIMONIAL', 'Multi-Business Visibility', 'The Organization Hub gives me total visibility across all our business units. It''s the control plane we always needed.', 'Linda Akoto', 'Industrial Group CEO', 4, true),
('t5', 'TESTIMONIAL', 'Local Mesh Sync Power', 'The local mesh sync is incredible. When I update stock at the warehouse, it reflects instantly on all POS terminals over our local network. It saves us huge amounts of data costs and works even when our fiber is down.', 'Kwame Boateng', 'Supermarket Supervisor', 5, true)
ON CONFLICT (id) DO NOTHING;