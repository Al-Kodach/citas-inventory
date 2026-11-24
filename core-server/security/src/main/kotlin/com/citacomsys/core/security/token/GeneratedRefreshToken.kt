package com.citacomsys.core.security.token

import kotlin.time.Instant
import java.util.UUID

data class GeneratedRefreshToken(
    val token: String,
    val tokenId: UUID,
    val expiresAt: Instant
)