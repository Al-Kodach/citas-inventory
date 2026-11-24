package com.citacomsys.feature.platform

import com.citacomsys.api.platform.repository.*
import com.citacomsys.api.platform.remote.*
import com.citacomsys.feature.platform.data.repository.*
import com.citacomsys.feature.platform.gateway.*
import com.citacomsys.feature.platform.service.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val platformModule = module {
    singleOf(::ExposedProductPackageRepository) {
        bind<ProductPackageRepository>()
        bind<PackageBillingTermRepository>()
    }
    singleOf(::ExposedPlatformContentRepository) { bind<PlatformContentRepository>() }
    singleOf(::ExposedPromotionCampaignRepository) { bind<PromotionCampaignRepository>() }

    single<ProductPackageService> {
        ProductPackageServiceImpl(
            packageRepository = get(),
            resourceUsageRepository = get(),
            packageBillingTermRepository = get()
        )
    }

    singleOf(::PromotionCampaignServiceImpl) { bind<PromotionCampaignService>() }
    singleOf(::ExposedSmsPackageRepository) { bind<SmsPackageRepository>() }
    singleOf(::PackageBillingTermServiceIml) { bind<PackageBillingTermService>() }
    singleOf(::SmsPackageServiceImpl) { bind<SmsPackageService>() }
    singleOf(::PackageProvisioningServiceImpl) { bind<PackageProvisioningService>() }

    singleOf(::PackagePresentationController)
}
