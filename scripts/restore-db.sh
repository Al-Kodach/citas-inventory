#!/usr/bin/env bash

# ==============================================================================
# CitasInventorySaaS Multi-Environment Database Restore Script
# ==============================================================================
# Usage:
#   1. Auto-detect container:
#      ./scripts/restore-db.sh <path_to_backup_file.sql.gz>
#
#   2. Explicitly specify target container (production, testing, staging):
#      ./scripts/restore-db.sh <path_to_backup_file.sql.gz> citas-postgres-prod
#      ./scripts/restore-db.sh <path_to_backup_file.sql.gz> citas-postgres-staging
# ==============================================================================

set -eo pipefail

if [ -z "$1" ]; then
    echo "Usage: $0 <path_to_backup_file.sql.gz> [target_container_name]"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "Error: Backup file '${BACKUP_FILE}' does not exist." >&2
    exit 1
fi

# Determine Target Container Name
DETECTED_CONTAINER=$(docker ps --format '{{.Names}}' 2>/dev/null | grep -i "postgres" | head -n 1 || true)
CONTAINER_NAME="${2:-${CONTAINER_NAME:-${DETECTED_CONTAINER:-citas-postgres-staging}}}"

# Extract DB user and name
DB_USER="${POSTGRES_USER:-$(docker exec "${CONTAINER_NAME}" printenv POSTGRES_USER 2>/dev/null || echo "citas_user")}"
DB_NAME="${POSTGRES_DB:-$(docker exec "${CONTAINER_NAME}" printenv POSTGRES_DB 2>/dev/null || echo "citas_db")}"

echo "⚠️  WARNING: Restoring database '${DB_NAME}' into container '${CONTAINER_NAME}'"
echo "Backup File: ${BACKUP_FILE}"
read -p "Are you sure you want to proceed? This will overwrite existing data in '${CONTAINER_NAME}'. (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Restore operation cancelled."
    exit 0
fi

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Restoring database '${DB_NAME}' in container '${CONTAINER_NAME}'..."

# Uncompress and pipe into psql inside container
if gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}"; then
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ✅ Database restored successfully into '${CONTAINER_NAME}'!"
else
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ❌ Database restore failed for container '${CONTAINER_NAME}'!" >&2
    exit 1
fi
