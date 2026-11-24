package com.citacomsys.billing

import com.citacomsys.api.billing.BillingRepository
import com.citacomsys.billing.engine.BillingFulfillmentEngine
import com.citacomsys.billing.engine.BillingFulfillmentScheduleJobHandler
import com.citacomsys.billing.engine.BillingMaintenanceJob
import com.citacomsys.billing.engine.BillingMaintenanceJobImpl
import com.citacomsys.billing.engine.DefaultPromotionEngine
import com.citacomsys.billing.engine.PromotionEngine
import com.citacomsys.billing.engine.QuotePricingCalculator
import com.citacomsys.billing.infra.repository.ExposedBillingRepository
import com.citacomsys.billing.service.BillingQuoteService
import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.billing.service.BillingServiceImpl
import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val billingModule = module {
    single<BillingRepository> { ExposedBillingRepository() }
    singleOf(::BillingServiceImpl) bind BillingService::class
    singleOf(::BillingFulfillmentEngine)
    singleOf(::BillingQuoteService)
    singleOf(::BillingMaintenanceJobImpl) bind BillingMaintenanceJob::class
    singleOf(::BillingFulfillmentScheduleJobHandler) bind BackgroundJobHandler::class
    singleOf(::DefaultPromotionEngine) bind PromotionEngine::class
    singleOf(::QuotePricingCalculator)
}
