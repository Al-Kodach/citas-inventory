package com.citacomsys.auth.infra

import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.api.auth.RefreshTokenRepository
import com.citacomsys.api.auth.UserSessionRepository
import com.citacomsys.auth.infra.repository.ExposedRefreshTokenRepository
import com.citacomsys.auth.infra.repository.ExposedUserSessionRepository
import com.citacomsys.auth.service.LoginAttemptService
import com.citacomsys.auth.infra.gateway.AuthController
import com.citacomsys.auth.service.LoginService
import com.citacomsys.auth.service.UserAuthenticationService
import com.citacomsys.auth.session.AuthenticationOrchestrator
import com.citacomsys.auth.session.RefreshTokenService
import com.citacomsys.auth.session.SessionServiceImpl
import com.citacomsys.auth.usecase.*
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authServiceModule = module {
    singleOf(::AuthController)

    singleOf(::GenerateActUseCase)
    singleOf(::RefreshUserTokenUseCase)
    singleOf(::ChangePasswordUseCase)
    singleOf(::AuthenticationOrchestrator)
    singleOf(::MfaVerificationUseCase)
    singleOf(::ResetPasswordUseCase)
    singleOf(::LoginAttemptService)
    singleOf(::UserAuthenticationService)
    singleOf(::LoginService)

    singleOf(::SessionServiceImpl) {
        binds(
            listOf(RefreshTokenService::class, SessionService::class)
        )
    }

    single<RefreshTokenRepository> { ExposedRefreshTokenRepository() }
    single<UserSessionRepository> { ExposedUserSessionRepository() }
}
