package com.citacomsys.core.persistence.table.user

import com.citacomsys.core.persistence.table.tenant.Organizations
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object Users : Table("users") {
    val id = uuid("id")
    val organizationId = reference("organization_id", Organizations.id)
    val name = varchar("name", 150)
    val username = varchar("username", 150)
    val phone = varchar("phone", 150).uniqueIndex()
    val email = varchar("email", 150).nullable().uniqueIndex()
    val isPhoneVerified = bool("is_phone_verified").default(false)
    val passwordHash = text("password_hash")
    val userType = enumerationByName("user_type", 20, UserType::class)
    // Primary base role for the User account (e.g. OPERATOR / USER)
    val role = varchar("role", 20).default("OPERATOR")
    val capabilities = text("capabilities").default("")
    val suspendedReason = text("suspended_reason").nullable().default(null)
    val forcePasswordChange = bool("force_password_change").default(false)
    val mfaEnabled = bool("mfa_enabled").default(false)
    val status = enumerationByName(
        "status",
        50,
        UserStatus::class
    ).default(UserStatus.ACTIVE)

    val deletedAt = timestamp("deleted_at").nullable().default(null)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(organizationId, username)
    }

    override val primaryKey = PrimaryKey(id)
}
