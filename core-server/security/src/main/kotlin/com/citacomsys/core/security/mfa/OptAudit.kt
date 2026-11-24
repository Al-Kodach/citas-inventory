package com.citacomsys.core.security.mfa

import java.util.UUID
import kotlin.time.Instant

data class OptAudit(
    val id: UUID,
    val workspaceId: UUID,
    val userId: UUID,
    val requestId: String,
    val prefix: String,
    val purpose: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
    val createdAt: Instant,
)