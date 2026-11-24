package com.citacomsys.test.server

import com.citacomsys.core.persistence.plugin.TransactionManagerKey
import com.citacomsys.core.persistence.transaction.TransactionManager
import com.citacomsys.core.security.AuthorizationContextProvider
import com.citacomsys.core.security.plugin.UserAuthorizationContext
import com.citacomsys.core.security.plugin.WorkspaceOperationalState
import com.citacomsys.core.security.token.jwt.ActionPrincipal
import com.citacomsys.core.security.token.jwt.WorkstationDevicePrincipal
import com.citacomsys.core.security.token.jwt.TokenType
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.types.SubscriptionStatus
import com.citacomsys.types.WorkspaceStatus
import com.citacomsys.types.User
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.ProductModule
import com.citacomsys.types.WorkstationSecurityContext
import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.AfterEach
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

abstract class BaseRouteTest {

    protected val transactionManager: TransactionManager =
        MockDatabaseUtils.createMockTransactionManager()

    protected open val tables: List<Table> = emptyList()
    protected val dbManager: TestDatabaseManager by lazy { TestDatabaseManager(tables) }

    @AfterEach
    fun baseTearDown() {
        unmockkAll()
        if (tables.isNotEmpty()) {
            dbManager.clearTables()
        }
    }

    protected fun setupDatabaseSchema() {
        dbManager.setupSchema()
    }

    /**
     * Executes a route test with a fully mocked security layer.
     */
    protected fun runRouteTest(
        testUser: User,
        koinModule: Module,
        routeBlock: Route.() -> Unit,
        testBlock: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        val mockContext = UserAuthorizationContext(
            principal = UserPrincipal(
                tokenId = "token-id",
                userId = testUser.id,
                username = testUser.username,
                organizationId = testUser.organizationId,
                workspaceId = "workspace-id",
                deviceId = "device-id",
                role = testUser.role,
                sessionId = "session-id",
                tokenType = TokenType.ACCESS
            ),
            hardwareId = "hardware-id",
            user = testUser,
            workspace = WorkspaceOperationalState(
                organizationId = testUser.organizationId,
                status = WorkspaceStatus.ACTIVE,
                subscriptionStatus = SubscriptionStatus.ACTIVE,
                modules = ProductModule.entries.toSet()
            )
        )

        val mockContextProvider = mockk<AuthorizationContextProvider>()
        coEvery { mockContextProvider.getUserContext(any()) } returns mockContext
        coEvery { mockContextProvider.getUserContext(any(), any()) } returns mockContext
        coEvery { mockContextProvider.getSecurityContext(any(), any()) } returns WorkstationSecurityContext(
            hardwareId = "hardware-id",
            masterStatus = HardwareMasterStatus.ACTIVE,
            bindingStatus = mapOf("workspace-id" to DeviceStatus.ACTIVE)
        )

        application {
            install(ContentNegotiation) { json() }
            install(DoubleReceive)

            installMockPlugins(testUser)

            install(Koin) {
                modules(
                    module {
                        single { mockContextProvider }
                    },
                    koinModule
                )
            }

            routing {
                routeBlock()
            }
        }

        testBlock()
    }

    protected var mockActionPrincipal: ActionPrincipal? = null

    protected fun Application.installMockPlugins(testUser: User) {

        install(createApplicationPlugin("MockTransactional") {
            onCall { it.attributes.put(TransactionManagerKey, transactionManager) }
        })

        install(Authentication) {
            class MockConfig(name: String) : AuthenticationProvider.Config(name)
            
            // Register specific mocks for each core auth scheme
            register(object : AuthenticationProvider(MockConfig("access-auth")) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(UserPrincipal(
                        tokenId = "token-id",
                        userId = testUser.id,
                        username = testUser.username,
                        organizationId = testUser.organizationId,
                        workspaceId = "workspace-id",
                        deviceId = "device-id",
                        role = testUser.role,
                        sessionId = "session-id",
                        tokenType = TokenType.ACCESS
                    ))
                }
            })

            register(object : AuthenticationProvider(MockConfig("admin-access-auth")) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(UserPrincipal(
                        tokenId = "token-id",
                        userId = testUser.id,
                        username = testUser.username,
                        organizationId = testUser.organizationId,
                        workspaceId = "workspace-id",
                        deviceId = "device-id",
                        role = testUser.role,
                        sessionId = "session-id",
                        tokenType = TokenType.ACCESS
                    ))
                }
            })

            register(object : AuthenticationProvider(MockConfig("device-auth")) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(WorkstationDevicePrincipal(
                        hardwareId = "device-id",
                        organizationId = testUser.organizationId,
                        workspaceId = "workspace-id"
                    ))
                }
            })

            register(object : AuthenticationProvider(MockConfig("device-access-auth")) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(WorkstationDevicePrincipal(
                        hardwareId = "device-id",
                        organizationId = testUser.organizationId,
                        workspaceId = "workspace-id"
                    ))
                }
            })

            register(object : AuthenticationProvider(MockConfig("action-auth")) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(mockActionPrincipal ?: mockk<ActionPrincipal>(relaxed = true))
                }
            })

            register(object : AuthenticationProvider(MockConfig("refresh-auth")) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(mockk<UserPrincipal>(relaxed = true))
                }
            })
        }
    }

    /**
     * Helper to save a JSON-enabled client within a test block.
     */
    protected fun ApplicationTestBuilder.createJsonClient(): HttpClient {
        return createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }
}
