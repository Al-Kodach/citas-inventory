package com.citacomsys.auth.infra.gateway

import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.auth.service.LoginService
import com.citacomsys.auth.usecase.ChangePasswordUseCase
import com.citacomsys.auth.usecase.GenerateActUseCase
import com.citacomsys.auth.usecase.RefreshUserTokenUseCase
import com.citacomsys.auth.usecase.ResetPasswordUseCase
import com.citacomsys.auth.usecase.MfaVerificationUseCase
import com.citacomsys.test.TestDataFactory
import com.citacomsys.test.server.BaseRouteTest
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.LoginRequest
import com.citacomsys.types.RemoteAuthResult
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import kotlin.test.assertEquals

class AuthRouteTest : BaseRouteTest() {

    private val loginService = mockk<LoginService>()

    private val authModule = module {
        single { loginService }
        single { mockk<RefreshUserTokenUseCase>(relaxed = true) }
        single { mockk<SessionService>(relaxed = true) }
        single { mockk<GenerateActUseCase>(relaxed = true) }
        single { mockk<MfaVerificationUseCase>(relaxed = true) }
        single { mockk<ResetPasswordUseCase>(relaxed = true) }
        single { mockk<ChangePasswordUseCase>(relaxed = true) }
        
        single { AuthController(get(), get(), get(), get(), get(), get(), get()) }
    }

    @Test
    fun `login from workspace bound device returns success`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = authModule,
        routeBlock = { authRoute() }
    ) {
        val user = TestDataFactory.adminUser
        val mockSession = AuthenticatedSession(AuthTokens("access", "refresh"), user, emptyList())
        coEvery { loginService.authenticate(any()) } returns Result.success(RemoteAuthResult.Success(mockSession))

        val jsonClient = createJsonClient()
        val response = jsonClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("0240000000", "password"))
            header("X-Hardware-Id", "trusted-workspace-hardware-id")
            header("X-Workspace-Id", "test-workspace-id")
            header("X-Workstation-Signature", "test-signature")
            header("X-Timestamp", System.currentTimeMillis().toString())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `new device hub login returns mfa required`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = authModule,
        routeBlock = { authRoute() }
    ) {
        val user = TestDataFactory.adminUser.copy(mfaEnabled = true)
        val challenge = AuthChallengeContext(userId = user.id, organizationId = user.organizationId)
        coEvery { loginService.authenticate(any()) } returns Result.success(RemoteAuthResult.MfaRequired(challenge))

        val jsonClient = createJsonClient()
        val response = jsonClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("0240000000", "password"))
            header("X-Hardware-Id", "new-unrecognized-device-id")
            header("X-Organization-Id", user.organizationId)
            header("X-Workstation-Signature", "test-signature")
            header("X-Timestamp", System.currentTimeMillis().toString())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `trusted hub device login bypasses mfa and returns success`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = authModule,
        routeBlock = { authRoute() }
    ) {
        val user = TestDataFactory.adminUser.copy(mfaEnabled = true)
        val mockSession = AuthenticatedSession(AuthTokens("access", "refresh"), user, emptyList())
        coEvery { loginService.authenticate(any()) } returns Result.success(RemoteAuthResult.Success(mockSession))

        val jsonClient = createJsonClient()
        val response = jsonClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("0240000000", "password"))
            header("X-Hardware-Id", "recognized-trusted-hub-device-id")
            header("X-Organization-Id", user.organizationId)
            header("X-Fleet-Auth", "valid-fleet-token")
            header("X-Workstation-Signature", "test-signature")
            header("X-Timestamp", System.currentTimeMillis().toString())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
