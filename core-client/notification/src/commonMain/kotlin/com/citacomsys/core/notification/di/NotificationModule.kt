package com.citacomsys.core.notification.di

import com.citacomsys.core.notification.AlertService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.InAppNotificationServiceImpl
import com.citacomsys.core.notification.NotificationService
import org.koin.core.module.Module
import org.koin.dsl.module

val notificationModule: Module = module {
    single<InAppNotificationService> {
        val service = InAppNotificationServiceImpl(get())
        AlertService.init(service)
        NotificationService.init(service)
        service
    }
}