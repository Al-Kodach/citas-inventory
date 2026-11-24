package com.citacomsys.feature.telemetry

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.core.remote.markAsPublic
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.feature.api.telemetry.PrivateUpdateResponse
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.shared.AppConfig
import com.citacomsys.shared.BuildVersionConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


abstract class BaseAppUpdater(
    httpClient: HttpClient,
    protected val telemetryClient: TelemetryLogClient
) : PlatformAppUpdater, BaseApi(httpClient) {

    override val currentVersion: String get() = BuildVersionConfig.APP_VERSION

    protected val _syncState = MutableStateFlow<CommonUpdateState>(CommonUpdateState.Idle)
    override val syncState: StateFlow<CommonUpdateState> = _syncState.asStateFlow()

    protected var discoveredRelease: PrivateUpdateResponse? = null
    protected var downloadedFileAbsolutePath: String? = null

    protected suspend fun trackUpdateEvent(action: String, summary: String) {
        telemetryClient.trackApplicationEvent(
            actorId = "SYSTEM",
            entityType = "APP_UPDATE",
            entityId = "LOCAL_APP",
            action = action,
            summary = summary
        )
    }

    override suspend fun checkForUpdates(): Result<PrivateUpdateResponse?> = runCatching {
        _syncState.value = CommonUpdateState.Checking
        trackUpdateEvent("CHECK", "Checking for system updates on server.")

        val response = client.get("/api/v1/telemetry/app-updates/latest") {
            markAsPublic()
        }

        if (response.status.value == 204) return@runCatching null

        val result: PrivateUpdateResponse = response.body()

        // Clean version comparison rule (e.g., 1.4.2 vs 1.4.1)
        if (isNewerVersion(result.version, currentVersion)) {
            discoveredRelease = result
            _syncState.value = CommonUpdateState.UpdateAvailable(result)
            trackUpdateEvent("UPDATE_AVAILABLE", "New version detected: v${result.version}")
            result
        } else {
            _syncState.value = CommonUpdateState.UpToDate
            trackUpdateEvent("UP_TO_DATE", "Current version v$currentVersion is already latest.")
            null
        }
    }.onFailure { error ->
        _syncState.value =
            CommonUpdateState.Failed(error.message ?: "Network handshake verification timeout")
        trackUpdateEvent("FAILED", "Update check failed: ${error.message}")
    }

    private fun isNewerVersion(server: String, local: String): Boolean {
        val serverParts = server.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until minOf(serverParts.size, localParts.size)) {
            if (serverParts[i] > localParts[i]) return true
            if (serverParts[i] < localParts[i]) return false
        }
        return serverParts.size > localParts.size
    }
}
