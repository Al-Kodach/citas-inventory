package com.citacomsys.core.persistence.table.inventory

import com.citacomsys.types.ProductType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

object ProductSnapshotViewTable : Table("view_product_snapshot") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id")
    val branchId = uuid("branch_id")
    val name = varchar("name", 255)
    val brandName = varchar("brand_name", 255)
    val sku = varchar("sku", 100).nullable()
    val barcode = varchar("barcode", 100).nullable()
    val unitOfMeasure = varchar("unit_of_measure", 50)
    val originApp = enumerationByName<ProductType>("product_type", 50)
    val isActive = bool("is_active")
    val reorderLevel = integer("reorder_level")
    val totalAvailable = integer("server_total_available")
    val currentPriceAmount = long("current_price_amount").nullable()
    val currentPriceCurrency = varchar("current_price_currency", 3).nullable()
    val nextExpiry = date("next_expiry").nullable()
    val versionTimestamp = timestamp("version_timestamp")
}