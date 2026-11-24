package com.citacomsys.core.notification

import com.citacomsys.types.JobStatus
import java.util.UUID
import kotlin.time.Instant

data class BackgroundJob(
    val id: UUID,
    val handlerId: String,
    val payload: String,
    val status: JobStatus,
    val attempts: Int,
    val lastAttemptAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

