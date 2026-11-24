package com.citacomsys.sales.service

import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.types.ChangeListResponse
import kotlin.time.Instant

private val logger = mu.KotlinLogging.logger("com.citacomsys.InventoryQueryService")

internal class InventoryQueryService(
    private val productRepo: ProductRepository,
) {
    suspend fun getSnapshotChanges(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String
    ): ChangeListResponse<ProductSnapshot, String> = productRepo.getSnapshotChanges(
        since,
        organizationId,
        workspaceId,
        branchId
    )
}
