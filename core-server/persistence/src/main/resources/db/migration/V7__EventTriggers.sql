CREATE OR REPLACE FUNCTION notify_table_changes()
RETURNS TRIGGER AS $$
DECLARE
    payload JSON;
    ent_type TEXT;
    ws_id TEXT;
    br_id TEXT;
    org_id TEXT;
    rec RECORD;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        rec := OLD;
    ELSE
        rec := NEW;
    END IF;

    ent_type := TG_TABLE_NAME;

    BEGIN
        ws_id := rec.workspace_id::TEXT;
    EXCEPTION WHEN others THEN
        ws_id := NULL;
    END;

    BEGIN
        br_id := rec.branch_id::TEXT;
    EXCEPTION WHEN others THEN
        br_id := NULL;
    END;

    BEGIN
        org_id := rec.organization_id::TEXT;
    EXCEPTION WHEN others THEN
        org_id := NULL;
    END;

    payload := json_build_object(
        'table', TG_TABLE_NAME,
        'operation', TG_OP,
        'entityType', ent_type,
        'organizationId', org_id,
        'workspaceId', ws_id,
        'branchId', br_id,
        'timestamp', CURRENT_TIMESTAMP
    );

    PERFORM pg_notify('table_changes', payload::TEXT);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION notify_security_changes()
RETURNS TRIGGER AS $$
DECLARE
    payload JSON;
    ent_type TEXT;
    ws_id TEXT;
    org_id TEXT;
    rec RECORD;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        rec := OLD;
    ELSE
        rec := NEW;
    END IF;

    ent_type := TG_TABLE_NAME;

    BEGIN
        ws_id := rec.workspace_id::TEXT;
    EXCEPTION WHEN others THEN
        ws_id := NULL;
    END;

    BEGIN
        org_id := rec.organization_id::TEXT;
    EXCEPTION WHEN others THEN
        org_id := NULL;
    END;

    payload := json_build_object(
        'table', TG_TABLE_NAME,
        'operation', TG_OP,
        'entityType', ent_type,
        'organizationId', org_id,
        'workspaceId', ws_id,
        'timestamp', CURRENT_TIMESTAMP
    );

    PERFORM pg_notify('security_event_channel', payload::TEXT);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Table Data Triggers
DROP TRIGGER IF EXISTS trg_org_notify ON organizations;
CREATE TRIGGER trg_org_notify AFTER INSERT OR UPDATE OR DELETE ON organizations FOR EACH ROW EXECUTE FUNCTION notify_table_changes();

DROP TRIGGER IF EXISTS trg_ws_notify ON workspaces;
CREATE TRIGGER trg_ws_notify AFTER INSERT OR UPDATE OR DELETE ON workspaces FOR EACH ROW EXECUTE FUNCTION notify_table_changes();

DROP TRIGGER IF EXISTS trg_branches_notify ON branches;
CREATE TRIGGER trg_branches_notify AFTER INSERT OR UPDATE OR DELETE ON branches FOR EACH ROW EXECUTE FUNCTION notify_table_changes();

DROP TRIGGER IF EXISTS trg_products_notify ON products;
CREATE TRIGGER trg_products_notify AFTER INSERT OR UPDATE OR DELETE ON products FOR EACH ROW EXECUTE FUNCTION notify_table_changes();

DROP TRIGGER IF EXISTS trg_batches_notify ON stock_batches;
CREATE TRIGGER trg_batches_notify AFTER INSERT OR UPDATE OR DELETE ON stock_batches FOR EACH ROW EXECUTE FUNCTION notify_table_changes();

-- Security Triggers
DROP TRIGGER IF EXISTS trg_sessions_security_notify ON user_sessions;
CREATE TRIGGER trg_sessions_security_notify AFTER INSERT OR UPDATE OR DELETE ON user_sessions FOR EACH ROW EXECUTE FUNCTION notify_security_changes();

DROP TRIGGER IF EXISTS trg_hardware_security_notify ON hardware_registry;
CREATE TRIGGER trg_hardware_security_notify AFTER INSERT OR UPDATE OR DELETE ON hardware_registry FOR EACH ROW EXECUTE FUNCTION notify_security_changes();

DROP TRIGGER IF EXISTS trg_workstation_bindings_notify ON workstation_bindings;
CREATE TRIGGER trg_workstation_bindings_notify AFTER INSERT OR UPDATE OR DELETE ON workstation_bindings FOR EACH ROW EXECUTE FUNCTION notify_security_changes();

DROP TRIGGER IF EXISTS trg_user_bindings_notify ON user_workspace_bindings;
CREATE TRIGGER trg_user_bindings_notify AFTER INSERT OR UPDATE OR DELETE ON user_workspace_bindings FOR EACH ROW EXECUTE FUNCTION notify_security_changes();
