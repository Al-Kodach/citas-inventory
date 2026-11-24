package com.citacomsys.module.device.di

import com.citacomsys.api.device.service.DeviceApiService
import com.citacomsys.module.device.data.remote.api.KtorDeviceApiService
import com.citacomsys.module.device.ui.screen.WorkstationListViewModel
import com.citacomsys.module.device.ui.screen.WorkstationSummaryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val deviceModule = module {
    single<DeviceApiService> { KtorDeviceApiService(get()) }
    viewModelOf(::WorkstationListViewModel)
    viewModel { parameters ->
        WorkstationSummaryViewModel(
            hardwareId = parameters.get(),
            apiService = get(),
            notificationService = get()
        )
    }
}
