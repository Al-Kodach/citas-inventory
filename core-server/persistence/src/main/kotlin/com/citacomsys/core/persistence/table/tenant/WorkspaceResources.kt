package com.citacomsys.core.persistence.table.tenant

import com.citacomsys.types.PackageTier
import com.citacomsys.types.ProductModule
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object WorkspaceResources : Table("workspace_resources") {
    val workspaceId = uuid("workspace_id").references(Workspaces.id)
    val packageId = varchar("package_id", 50)
    val packageName = varchar("package_name", 255)
    val tier = enumerationByName<PackageTier>("tier", 50)

    val maxUsers = integer("max_users")
    val maxDevice = integer("max_device")
    val maxBranches = integer("max_branches")
    val maxProductsLimit = integer("max_products_limit")
    val maxActiveBatchesLimit = integer("max_active_batches_limit")
    val retentionMonths = integer("retention_months")

    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(workspaceId)
}

object WorkspaceResourceUsageTable : Table("workspace_resource_usages") {
    val workspaceId = uuid("workspace_id").references(Workspaces.id)

    val productCountUsed = integer("product_count_used")
    val activeBatchesCount = integer("active_batches_count")
    val userCount = integer("user_count")
    val deviceCount = integer("device_count")
    val branchCount = integer("branch_count")

    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(workspaceId)
}

object WorkspaceModules : Table("workspace_modules") {
    val module = enumerationByName("module", 50, ProductModule::class)
    val workspaceId = uuid("workspace_id").references(Workspaces.id)

    override val primaryKey = PrimaryKey(module, workspaceId)
}

