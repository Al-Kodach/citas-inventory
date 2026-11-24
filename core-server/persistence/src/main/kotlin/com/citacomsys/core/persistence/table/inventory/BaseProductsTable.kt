package com.citacomsys.core.persistence.table.inventory

import com.citacomsys.types.ProductType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.datetime.timestamp

object BaseProductsTable : Table("products") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id")
    val name = varchar("name", 255)
    val sku = varchar("sku", 100).nullable()
    val barcode = varchar("barcode", 100).nullable()
    val category = varchar("category", 100).nullable()
    val reorderLevel = integer("reorder_level").default(0)
    val brandName = varchar("brand_name", 100)
    val description = text("description").nullable()
    val unitOfMeasure = varchar("unit_of_measure", 50)
    val productType = enumerationByName<ProductType>("product_type", 50)
    val isActive = bool("is_active").default(true)
    val totalAvailable = integer("total_available").default(0)
    val currentPriceAmountMinor = long("current_price_amount_minor").default(0L)
    val currentPriceCurrency = varchar("current_price_currency", 3).default("GHS")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(
            customIndexName = "uidx_tenant_sku_partial",
            isUnique = true,
            columns = arrayOf(workspaceId, sku),
            filterCondition = { sku.isNotNull() }
        )

        index(
            customIndexName = "uidx_tenant_barcode_partial",
            isUnique = true,
            columns = arrayOf(workspaceId, barcode),
            filterCondition = { barcode.isNotNull() }
        )
    }
}