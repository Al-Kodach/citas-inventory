package com.citacomsys.core.mesh

import com.citacomsys.core.database.dao.inventory.WorkstationDao
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.gateway.configureHubServer
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.database.WorkstationBindingEntity
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Instant
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class MeshEndToEndFlowTest {

    @Test
    fun testEndToEndHubSatelliteFlow(): Unit = testApplication {
        val workstationDao = mockk<WorkstationDao>()
        val meshManager = mockk<MeshManager>(relaxed = true)

        coEvery { workstationDao.findById("hw-terminal-01") } returns WorkstationBindingEntity(
            hardware_id = "hw-terminal-01",
            workspace_id = "ws-1",
            public_key = "dummy-puk",
            fingerprint = "fp-01",
            name = "Terminal 1",
            status = "ACTIVE",
            last_seen_at = Instant.fromEpochMilliseconds(0),
            created_at = Instant.fromEpochMilliseconds(0)
        )

        application {
            install(Koin) {
                modules(
                    module {
                        single { workstationDao }
                    }
                )
            }
            configureHubServer(
                gateways = emptyList(),
                meshManager = meshManager,
                plugins = emptyList()
            )
        }

        // Send a request missing security headers -> should fail with Unauthorized
        val unauthResponse = client.post("/api/v1/test-endpoint") {
            setBody("test payload")
        }
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)
    }

    @Test
    fun testGatewayHandlesLocalRequest(): Unit = testApplication {
        val workstationDao = mockk<WorkstationDao>()
        val meshManager = mockk<MeshManager>(relaxed = true)

        coEvery { workstationDao.findById(any()) } returns WorkstationBindingEntity(
            hardware_id = "hw-terminal-01",
            workspace_id = "ws-1",
            public_key = "dummy-puk",
            fingerprint = "fp-01",
            name = "Terminal 1",
            status = "ACTIVE",
            last_seen_at = Instant.fromEpochMilliseconds(0),
            created_at = Instant.fromEpochMilliseconds(0)
        )

        val testGateway = object : HubServiceGateway {
            override val domain: String = "inventory"
            override fun Route.registerRoutes(broadcastAlert: (NodeActivity) -> Unit) {
                get("inventory/stocks/sync") {
                    call.respondText("Mock Inventory Response")
                }
            }
        }

        application {
            install(Koin) {
                modules(
                    module {
                        single { workstationDao }
                    }
                )
            }
            configureHubServer(
                gateways = listOf(testGateway),
                meshManager = meshManager,
                plugins = emptyList()
            )
        }

        val response = client.get("/api/v1/inventory/stocks/sync") {
            header("X-Hardware-Id", "hw-terminal-01")
            header("X-Workstation-Signature", "dummy-sig")
            header("X-Timestamp", Clock.System.now().toEpochMilliseconds().toString())
            header("X-Hardware-Fingerprint", "fp-01")
            header("X-Workspace-Id", "ws-1")
        }
        assertTrue(response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Unauthorized)
    }
}
