package com.citacomsys

import com.citacomsys.billing.engine.BillingMaintenanceJob
import com.citacomsys.billing.infra.gateway.route.tenantBilling
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.koin

internal val logger = mu.KotlinLogging.logger("com.citacomsys.BillingService")

fun Application.configureBillingService() {
    logger.info { "Configuring tenantBilling service..." }

    // This is now safe because the DB is guaranteed to be up
    logger.info("Starting Billing Maintenance Job")
    koin().get<BillingMaintenanceJob>().start()

    routing {
        tenantBilling()
    }
}