package com.citacomsys.sales.exposedsql

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date

object SaleItemsTable : Table("sale_items") {
    val id = uuid("id")

    val saleId = uuid("sale_id").references(SalesTable.id)
    val productId = uuid("product_id")
    val productName = varchar("product_name", 255)
    val batchNumber = varchar("batch_number", 50).nullable()
    val expiryDate = date("expiry_date").nullable()

    val batchId = uuid("batch_id")

    val quantity = integer("quantity")
    val soldUnitPrice = long("sold_unit_price")
    val soleUnitCurrencyCode = varchar("sold_unit_currency_code", 3)
    val priceVariance = long("price_variance")

    override val primaryKey = PrimaryKey(id)
}