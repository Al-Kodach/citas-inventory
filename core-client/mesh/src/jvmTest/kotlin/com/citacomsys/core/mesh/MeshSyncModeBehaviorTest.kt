package com.citacomsys.core.mesh

import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.gateway.LocalMeshRelayPlugin
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MeshSyncModeBehaviorTest {

    private class ConfigurablePreferencesRepository(
        initialMode: String,
        initialHubIp: String? = "192.168.1.100"
    ) : SyncPreferencesRepository {
        override val meshSyncMode: StateFlow<String> = MutableStateFlow(initialMode)
        override val isMotherHubEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val syncConfig: StateFlow<SyncConfig> = MutableStateFlow(SyncConfig())
        override val standaloneSyncConfig: StateFlow<SyncConfig> = MutableStateFlow(SyncConfig())
        override val hubSyncConfig: StateFlow<SyncConfig> = MutableStateFlow(SyncConfig())

        override val activeHubIp: StateFlow<String?> = MutableStateFlow(initialHubIp)
        override val activeHubPort: StateFlow<Int> = MutableStateFlow(8443)
        override val activeHubOrgId: StateFlow<String?> = MutableStateFlow("org-1")
        override val activeHubOrgToken: StateFlow<String?> = MutableStateFlow("token")
        override val activeHubTlsFingerprint: StateFlow<String?> = MutableStateFlow("fp")

        override fun saveLastSyncMetadata(metadata: LastSyncMetadata) {}
        override fun observeLastSyncMetadata() = flowOf(null)
        override fun setLastEventSeq(seq: Long) {}
        override fun getLastEventSeq(): Long = 0L

        override fun setMeshSyncMode(mode: String) {}
        override fun setMotherHubEnabled(enabled: Boolean) {}
        override fun setSyncConfig(config: SyncConfig) {}
        override fun setStandaloneSyncConfig(config: SyncConfig) {}
        override fun setHubSyncConfig(config: SyncConfig) {}
        override fun setActiveHubInfo(ip: String?, port: Int, orgId: String?, token: String?, tlsFingerprint: String?) {}
    }

    @Test
    fun testCloudOnlyBypassesLanHub() = runTest {
        val prefs = ConfigurablePreferencesRepository("CLOUD_ONLY", initialHubIp = "192.168.1.100")
        val mockEngine = MockEngine { request ->
            // Verify request goes to cloud domain, not the active hub IP
            assertEquals("api.citas.com", request.url.host)
            respond("Cloud Response", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val client = HttpClient(mockEngine) {
            install(LocalMeshRelayPlugin) {
                this.preferences = prefs
            }
        }

        val response = client.get("https://api.citas.com/v1/data")
        assertEquals("Cloud Response", response.bodyAsText())
    }

    @Test
    fun testHybridMeshRedirectsToHub() = runTest {
        val prefs = ConfigurablePreferencesRepository("HYBRID_MESH", initialHubIp = "192.168.1.100")
        val mockEngine = MockEngine { request ->
            // Verify request is redirected to active hub IP
            assertEquals("192.168.1.100", request.url.host)
            assertEquals(8443, request.url.port)
            respond("Hub Response", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val client = HttpClient(mockEngine) {
            install(LocalMeshRelayPlugin) {
                this.preferences = prefs
            }
        }

        val response = client.get("https://api.citas.com/v1/data")
        assertEquals("Hub Response", response.bodyAsText())
    }
}
