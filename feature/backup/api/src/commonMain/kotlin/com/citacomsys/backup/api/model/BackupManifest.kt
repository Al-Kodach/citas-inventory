package com.citacomsys.backup.api.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class BackupManifest(
    val backupVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Instant,
    val createdBy: String,
    val checksum: String,
    val timestamp: Instant
)