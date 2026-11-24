package com.citacomsys.pharmacy.exposedsql

import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import com.citacomsys.types.ProductType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

object PharmacyProductViewTable : Table("view_pharmacy_full_product") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id")
    val name = varchar("name", 255)
    val brandName = varchar("brand_name", 255)
    val genericName = varchar("generic_name", 255).nullable()
    val sku = varchar("sku", 100).nullable()
    val barcode = varchar("barcode", 100).nullable()
    val unitOfMeasure = varchar("unit_of_measure", 50)
    val category = varchar("category", 100).nullable()
    val reorderLevel = integer("reorder_level")
    val originApp = enumerationByName<ProductType>("product_type", 50)
    val description = text("description").nullable()
    val isActive = bool("is_active")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    // Extension fields
    val dosageForm = varchar("dosage_form", 100).nullable()
    val strength = varchar("strength", 100).nullable()
    val requiresPrescription = bool("requires_prescription")
    val therapeuticClass = varchar("therapeutic_class", 100).nullable()
    val fdaNumber = varchar("fda_number", 100).nullable()

    // Aggregate Snapshot fields
    val totalAvailable = integer("total_available")
    val currentPriceAmount = long("current_price_amount").nullable()
    val currentPriceCurrency = varchar("current_price_currency", 3).nullable()
    val nextExpiry = date("next_expiry").nullable()
    val versionTimestamp = timestamp("version_timestamp")
}

object PharmacyProductsTable : Table("pharmacy_products") {
    val productId = reference("product_id", BaseProductsTable.id)
    val workspaceId = uuid("workspace_id")

    val dosageForm = varchar("dosage_form", 100).nullable()
    val genericName = varchar("generic_name", 100).nullable()
    val strength = varchar("strength", 100).nullable()
    val requiresPrescription = bool("requires_prescription").default(false)
    val therapeuticClass = varchar("therapeutic_class", 100).nullable()
    val fdaNumber = varchar("fda_number", 100).nullable()

    override val primaryKey = PrimaryKey(productId)
}
