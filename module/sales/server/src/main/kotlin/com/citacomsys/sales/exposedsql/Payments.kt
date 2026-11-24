package com.citacomsys.sales.exposedsql

import com.citacomsys.api.sales.PaymentMethod
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object Payments : Table("payments") {

    val id = uuid("id")

    val saleId = uuid("sale_id").references(SalesTable.id)

    val amount = long("amount")
    val currencyCode = varchar("currency_code", 3)
    val method = enumerationByName("method", 16, PaymentMethod::class)

    val reference = varchar("reference", 128).nullable()

    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}