package com.citacomsys.feature.platform.di

import com.citacomsys.api.platform.remote.PlatformApiService
import com.citacomsys.api.platform.remote.ProductPackagePresentationService
import com.citacomsys.api.platform.remote.ProductPackageService
import com.citacomsys.api.platform.remote.PromotionCampaignService
import com.citacomsys.api.platform.remote.SmsPackageService
import com.citacomsys.feature.platform.data.remote.api.ktor.*
import com.citacomsys.feature.platform.ui.welcome.PlatformViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val platformFeatureModule = module {
    single<ProductPackageService> { KtorProductPackageService(get()) }
    single<PromotionCampaignService> { KtorPromotionCampaignService(get()) }
    single<SmsPackageService> { KtorSmsPackageService(get()) }
    single<ProductPackagePresentationService> { KtorProductPackagePresentationService(get()) }
    single<PlatformApiService> { KtorPlatformApiService(get()) }

    viewModelOf(::PlatformViewModel)
}
