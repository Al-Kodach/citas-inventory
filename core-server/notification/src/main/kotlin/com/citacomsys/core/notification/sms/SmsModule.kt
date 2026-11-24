package com.citacomsys.core.notification.sms

import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import com.citacomsys.core.notification.sms.data.ExposedSmsAuditDataSource
import com.citacomsys.core.notification.sms.data.SmsAuditDataSource
import com.citacomsys.core.notification.sms.hubtel.HubtelConfig
import com.citacomsys.core.notification.sms.hubtel.HubtelSmsGateway
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.core.notification.sms.notification.SmsServiceImpl
import com.typesafe.config.Config
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val smsModuleDi = module {
    single<HubtelConfig> {
        getConfig(get())
    }

    single<SmsGateway> { HubtelSmsGateway(config = get(), httpClient = get()) }

    singleOf(::SmsServiceImpl) {
        bind<BackgroundJobHandler>()
        bind<SmsService>()
    }
    single<SmsAuditDataSource> { ExposedSmsAuditDataSource() }
}

private fun getConfig(config: Config): HubtelConfig {
    return HubtelConfig(
        clientId = config.getString("hubtel.clientId"),
        clientSecret = config.getString("hubtel.clientSecret"),
        senderId = config.getString("hubtel.senderId"),
        senderName = config.getString("hubtel.senderName")
    )
}
