package com.citacomsys.core.domain.service

/**
 * Common cross-module interface for triggering full data synchronization cycles
 * without requiring direct Gradle module dependencies on the sync engine implementation.
 */
interface DataSyncTrigger {
    suspend fun syncAll()
}
