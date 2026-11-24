CREATE TABLE IF NOT EXISTS products (
    id                      UUID PRIMARY KEY,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    sku                     VARCHAR(100),
    barcode                 VARCHAR(100),
    name                    VARCHAR(255) NOT NULL,
    category                VARCHAR(100),
    reorder_level           INTEGER NOT NULL DEFAULT 0,
    brand_name              VARCHAR(100) NOT NULL,
    description             TEXT,
    unit_of_measure         VARCHAR(50) NOT NULL DEFAULT 'Piece',
    product_type            VARCHAR(50) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    total_available         INTEGER NOT NULL DEFAULT 0,
    current_price_amount_minor BIGINT NOT NULL DEFAULT 0,
    current_price_currency  VARCHAR(3) NOT NULL DEFAULT 'GHS',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_tenant_sku ON products (workspace_id, sku) WHERE sku IS NOT NULL AND sku <> '';
CREATE INDEX IF NOT EXISTS idx_products_tenant_barcode ON products (workspace_id, barcode) WHERE barcode IS NOT NULL AND barcode <> '';
CREATE INDEX IF NOT EXISTS idx_products_workspace_updated ON products(workspace_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS stock_batches (
    id                      UUID PRIMARY KEY,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_id              UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    branch_id               UUID NOT NULL REFERENCES branches(id) ON DELETE SET NULL,
    batch_number            VARCHAR(100) NOT NULL,
    initial_quantity        INTEGER NOT NULL DEFAULT 0,
    current_quantity        INTEGER NOT NULL DEFAULT 0,
    cost_price_amount_minor BIGINT NOT NULL,
    cost_price_currency     VARCHAR(3) NOT NULL DEFAULT 'GHS',
    selling_price_amount_minor BIGINT NOT NULL,
    selling_price_currency  VARCHAR(3) NOT NULL DEFAULT 'GHS',
    expiry_date             DATE,
    is_archived             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_stock_batches_unique_entry ON stock_batches (workspace_id, product_id, branch_id, batch_number);
CREATE INDEX IF NOT EXISTS idx_stock_batches_fefo ON stock_batches(workspace_id, product_id, is_archived, expiry_date ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_stock_batches_updated ON stock_batches(workspace_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS stock_movements (
    id                      VARCHAR(64) PRIMARY KEY,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    product_id              UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    batch_id                UUID NOT NULL REFERENCES stock_batches(id) ON DELETE CASCADE,
    branch_id               UUID NOT NULL REFERENCES branches(id) ON DELETE SET NULL,
    batch_number            VARCHAR(64) NOT NULL,
    type                    VARCHAR(16) NOT NULL,
    quantity                INTEGER NOT NULL,
    reference_id            VARCHAR(64),
    reference_type          VARCHAR(16),
    actor_type              VARCHAR(16) NOT NULL,
    created_at              BIGINT NOT NULL,
    CHECK (type IN ('IN', 'OUT'))
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_batch_id ON stock_movements(batch_id, created_at DESC);

-- Pharmacy & Retail Extensions
CREATE TABLE IF NOT EXISTS pharmacy_products (
    product_id              UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id),
    dosage_form             VARCHAR(100),
    generic_name            VARCHAR(100),
    strength                VARCHAR(100),
    requires_prescription   BOOLEAN DEFAULT FALSE,
    therapeutic_class       VARCHAR(100),
    fda_number              VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_pharmacy_products_workspace ON pharmacy_products(workspace_id);

CREATE TABLE IF NOT EXISTS retail_products (
    product_id              UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    workspace_id            UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    tags                    TEXT,
    attributes              TEXT,
    updated_at              TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_retail_products_workspace ON retail_products(workspace_id);

-- Denormalization Trigger & Functions
CREATE OR REPLACE FUNCTION fn_update_product_stock_totals()
RETURNS TRIGGER AS $$
DECLARE
    target_product_id UUID;
    target_batch_id UUID;
    first_batch RECORD;
BEGIN
    IF (TG_TABLE_NAME = 'stock_batches') THEN
        IF (TG_OP = 'DELETE') THEN
            target_product_id := OLD.product_id;
            target_batch_id := OLD.id;
        ELSE
            target_product_id := NEW.product_id;
            target_batch_id := NEW.id;
        END IF;
    ELSIF (TG_TABLE_NAME = 'stock_movements') THEN
        IF (TG_OP = 'DELETE') THEN
            target_batch_id := OLD.batch_id;
            SELECT product_id INTO target_product_id FROM stock_batches WHERE id = target_batch_id;
        ELSE
            target_batch_id := NEW.batch_id;
            SELECT product_id INTO target_product_id FROM stock_batches WHERE id = target_batch_id;
        END IF;
    END IF;

    IF (target_batch_id IS NOT NULL) THEN
        UPDATE stock_batches
        SET
            current_quantity = (
                initial_quantity + COALESCE((
                    SELECT SUM(CASE WHEN sm.type = 'OUT' THEN -sm.quantity ELSE sm.quantity END)
                    FROM stock_movements sm
                    WHERE sm.batch_id = target_batch_id
                ), 0)
            ),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = target_batch_id;
    END IF;

    IF (target_product_id IS NOT NULL) THEN
        SELECT b.selling_price_amount_minor, b.selling_price_currency
        INTO first_batch
        FROM stock_batches b
        WHERE b.product_id = target_product_id AND b.is_archived = FALSE
        ORDER BY b.expiry_date ASC NULLS LAST
        LIMIT 1;

        UPDATE products
        SET
            total_available = COALESCE((
                SELECT SUM(b.current_quantity)
                FROM stock_batches b
                WHERE b.product_id = target_product_id AND b.is_archived = FALSE
            ), 0),
            current_price_amount_minor = COALESCE(first_batch.selling_price_amount_minor, 0),
            current_price_currency = COALESCE(first_batch.selling_price_currency, 'GHS'),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = target_product_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_stock_batches_update_totals ON stock_batches;
CREATE TRIGGER trg_stock_batches_update_totals
AFTER INSERT OR UPDATE OR DELETE ON stock_batches
FOR EACH ROW EXECUTE FUNCTION fn_update_product_stock_totals();

DROP TRIGGER IF EXISTS trg_stock_movements_update_totals ON stock_movements;
CREATE TRIGGER trg_stock_movements_update_totals
AFTER INSERT OR DELETE ON stock_movements
FOR EACH ROW EXECUTE FUNCTION fn_update_product_stock_totals();

-- Fast Denormalized Views
CREATE OR REPLACE VIEW view_batch_snapshot AS
SELECT
    COALESCE(b.id, p.id) AS batch_id,
    p.workspace_id AS workspace_id,
    COALESCE(b.branch_id, '00000000-0000-0000-0000-000000000000'::uuid) AS branch_id,
    CAST(COALESCE(b.batch_number, 'STUB-EMPTY') AS VARCHAR(100)) AS batch_number,
    p.id AS product_id,
    p.name AS product_name,
    p.brand_name AS brand_name,
    p.unit_of_measure AS unit_of_measure,
    COALESCE(b.expiry_date, '9999-12-31'::date) AS expiry_date,
    p.is_active AS product_is_active,
    COALESCE(b.is_archived, FALSE) AS batch_is_archived,
    COALESCE(b.current_quantity, p.total_available, 0) AS available_quantity,
    COALESCE(b.initial_quantity, 0) AS initial_quantity,
    COALESCE(b.selling_price_amount_minor, p.current_price_amount_minor, 0) AS selling_price_amount_minor,
    COALESCE(b.selling_price_currency, p.current_price_currency, 'GHS') AS selling_price_currency,
    COALESCE(b.created_at, p.created_at) AS batch_created_at,
    GREATEST(p.updated_at, COALESCE(b.updated_at, p.updated_at)) AS version_timestamp
FROM products p
LEFT JOIN stock_batches b ON b.product_id = p.id;

CREATE OR REPLACE VIEW view_product_snapshot AS
SELECT
    p.id AS id,
    p.workspace_id AS workspace_id,
    COALESCE(b.branch_id, '00000000-0000-0000-0000-000000000000'::uuid) AS branch_id,
    p.name AS name,
    p.brand_name AS brand_name,
    p.sku AS sku,
    p.barcode AS barcode,
    p.unit_of_measure AS unit_of_measure,
    p.product_type AS product_type,
    p.is_active AS is_active,
    p.reorder_level AS reorder_level,
    p.total_available AS server_total_available,
    p.current_price_amount_minor AS current_price_amount,
    p.current_price_currency AS current_price_currency,
    b.current_quantity AS batch_available,
    b.selling_price_amount_minor AS batch_price_amount,
    b.selling_price_currency AS batch_price_currency,
    b.id AS batch_id,
    b.batch_number AS batch_number,
    b.expiry_date AS expiry_date,
    GREATEST(p.updated_at, COALESCE(b.updated_at, p.updated_at)) AS version_timestamp
FROM products p
LEFT JOIN stock_batches b ON b.product_id = p.id;

CREATE OR REPLACE VIEW view_pharmacy_product_snapshot AS
SELECT
    v.*,
    pp.dosage_form,
    pp.generic_name,
    pp.strength,
    pp.requires_prescription,
    pp.therapeutic_class,
    pp.fda_number
FROM view_product_snapshot v
JOIN pharmacy_products pp ON v.id = pp.product_id;

CREATE OR REPLACE VIEW view_retail_product_snapshot AS
SELECT
    v.*,
    rp.tags,
    rp.attributes
FROM view_product_snapshot v
JOIN retail_products rp ON v.id = rp.product_id;
