package com.citacomsys.core.mesh.security

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import io.ktor.client.HttpClientConfig

internal actual fun applyMeshLanTls(
    config: HttpClientConfig<*>,
    prefs: SyncPreferencesRepository
) {
    // Browser clients do not host or join the LAN mesh.
}
