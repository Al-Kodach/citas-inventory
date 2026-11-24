package com.citacomsys.feature.user.server

import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.test.TestDataFactory
import com.citacomsys.test.server.BaseRouteTest
import io.ktor.client.request.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import kotlin.test.assertEquals

class UserRouteTest : BaseRouteTest() {

    private val userService = mockk<UserPort>()

    private val userModule = module {
        single<UserPort> { userService }
        single { mockk<TelemetryAuditor>(relaxed = true) }
        single { UserController(get(), get()) }
    }

    @Test
    fun `find user by phone returns success`() {
        runRouteTest(
            testUser = TestDataFactory.adminUser,
            koinModule = userModule,
            routeBlock = { userRouting() }
        ) {
            val mockUser = TestDataFactory.adminUser
            coEvery { userService.findByPhone("0240000000") } returns Result.success(mockUser)

            val jsonClient = createJsonClient()
            val response = jsonClient.get("/api/v1/users/0240000000")

            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
