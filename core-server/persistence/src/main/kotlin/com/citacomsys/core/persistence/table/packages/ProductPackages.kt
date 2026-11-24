package com.citacomsys.core.persistence.table.packages

import com.citacomsys.types.PackageTier
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object ProductPackages : Table("product_packages") {
    val id = varchar("id", 50)
    val name = varchar("name", 255)

    val packageTier = enumerationByName("package_tier", 20, PackageTier::class)
    
    /**
     * The number of SMS units allocated for the duration of the billing cycle.
     * This ensures quotas are reset correctly upon subscription changes or renewals.
     */
    val smsQuotaPerCycle = integer("sms_quota_per_cycle").default(0)
    val description = text("description").nullable()

    val recommended = bool("recommended").default(false)
    val isPublic = bool("is_public").default(true)

    val createdAt = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at")
        .defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
