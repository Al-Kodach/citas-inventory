package com.citacomsys.backup.di

import com.citacomsys.backup.api.BackupService
import com.citacomsys.backup.service.BackupServiceImpl
import com.citacomsys.backup.ui.BackupRestoreViewModel
import com.citacomsys.backup.util.BackupAssembler
import com.citacomsys.backup.util.RestoreCoordinator
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal expect fun platformModule(): Module


val backupFeatureModule = module {
    // Provide your custom platform driver configurations
    includes(platformModule())

    // Wire up your shared engines automatically
    single<BackupAssembler> {
        BackupAssembler(get(), get(), get(), get(), get(), get())
    }
    single<RestoreCoordinator> { RestoreCoordinator(get(), get(), get(), get(), get()) }
    single<BackupService> { BackupServiceImpl(get(), get()) }
    viewModelOf(::BackupRestoreViewModel)
}
