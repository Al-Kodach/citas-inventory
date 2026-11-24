package com.citacomsys.feature.api.telemetry

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable



@Serializable
data class PrivateUpdateResponse(
    val version: String,
    val changelog: String,
    val publishedAtString: String,
    val isMandatoryForceUpdate: Boolean = false, // Crucial Enforcement Flag
    val binaries: List<PrivateBinaryAssetDto>
)

@Serializable
data class PrivateBinaryAssetDto(
    val fileName: String,
    val downloadUrl: String,
    val sizeInBytes: Long
)

interface PlatformAppUpdater {
    val currentVersion: String
    val syncState: Flow<CommonUpdateState>

    suspend fun checkForUpdates(): Result<PrivateUpdateResponse?>
    fun startDownloading()
    fun triggerInstallation()
}

sealed interface CommonUpdateState {
    object Idle : CommonUpdateState
    object Checking : CommonUpdateState
    data class UpdateAvailable(val release: PrivateUpdateResponse) : CommonUpdateState
    data class Downloading(val progress: Float) : CommonUpdateState
    object ReadyToInstall : CommonUpdateState
    data class Failed(val reason: String) : CommonUpdateState
    object UpToDate : CommonUpdateState
}