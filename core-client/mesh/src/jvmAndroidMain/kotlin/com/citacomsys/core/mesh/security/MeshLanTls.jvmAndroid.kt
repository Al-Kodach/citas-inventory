package com.citacomsys.core.mesh.security

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.service.MeshHubTrustManager
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIOEngineConfig

internal actual fun applyMeshLanTls(
    config: HttpClientConfig<*>,
    prefs: SyncPreferencesRepository
) {
    @Suppress("UNCHECKED_CAST")
    (config as HttpClientConfig<CIOEngineConfig>).engine {
        https {
            trustManager = MeshHubTrustManager {
                prefs.activeHubTlsFingerprint.value
            }
        }
    }
}
