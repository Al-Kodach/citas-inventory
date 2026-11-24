package com.citacomsys.core.security

import com.citacomsys.core.security.biometric.BiometricVerifier
import com.citacomsys.core.security.biometric.BiometricVerifierImpl
import com.citacomsys.core.security.data.AppReleaseRepository
import com.citacomsys.core.security.hashing.BCryptPasswordHasher
import com.citacomsys.core.security.mfa.data.ExposedOtpDataStore
import com.citacomsys.core.security.mfa.data.OtpDataStore
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.core.security.mfa.MfaServiceImpl
import com.citacomsys.core.security.mfa.OtpValidationService
import com.citacomsys.core.security.plugin.configureAuthentication
import com.citacomsys.feature.api.telemetry.TelemetryRequestContextElement
import kotlinx.coroutines.withContext
import io.ktor.server.application.ApplicationCallPipeline
import com.citacomsys.core.security.service.ActionTokenIssuer
import com.citacomsys.core.security.service.DeviceTokenIssuer
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.service.UserTokenIssuer
import com.citacomsys.core.security.mfa.OtpGeneratorService
import com.citacomsys.core.security.mfa.SecureOtpGeneratorServiceImpl
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuardImpl
import com.citacomsys.core.security.service.OrgSecurityConfigProvider
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.token.jwt.JWTTokenProvider
import com.citacomsys.core.security.token.jwt.JwtConfig
import com.citacomsys.core.security.token.jwt.JwtPrincipalMapper
import com.citacomsys.core.security.token.jwt.TokenConfig
import com.citacomsys.core.security.vault.SecretVault
import com.citacomsys.shared.security.PasswordHasher
import com.typesafe.config.Config
import io.ktor.server.application.Application
import mu.KotlinLogging
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }

private fun loadJwtConfig(config: Config): JwtConfig {
    return JwtConfig(
        issuer = config.getString("jwt.issuer"),
        audience = config.getString("jwt.audience"),

        userAccessSecret = config.getString("jwt.user_access_secret"),
        userRefreshSecret = config.getString("jwt.user_refresh_secret"),
        deviceSecret = config.getString("jwt.device_secret"),

        accessTokenDurationMinutes = config.getLong("jwt.access_token_duration_minutes"),
        refreshTokenDurationDays = config.getLong("jwt.refresh_token_duration_days"),
        deviceTokenDurationDays = config.getLong("jwt.device_token_duration_days")
    )
}

fun Application.configureSecurity() {
    logger.info { "Configuring Security..." }
    loadKoinModules(securityModule)

    logger.debug { "Configuring Jwt authentication..." }
    val accessProvider: JWTTokenProvider by inject()
    val refreshProvider: JWTTokenProvider by inject(named("refreshProvider"))
    val mapper = JwtPrincipalMapper()

    configureAuthentication(
        accessVerifier = accessProvider.verifier,
        refreshVerifier = refreshProvider.verifier,
        mapper = mapper
    )

//    this.install(TailscaleSecurity)

    intercept(ApplicationCallPipeline.Monitoring) {
        val auditContext = context.toAuditRequestContext()
        context.response.headers.append("X-Request-ID", auditContext.requestId)
        withContext(TelemetryRequestContextElement(auditContext)) {
            proceed()
        }
    }
}


private val jwtModule = module {
    single { loadJwtConfig(get<Config>()) }
    single { JwtPrincipalMapper() }
    single<BiometricVerifier> { BiometricVerifierImpl() }
    single<WorkspaceQuotaGuard> { WorkspaceQuotaGuardImpl() }

    // --- Access ---
    single {
        val config = get<JwtConfig>()
        JWTTokenProvider(
            TokenConfig(
                secret = config.userAccessSecret,
                issuer = config.issuer,
                audience = config.audience,
                durationMillis = TimeUnit.MINUTES.toMillis(config.accessTokenDurationMinutes)
            )
        )
    }

    single {
        OrgSecurityConfigProvider(get(), get())
    }

    single(named("refreshProvider")) {
        val config = get<JwtConfig>()
        JWTTokenProvider(
            TokenConfig(
                secret = config.userRefreshSecret,
                issuer = config.issuer,
                audience = config.audience,
                durationMillis = TimeUnit.DAYS.toMillis(config.refreshTokenDurationDays)
            )
        )
    }

    single(named("deviceProvider")) {
        val config = get<JwtConfig>()
        JWTTokenProvider(
            TokenConfig(
                secret = config.deviceSecret,
                issuer = config.issuer,
                audience = config.audience,
                durationMillis = TimeUnit.DAYS.toMillis(config.deviceTokenDurationDays)
            )
        )
    }
}

private val issuerModule = module {
    // Access issuers
    single {
        UserTokenIssuer(
            accessProvider = get(),
            refreshProvider = get(named("refreshProvider"))
        )
    }
    single { DeviceTokenIssuer(get(named("deviceProvider"))) }
    single<ActionTokenIssuer> { ActionTokenIssuer(provider = get()) }
}

private val otpModule = module {
    single<MfaService> {
        MfaServiceImpl(
            otpDataStore = get(),
            otpGateway = get(),
            emailService = get(),
            otpGenerator = get(),
            securityThrottler = get(),
            auditor = get()
        )
    }
    single<OtpValidationService> { OtpValidationService(get(), get()) }
    single<OtpDataStore> { ExposedOtpDataStore() }
    single<OtpGeneratorService> { SecureOtpGeneratorServiceImpl() }
}

internal val securityModule = module {
    includes(jwtModule, issuerModule, otpModule)

    single<SecurityThrottlerService> { SecurityThrottlerServiceImpl(get()) }
    single<PasswordHasher> { BCryptPasswordHasher() }
    single {
        SecretVault(
            masterKey = System.getenv("CITAS_ENCRYPTION_KEY") ?: "citas_default_key"
        )
    }
    singleOf(::AuthorizationContextProvider)
    singleOf(::AppReleaseRepository)
//    singleOf(::TailscaleService)
    singleOf(::SecurityAlertService)
}
