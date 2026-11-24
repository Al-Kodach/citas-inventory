package com.citacomsys.feature.api.telemetry

import kotlinx.serialization.Serializable

@Serializable
data class SecurityBypassPayload(
    val managerUserId: String,
    val currentDeviceVersion: String,
    val targetedForcedVersion: String,
    val justification: String = "Emergency local operational workaround"
)