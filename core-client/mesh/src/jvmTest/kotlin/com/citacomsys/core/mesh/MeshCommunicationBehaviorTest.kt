package com.citacomsys.core.mesh

import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.gateway.installMeshRetryPolicy
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.net.ConnectException
import kotlin.test.Test
import kotlin.test.assertEquals

class MeshCommunicationBehaviorTest {

    private class FakePreferencesRepository(
        initialMode: String = "HYBRID_MESH"
    ) : SyncPreferencesRepository {
        override val meshSyncMode: StateFlow<String> = MutableStateFlow(initialMode)
        override val isMotherHubEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val syncConfig: StateFlow<SyncConfig> = MutableStateFlow(SyncConfig())
        override val standaloneSyncConfig: StateFlow<SyncConfig> = MutableStateFlow(SyncConfig())
        override val hubSyncConfig: StateFlow<SyncConfig> = MutableStateFlow(SyncConfig())

        override val activeHubIp: StateFlow<String?> = MutableStateFlow("192.168.1.50")
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

        override fun setActiveHubInfo(
            ip: String?,
            port: Int,
            orgId: String?,
            token: String?,
            tlsFingerprint: String?
        ) {
        }
    }

    @Test
    fun testRetryPolicyFailFastUnderLocalLock(): Unit = runTest {
        val prefs = FakePreferencesRepository("LOCAL_LOCK")
        var aspectCount = 0

        val mockEngine = MockEngine { request ->
            aspectCount++
            throw ConnectException("Connection refused")
        }

        val client = HttpClient(mockEngine) {
            install(HttpRequestRetry) {
                installMeshRetryPolicy(prefs)
            }
        }

        runCatching {
            client.get("https://api.citas.com/v1/test")
        }

        // Under LOCAL_LOCK, 1 initial attempt + 3 retries = 4 total attempts before failing fast
        assertEquals(4, aspectCount)
    }
}
