package com.citacomsys.inventory.server.plugins

import com.citacomsys.feature.telemetry.server.telemetryServiceModule
import com.citacomsys.auth.infra.authServiceModule
import com.citacomsys.billing.billingModule
import com.citacomsys.core.data.di.coreModule
import com.citacomsys.core.notification.di.notificationServiceModule
import com.citacomsys.core.notification.sms.smsModuleDi
import com.citacomsys.core.persistence.persistenceModule
import com.citacomsys.core.storage.storageModule
import com.citacomsys.device.infra.deviceServiceModule
import com.citacomsys.inventory.home.server.homeModule
import com.citacomsys.core.security.service.GeoFencingService
import com.citacomsys.inventory.server.evnConfig.EnvConfig
import com.citacomsys.module.inventory.inventoryServiceModule
import com.citacomsys.feature.platform.platformModule
import com.citacomsys.organization.serverOrganizationServiceModule
import com.citacomsys.feature.user.server.userServiceModule
import com.citacomsys.kernel.server.di.kernelServerModule
import com.citacomsys.workspace.infra.workspaceServiceModule
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val logger = KotlinLogging.logger {}

fun Application.configureDependencyInjection() {
    val env = EnvConfig.get("ENV")

    logger.info { "Running application it $env" }

    val config = ConfigFactory.load("application.conf")
        .resolve()

    install(Koin) {
        printLogger()
        modules(
            coreModule,
            persistenceModule,
            billingModule,
            storageModule,
            authServiceModule,
            kernelServerModule,
            platformModule,
            serverOrganizationServiceModule,
            userServiceModule,
            telemetryServiceModule,
            notificationServiceModule,
            workspaceServiceModule,
            deviceServiceModule,
            inventoryServiceModule,
            smsModuleDi,
            homeModule,

            module {
                single {
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                }
                single { GeoFencingService() }
                single { config }
                single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
                single(named("IO")) { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
            }
        )
    }
}
