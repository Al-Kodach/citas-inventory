package com.citacomsys.inventory.server

import com.citacomsys.configureBillingService
import com.citacomsys.core.notification.configureAutomatedAlertWorkers
import com.citacomsys.core.notification.di.startNotificationService
import com.citacomsys.core.persistence.configurePersistence
import com.citacomsys.core.security.configureSecurity
import com.citacomsys.inventory.admin.configureAdminApp
import com.citacomsys.inventory.server.evnConfig.EnvConfig
import com.citacomsys.inventory.server.plugins.configureDependencyInjection
import com.citacomsys.inventory.server.plugins.configureObservability
import com.citacomsys.inventory.server.plugins.configureRateLimiting
import com.citacomsys.inventory.server.plugins.configureRouting
import com.citacomsys.inventory.server.plugins.configureStatusPages
import com.citacomsys.module.inventory.gateway.configureBaseProductService
import com.citacomsys.module.server.customer.configureCustomerServiceModule
import com.citacomsys.organization.configureOrganizationService
import com.citacomsys.kernel.server.di.configureKernelServer
import com.citacomsys.pharmacy.configurePharmacyService
import com.citacomsys.retail.configureRetailService
import com.citacomsys.sales.configureSaleModule
import com.citacomsys.workspace.configureWorkspaceModule
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    EnvConfig.load()
    embeddedServer(
        Netty,
        port = 8080,
        module = { module() }
    ).start(wait = true)
}

fun Application.module() {
    // Core
    configureDependencyInjection()
    configurePersistence()
    configureSecurity()
    configureObservability()
    configureRateLimiting()
    configureStatusPages()
    configureRouting()

    // Service
    configureBillingService()
    configureOrganizationService()
    configureKernelServer()
    configureWorkspaceModule()
    configureCustomerServiceModule()
    configureSaleModule()
    configureBaseProductService()

    // Apps
    configurePharmacyService()
    configureRetailService()
    configureAdminApp()

    startNotificationService()
    configureAutomatedAlertWorkers()
}


