#!/usr/bin/env bash

# ==============================================================================
# CitasInventorySaaS Multi-Environment Database Automated Backup & R2 Cloud Sync Script
# ==============================================================================
# Usage:
#   1. Auto-detect running container & local backup:
#      ./scripts/backup-db.sh
#
#   2. Specify target container explicitly:
#      ./scripts/backup-db.sh citas-postgres-prod
#
#   3. Offsite Cloud Backup (Cloudflare R2 / S3):
#      Automatically uploads to Cloudflare R2 if R2_ACCESS_KEY, R2_SECRET_KEY,
#      R2_ENDPOINT, and R2_BUCKET_NAME are present in .env or system environment.
# ==============================================================================

set -eo pipefail

# 1. Load .env variables if present
if [ -f ".env" ]; then
    set -a
    # shellcheck disable=SC1090
    source <(grep -v '^#' .env | sed -E 's/^[[:space:]]*export[[:space:]]+//') 2>/dev/null || true
    set +a
fi

# 2. Determine Target Container Name
DETECTED_CONTAINER=$(docker ps --format '{{.Names}}' 2>/dev/null | grep -i "postgres" | head -n 1 || true)
CONTAINER_NAME="${1:-${CONTAINER_NAME:-${DETECTED_CONTAINER:-citas-postgres-staging}}}"

if [ -z "${CONTAINER_NAME}" ]; then
    echo "❌ Error: Could not detect any running PostgreSQL Docker container." >&2
    echo "Please specify container name: $0 <container_name>" >&2
    exit 1
fi

# 3. Extract database user and name from container
CONTAINER_PG_USER=$(docker exec "${CONTAINER_NAME}" printenv POSTGRES_USER 2>/dev/null || true)
CONTAINER_PG_DB=$(docker exec "${CONTAINER_NAME}" printenv POSTGRES_DB 2>/dev/null || true)

DB_USER="${CONTAINER_PG_USER:-${POSTGRES_USER:-citas_user}}"
DB_NAME="${CONTAINER_PG_DB:-${POSTGRES_DB:-citas_db}}"
BACKUP_DIR="${BACKUP_DIR:-./backups/db}"
RETENTION_DAYS=30

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILENAME="${DB_NAME}_backup_${TIMESTAMP}.sql.gz"
BACKUP_FILEPATH="${BACKUP_DIR}/${BACKUP_FILENAME}"

mkdir -p "${BACKUP_DIR}"

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Target Container: ${CONTAINER_NAME}"
echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Starting database backup for '${DB_NAME}' as user '${DB_USER}'..."

# 4. Run pg_dump inside target container and compress output
if docker exec "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" | gzip > "${BACKUP_FILEPATH}"; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILEPATH}" | cut -f1)
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ✅ Local Backup created: ${BACKUP_FILEPATH} (${BACKUP_SIZE})"
else
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ❌ Backup failed for container '${CONTAINER_NAME}'!" >&2
    rm -f "${BACKUP_FILEPATH}"
    exit 1
fi

# 5. Cloudflare R2 / S3 Offsite Upload Integration
BUCKET_NAME="${R2_BUCKET_NAME:-${S3_BUCKET_NAME}}"
ENDPOINT_URL="${R2_ENDPOINT:-${S3_ENDPOINT_URL}}"
AWS_ACCESS_KEY="${R2_ACCESS_KEY:-${AWS_ACCESS_KEY_ID}}"
AWS_SECRET_KEY="${R2_SECRET_KEY:-${AWS_SECRET_ACCESS_KEY}}"

if [ -n "${BUCKET_NAME}" ] && [ -n "${ENDPOINT_URL}" ] && [ -n "${AWS_ACCESS_KEY}" ] && [ -n "${AWS_SECRET_KEY}" ]; then
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ☁️ Syncing backup to Cloudflare R2 bucket '${BUCKET_NAME}'..."
    if command -v aws &> /dev/null; then
        AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY}" \
        AWS_SECRET_ACCESS_KEY="${AWS_SECRET_KEY}" \
        aws s3 cp "${BACKUP_FILEPATH}" "s3://${BUCKET_NAME}/backups/${BACKUP_FILENAME}" \
            --endpoint-url "${ENDPOINT_URL}"
        echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ✅ Cloudflare R2 Offsite Sync completed successfully!"
    else
        echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ⚠️  'aws' CLI tool not found. Install aws-cli to enable Cloudflare R2 auto-upload."
    fi
else
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ℹ️  Cloudflare R2 credentials not fully set in .env. Skipping cloud upload."
fi

# 6. Clean up local backups older than RETENTION_DAYS
echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Cleaning up backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -type f -name "*.sql.gz" -mtime +${RETENTION_DAYS} -exec rm -vf {} \;

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Backup maintenance finished."
