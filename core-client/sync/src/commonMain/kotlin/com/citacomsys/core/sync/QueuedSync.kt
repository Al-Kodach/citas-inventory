package com.citacomsys.core.sync

import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.EntityType

internal data class QueuedSync(
    val entityType: EntityType,
    val priority: SyncPriority,
    val urgencyScore: Double = 0.0
) : Comparable<QueuedSync> {
    override fun compareTo(other: QueuedSync): Int {
        // Compare by urgency score (Higher score = higher priority)
        val scoreComparison = other.urgencyScore.compareTo(this.urgencyScore)
        if (scoreComparison != 0) return scoreComparison

        // Fallback to static enum priority
        return this.priority.ordinal.compareTo(other.priority.ordinal)
    }
}
