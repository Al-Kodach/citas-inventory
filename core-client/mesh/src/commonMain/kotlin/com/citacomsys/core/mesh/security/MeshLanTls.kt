package com.citacomsys.core.mesh.security

import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import io.ktor.client.HttpClientConfig

/**
 * Pins the Mother Hub's TLS certificate on CIO (Android/JVM).
 * Cloud HTTPS continues to use the platform trust store.
 */
fun HttpClientConfig<*>.installMeshLanTls(
    prefs: SyncPreferencesRepository
) {
    applyMeshLanTls(this, prefs)
}

internal expect fun applyMeshLanTls(
    config: HttpClientConfig<*>,
    prefs: SyncPreferencesRepository
)
