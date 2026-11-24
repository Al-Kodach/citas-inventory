package com.citacomsys.core.database.backup

import okio.Path

data class DatabaseMetaData(
    val schemaVersion: Int,
    val currentVersion: Int
)

enum class RestoreScope {
    INVENTORY,
    CUSTOMER,
    BOTH
}

interface DatabaseBackupDriver {
    suspend fun getDatabaseMetaInfo(): DatabaseMetaData

    suspend fun exportDatabases(tempDir: Path)

    suspend fun importDatabases(tempDir: Path, scope: RestoreScope)

    suspend fun validateDatabases(tempDir: Path, scope: RestoreScope)

    suspend fun runIntegrityCheck(scope: RestoreScope)

    suspend fun runMigrations()

    suspend fun close()
}
