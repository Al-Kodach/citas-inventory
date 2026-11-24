package com.citacomsys.core.persistence.table.packages

import org.jetbrains.exposed.v1.core.Table

object ProductPackageLimits : Table("product_package_limits") {
    val packageId = varchar("package_id", 50).references(ProductPackages.id)
    val maxUsers = integer("max_users")
    val maxDevice = integer("max_device")
    val maxBranches = integer("max_branches")
    val maxProductsLimit = integer("max_products_limit")
    val maxActiveBatchesLimit = integer("max_active_batches_limit")
    val historyRetentionMonths = integer("history_retention_months")

    override val primaryKey = PrimaryKey(packageId)
}
