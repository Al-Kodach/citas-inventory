package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SessionItemDto(
    val sessionId: String,
    val username: String,
    val deviceName: String,
    val createdAt: Instant,
    val ipAddr: String,
    val platform: String? = null
)
