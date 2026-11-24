package com.citacomsys.core.persistence.table.packages

import com.citacomsys.types.ResourceType
import org.jetbrains.exposed.v1.core.Table

object ResourceUnitPrices : Table("resource_unit_prices") {
    val resourceType = enumerationByName("resource_type", 50, ResourceType::class)
    val priceAmount = long("price_amount")
    val currencyCode = varchar("currency_code", 3).default("GHS")

    override val primaryKey = PrimaryKey(resourceType)
}
