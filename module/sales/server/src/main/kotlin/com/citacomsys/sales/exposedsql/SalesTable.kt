package com.citacomsys.sales.exposedsql

import com.citacomsys.api.sales.SaleStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object SalesTable : Table("sales") {

    val id = uuid("id")
    val workspaceId = uuid("workspace_id")
    val branchId = uuid("branch_id").index()

    val totalAmount = long("total_amount")
    val priceVariance = long("price_variance")
    val currency = varchar("currency", 3)
    val status = enumerationByName("status", 16, SaleStatus::class)
    val totalItems = integer("total_items")
    val customerId = uuid("customer_id").nullable()

    val createdBy = uuid("created_by")

    val createdAt = timestamp("created_at")


    override val primaryKey = PrimaryKey(id)
}