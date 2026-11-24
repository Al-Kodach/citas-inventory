package com.citacomsys.core.persistence.table.inventory

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

object ViewBatchSnapshotTable: Table("view_batch_snapshot") {
    val batchId = uuid("batch_id")
    val workspaceId = uuid("workspace_id")
    val branchId = uuid("branch_id")
    val batchNumber = varchar("batch_number", 100)
    val productId = uuid("product_id")
    val productName = varchar("product_name", 225)
    val brandName = varchar("brand_name", 225)
    val unitOfMeasure = varchar("unit_of_measure", 10)
    val expiryDate = date("expiry_date").nullable()
    val availableQuantity = integer("available_quantity")
    val productIsActive = bool("product_is_active")
    val batchIsArchived = bool("batch_is_archived")
    val initialQuantity = integer("initial_quantity")
    val sellingPriceMinor = long("selling_price_amount_minor")
    val sellingPriceCurrency = varchar("selling_price_currency", 3)
    val batchCreatedAt = timestamp("batch_created_at")
    val versionTimestamp = timestamp("version_timestamp")
}