package com.citacomsys.feature.auth.di

import com.citacomsys.device.di.deviceServiceModule
import com.citacomsys.api.auth.service.AuthApiService
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.feature.auth.data.remote.api.ktor.KtorAuthApi
import com.citacomsys.feature.auth.data.repository.AuthServiceImpl
import com.citacomsys.feature.auth.ui.screen.AuthViewModel
import com.citacomsys.feature.auth.ui.screen.ResetPasswordViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authFeatureModule = module {
    includes(deviceServiceModule)

    singleOf(::AuthServiceImpl) {
        bind<AuthService>()
    }

    single<AuthApiService> {
        KtorAuthApi(client = get(named("authClient")))
    }

    viewModelOf(::AuthViewModel)
    viewModelOf(::ResetPasswordViewModel)
}
