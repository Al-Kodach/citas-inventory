package com.citacomsys.core.notification.di

import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.InAppNotificationServiceImpl
import org.koin.dsl.module

val notificationModule = module {
    single<InAppNotificationService> { InAppNotificationServiceImpl(get()) }
}
