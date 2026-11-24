package com.citacomsys.core.security.mfa.data.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object OtpTable: UUIDTable("otps") {
    val userId = uuid("user_id")
    val providerRequestId = varchar("provider_request_id", 255)
    val purpose = varchar("purpose", 255)
    val hashedCode = varchar("hashed_code", 255)
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val cancelledAt = timestamp("cancelled_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}