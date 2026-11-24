package com.citacomsys.core.notification

import com.citacomsys.types.ProductModule

/**
 * Interface for feature-specific automated background tasks.
 * Registered processors are executed during the daily system-wide alert cycle.
 */
interface AlertProcessor {
    /**
     * Unique identifier for the processor (usually the module ID).
     */
    val processorId: String

    /**
     * Checks if this processor should execute for the given workspace based on their active modules.
     */
    fun shouldProcess(modules: Set<ProductModule>): Boolean

    /**
     * Executes the background task logic for the specified workspace.
     */
    suspend fun process(workspaceId: String)
}
