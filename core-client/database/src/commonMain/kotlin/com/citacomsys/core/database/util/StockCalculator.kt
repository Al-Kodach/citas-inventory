package com.citacomsys.core.database.util

import com.citacomsys.database.StockMovementEntity
import com.citacomsys.types.StockMovementType
import kotlin.time.Instant

object StockCalculator {

    fun availableQuantity(
        createdAt: Instant,
        lastSyncedAt: Instant?,
        baseQty: Long,
        movements: List<StockMovementEntity>
    ): Long {

        val pivotTime = lastSyncedAt ?: createdAt
        val deltaMovements = movements.filter { it.created_at > pivotTime }

        return deltaMovements.fold(baseQty) { acc, entity ->
            if (entity.type == StockMovementType.IN.name) {
                acc + entity.quantity
            } else {
                acc - entity.quantity
            }
        }
    }
}