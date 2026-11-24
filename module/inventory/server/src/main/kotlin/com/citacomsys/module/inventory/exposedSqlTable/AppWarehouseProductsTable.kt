package com.citacomsys.module.inventory.exposedSqlTable

import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import org.jetbrains.exposed.v1.core.Table

object AppWarehouseProducts : Table("app_warehouse_products") {
    val productId = reference("product_id", BaseProductsTable.id)
    val workspaceId = uuid("workspace_id")

    val weightKg = double("weight_kg").nullable()
    val palletSpaceRequirement = integer("pallet_space_requirement").default(1)
    val hazardClass = varchar("hazard_class", 50).nullable()
    val isPerishable = bool("is_perishable").default(false)
    val storageTemperatureCelsius = double("storage_temperature_celsius").nullable()

    override val primaryKey = PrimaryKey(productId)
}