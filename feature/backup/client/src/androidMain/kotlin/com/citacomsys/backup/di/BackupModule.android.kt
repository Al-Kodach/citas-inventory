package com.citacomsys.backup.di

import com.citacomsys.backup.api.fileUtil.BackupArchiveService
import com.citacomsys.backup.api.fileUtil.BackupCryptoService
import com.citacomsys.backup.fileUtil.BackupArchiveServiceImpl
import com.citacomsys.backup.fileUtil.BackupCryptoServiceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module {
    return module {
        single<BackupArchiveService> { BackupArchiveServiceImpl() }
        single<BackupCryptoService> { BackupCryptoServiceImpl() }
    }
}