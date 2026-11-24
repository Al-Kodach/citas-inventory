package com.citacomsys.retail.exposedsql

import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import org.jetbrains.exposed.v1.core.Table

object RetailProductsTable : Table("retail_products") {
    val productId = reference("product_id", BaseProductsTable.id)
    val workspaceId = uuid("workspace_id")
    val tags = text("tags").nullable() // JSON array of strings
    val attributes = text("attributes").nullable() // JSON object

    override val primaryKey = PrimaryKey(productId)
}
