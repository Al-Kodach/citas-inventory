package com.citacomsys.core.notification.di

import com.citacomsys.core.notification.AlertEngine
import com.citacomsys.core.notification.DailyAlertOrchestrator
import com.citacomsys.core.notification.BackgroundJobCoordinator
import com.citacomsys.core.notification.BackgroundJobQueue
import com.citacomsys.core.notification.ProductionNotificationService
import com.citacomsys.core.notification.config.getConfig
import com.citacomsys.core.notification.email.EmailService
import com.citacomsys.core.notification.email.EmailServiceImpl
import com.citacomsys.core.notification.backgroundJobHandler.NoBackgroundJobHandler
import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import com.citacomsys.core.notification.otp.HubtelOtpGateway
import com.citacomsys.core.notification.otp.OtpGateway
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.notification.sse.SseEventHandlerRegistry
import com.citacomsys.core.notification.sse.SseServiceImpl
import com.citacomsys.core.notification.service.SseService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import mu.KotlinLogging
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.koin

val notificationServiceModule = module {
    single { getConfig(get()) }
    single<NotificationService> {
        ProductionNotificationService(
            smsService = get(),
            emailService = get()
        )
    }
    single { BackgroundJobQueue() }
    single {
        val handlers = getAll<BackgroundJobHandler>()
        BackgroundJobCoordinator(handlers, get())
    }
    single { SseEventHandlerRegistry(getAll()) }

    single { AlertEngine(get()) }

    single<SseService> {
        SseServiceImpl(applicationScope = get())
    }

    singleOf(::NoBackgroundJobHandler) {
        bind<BackgroundJobHandler>()
    }

    singleOf(::EmailServiceImpl) {
        bind<EmailService>()
        bind<BackgroundJobHandler>()
    }

    single<OtpGateway> { HubtelOtpGateway(config = get(), httpClient = get()) }

    single { DailyAlertOrchestrator(getAll()) }
}

fun Application.startNotificationService() {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting Notification and Background Cron Services..." }
    val coordinator = koin().get<BackgroundJobCoordinator>()
    monitor.subscribe(ApplicationStarted) {
        environment.log.info("Server is started, initializing background job coordinator...")
        coordinator.start()
    }

    monitor.subscribe(ApplicationStopping) {
        environment.log.info("Server is stopped, shutting down background job coordinator...")
        coordinator.stop()
    }
    logger.info { "Notification background services activated successfully." }
}
