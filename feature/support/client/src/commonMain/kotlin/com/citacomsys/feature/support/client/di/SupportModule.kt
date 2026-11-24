package com.citacomsys.feature.support.client.di

import com.citacomsys.feature.support.api.service.SupportApiService
import com.citacomsys.feature.support.client.data.remote.KtorSupportApiService
import com.citacomsys.feature.support.client.ui.SupportViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val supportFeatureModule = module {
    singleOf(::KtorSupportApiService) { bind<SupportApiService>() }
    viewModel { SupportViewModel(get(), get(), get()) }
}
