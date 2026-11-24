package com.citacomsys.core.persistence.table.inventory

import com.citacomsys.types.ActorType
import com.citacomsys.types.ReferenceType
import com.citacomsys.types.StockMovementType
import org.jetbrains.exposed.v1.core.Table

object StockMovementsTable : Table("stock_movements") {

    val id = varchar("id", 64)

    val workspaceId = uuid("workspace_id").index()
    val branchId = uuid("branch_id").index()
    val productId = uuid("product_id").references(BaseProductsTable.id)
    val batchId = uuid("batch_id").references(StockBatchesTable.id)
    val batchNumber = varchar("batch_number", 64)

    val type = enumerationByName("type", 16, StockMovementType::class)
    val quantity = integer("quantity")
    val referenceId = varchar("reference_id", 64).nullable()
    val referenceType = enumerationByName(
        "reference_type",
        16,
        ReferenceType::class
    ).nullable()
    val actorType = enumerationByName("actor_type", 16, ActorType::class)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, workspaceId)
        index(false, productId)
        index(false, batchId)
        index(false, referenceId)
    }
}