package com.citacomsys.feature.api.telemetry.model

import com.citacomsys.feature.api.telemetry.EndpointHostLog
import com.citacomsys.feature.api.telemetry.dto.ApplicationEventDTO
import com.citacomsys.feature.api.telemetry.dto.IdentityEventDTO
import kotlinx.serialization.Serializable

@Serializable
data class ClientLogSyncBatch(
    val deviceId: String,
    val appFlavor: String, // "admin" or "tenant_client"
    val identityLogs: List<IdentityEventDTO> = emptyList(),
    val endpointLogs: List<EndpointHostLog> = emptyList(),
    val applicationLogs: List<ApplicationEventDTO> = emptyList()
)

@Serializable
data class TelemetrySyncResponse(
    val status: String,
    val processedCount: Int
)
