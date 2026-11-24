package com.citacomsys.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class LastSyncMetadata(
    val lastSuccessfulPullTimestamp: Instant? = null,
    val lastSuccessfulPushTimestamp: Instant? = null,
    val totalProcessedRequestsCount: Long = 0L,
    val lastSystemErrorMessage: String? = null,
    val lastFailureTimestamp: Instant? = null
)
