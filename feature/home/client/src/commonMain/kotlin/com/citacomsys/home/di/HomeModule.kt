package com.citacomsys.home.di

import com.citacomsys.core.spi.StartupTask
import com.citacomsys.core.ui.search.GlobalSearchViewModel
import com.citacomsys.home.api.DashboardApi
import com.citacomsys.home.data.remote.api.KtorDashboardApi
import com.citacomsys.home.data.repository.DashboardRepository
import com.citacomsys.home.data.repository.DashboardRepositoryImpl
import com.citacomsys.home.sync.BusinessDashboardSyncHandler
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val homeFeatureModule = module {
    single<DashboardRepository> { DashboardRepositoryImpl(get(), get()) }
    single<DashboardApi> { KtorDashboardApi(get()) }
    singleOf(::BusinessDashboardSyncHandler) bind StartupTask::class
    viewModelOf(::GlobalSearchViewModel)
}