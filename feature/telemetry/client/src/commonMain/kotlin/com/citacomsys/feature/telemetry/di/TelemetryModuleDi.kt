package com.citacomsys.feature.telemetry.di

import com.citacomsys.feature.api.telemetry.OrganizationSecurityApiService
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.feature.api.telemetry.WorkspaceSecurityApiService
import com.citacomsys.feature.api.telemetry.dao.LocalTelemetryDao
import com.citacomsys.feature.telemetry.KmpOrganizationSecurityApiService
import com.citacomsys.feature.telemetry.KmpTelemetryLogClient
import com.citacomsys.feature.telemetry.KmpTenantSecurityApiService
import com.citacomsys.feature.telemetry.SqlDelightLocalTelemetryDao
import com.citacomsys.feature.telemetry.TelemetryDatabaseManager
import com.citacomsys.feature.telemetry.TelemetrySyncManager
import com.citacomsys.feature.telemetry.presentation.screen.ForceUpdateViewModel
import com.citacomsys.feature.telemetry.presentation.screen.LocalTelemetryViewModel
import com.citacomsys.feature.telemetry.presentation.screen.WorkspaceSecurityHubViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect fun platformTelemetryModule(): Module

val telemetryFeatureModuleDi = module {
    includes(platformTelemetryModule())
    
    singleOf(::KmpTelemetryLogClient) {
        bind<TelemetryLogClient>()
    }
    singleOf(::KmpTenantSecurityApiService) {
        bind<WorkspaceSecurityApiService>()
    }
    singleOf(::KmpOrganizationSecurityApiService) {
        bind<OrganizationSecurityApiService>()
    }
    singleOf(::TelemetryDatabaseManager)
    single<LocalTelemetryDao> {
        SqlDelightLocalTelemetryDao(get(), kotlinx.coroutines.Dispatchers.Default)
    }
    singleOf(::TelemetrySyncManager)

    viewModelOf(::LocalTelemetryViewModel)
    viewModelOf(::WorkspaceSecurityHubViewModel)
    viewModelOf(::ForceUpdateViewModel)
}
