package com.citacomsys.core.persistence.table.inventory

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

object StockBatchesTable : Table("stock_batches") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").index()
    val branchId = uuid("branch_id").index()
    val productId = uuid("product_id").references(BaseProductsTable.id)
    val batchNumber = varchar("batch_number", 100)
    val initialQuantity = integer("initial_quantity").default(0)
    val currentQuantity = integer("current_quantity").default(0)
    val costPriceAmountMinor = long("cost_price_amount_minor")
    val costPriceCurrency = varchar("cost_price_currency", 3)
    val sellingPriceAmountMinor = long("selling_price_amount_minor")
    val sellingPriceCurrency = varchar("selling_price_currency", 3)
    val expiryDate = date("expiry_date").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val isArchived = bool("is_archived").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(workspaceId, productId, batchNumber)
    }
}

