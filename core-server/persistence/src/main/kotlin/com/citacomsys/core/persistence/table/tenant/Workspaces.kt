package com.citacomsys.core.persistence.table.tenant

import com.citacomsys.types.Address
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.PackageTier
import com.citacomsys.types.ProductType
import com.citacomsys.types.WorkspaceStatus
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

object Workspaces : UUIDTable("workspaces") {
    val organizationId = reference("organization_id", Organizations.id)
    val name = varchar("name", 255)
    val email = varchar("email", 150).nullable()
    val deploymentMode = enumerationByName<DeploymentMode>("deployment_mode", 50)
    val address = jsonb<Address>("address", Json.Default, Address.serializer()).nullable()
    val phone = text("phone").nullable()
    val status = enumerationByName<WorkspaceStatus>("status", 20)
    val suspendedReason = text("suspended_reason").nullable()

    val productKey = varchar("product_key", 255).uniqueIndex()
    val businessCode = varchar("business_code", 255).uniqueIndex()
    val productType = enumerationByName<ProductType>("product_type", 255)

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        // business type + ID must be unique:
        uniqueIndex(organizationId, productType, name)
    }
}
