package com.citacomsys.core.sync.telemetry

import kotlin.time.Duration.Companion.minutes

object TelemetrySyncConfig {
    //  Run light authentication/access syncs every 5 minutes
    val IDENTITY_SYNC_INTERVAL = 5.minutes

    // Run heavier application change log syncs every 30 minutes to save resources
    val APPLICATION_SYNC_INTERVAL = 30.minutes

    // Max logs to pull down per atomic network batch chunk
    const val BATCH_CHUNK_LIMIT = 50L
}
