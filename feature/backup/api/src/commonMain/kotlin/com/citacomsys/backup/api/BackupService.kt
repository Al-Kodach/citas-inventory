package com.citacomsys.backup.api

enum class RestoreScope {
    INVENTORY,
    CUSTOMER,
    BOTH
}

sealed interface BackupResult {

    data class Success(
        val path: String,
        val fileSize: Long
    ) : BackupResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : BackupResult
}

sealed interface RestoreResult {

    data object Success : RestoreResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : RestoreResult
}

interface BackupService {
    suspend fun createBackup(password: String, outputPath: String): BackupResult

    suspend fun generateBackupBytes(password: String): BackupBytesResult

    suspend fun restoreBackup(
        backupPath: String,
        password: String,
        scope: RestoreScope = RestoreScope.BOTH
    ): RestoreResult
}

sealed interface BackupBytesResult {
    data class Success(val bytes: ByteArray) : BackupBytesResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success

            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    data class Failure(val message: String) : BackupBytesResult
}
