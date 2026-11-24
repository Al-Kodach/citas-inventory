package com.citacomsys.admin.api.security

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PublishReleasePayload(
    val appId: String = "citas-workstation",
    val appName: String = "Citas Industrial Workstation",
    val version: String,
    val productType: String = "INVENTORY",
    val channel: String = "STABLE",
    val changelog: String = "",
    val isMandatoryForceUpdate: Boolean = false,
    val isLatest: Boolean = true,
    val isPublished: Boolean = true,
    val binaries: List<AdminBinaryAssetInput> = emptyList()
)

@Serializable
data class AdminBinaryAssetInput(
    val fileName: String,
    val downloadUrl: String,
    val sizeInBytes: Long = 0L,
    val fileSizeMb: Double = 0.0,
    val osTarget: String, // 'WINDOWS', 'MACOS', 'ANDROID', 'LINUX'
    val platform: String = osTarget,
    val architecture: String = "UNIVERSAL",
    val packageType: String = "ZIP",
    val sha256Checksum: String? = null
)

@Serializable
data class SelectedBinaryAsset(
    val fileName: String,
    val fileSize: Long,
    val osTarget: String, // 'WINDOWS', 'MACOS', 'ANDROID', 'LINUX'

    @Transient
    val platformFile: PlatformFile? = null
)
