package com.citacomsys.auth.infra.gateway

import com.citacomsys.core.security.AuthorizationContextProvider
import com.citacomsys.core.security.token.jwt.WorkstationDevicePrincipal
import com.citacomsys.test.TestDataFactory
import com.citacomsys.test.server.BaseRouteTest
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.UserDeviceInfo
import com.citacomsys.types.WorkstationSecurityContext
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class WorkstationSignatureEndToEndTest : BaseRouteTest() {

    @Test
    fun `server workstation signature provider successfully authenticates valid signature`() {
        val hardwareId = "test-hardware-e2e"
        val workspaceId = "test-workspace-e2e"
        val timestamp = System.currentTimeMillis()

        // Generate real RSA key pair for testing
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        val publicKeyBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)

        // Sign payload matching server expectation: "$activeHardwareId:$activeWorkspaceId:$timestamp"
        val payload = "$hardwareId:$workspaceId:$timestamp"
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(kp.private)
        sig.update(payload.encodeToByteArray())
        val signatureBase64 = Base64.getEncoder().encodeToString(sig.sign())

        val authContextProvider = mockk<AuthorizationContextProvider>()
        coEvery { authContextProvider.getSecurityContext(workspaceId, hardwareId) } returns WorkstationSecurityContext(
            hardwareId = hardwareId,
            fingerprint = null,
            masterStatus = HardwareMasterStatus.ACTIVE,
            bindingStatus = DeviceStatus.ACTIVE
        )

        val testModule = module {
            single { authContextProvider }
        }

        runRouteTest(
            testUser = TestDataFactory.adminUser,
            koinModule = testModule,
            routeBlock = {
                authenticate("device-auth") {
                    get("/api/v1/secure/test") {
                        val principal = call.principal<WorkstationDevicePrincipal>()
                        call.respond(mapOf("status" to "SUCCESS", "hardwareId" to principal?.hardwareId))
                    }
                }
            }
        ) {
            val client = createJsonClient()
            val response = client.get("/api/v1/secure/test") {
                header("X-Hardware-Id", hardwareId)
                header("X-Workspace-Id", workspaceId)
                header("X-Workstation-Signature", signatureBase64)
                header("X-Timestamp", timestamp.toString())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("SUCCESS"))
        }
    }
}
