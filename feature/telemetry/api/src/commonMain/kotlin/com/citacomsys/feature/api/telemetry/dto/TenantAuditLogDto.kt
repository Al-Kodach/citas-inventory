package com.citacomsys.feature.api.telemetry.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
data class AuditTimelineEntry(
    val requestId: String,
    val logSource: String,      // "IDENTITY", "APPLICATION"
    val level: String,          // "INFO", "WARNING", "ERROR", "CRITICAL"
    val createdAt: Instant,
    val action: String,
    val summary: String,
    val userId: String?,
    val branchId: String?,
    val workspaceId: String? = null
)
