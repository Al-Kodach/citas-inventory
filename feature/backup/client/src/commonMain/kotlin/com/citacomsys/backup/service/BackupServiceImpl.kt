package com.citacomsys.backup.service

import com.citacomsys.backup.api.BackupBytesResult
import com.citacomsys.backup.api.BackupResult
import com.citacomsys.backup.api.BackupService
import com.citacomsys.backup.api.RestoreResult
import com.citacomsys.backup.api.RestoreScope
import com.citacomsys.backup.util.BackupAssembler
import com.citacomsys.backup.util.RestoreCoordinator

internal class BackupServiceImpl(
    private val assembler: BackupAssembler,
    private val restoreCoordinator: RestoreCoordinator
) : BackupService {

    override suspend fun createBackup(password: String, outputPath: String): BackupResult {
        return assembler.assemble(password, outputPath)
    }

    // New bridge supporting your FileKit universal save extension
    override suspend fun generateBackupBytes(password: String): BackupBytesResult {
        return assembler.assembleToBytes(password)
    }

    override suspend fun restoreBackup(
        backupPath: String,
        password: String,
        scope: RestoreScope
    ): RestoreResult {
        return restoreCoordinator.restore(backupPath, password, scope)
    }

}
