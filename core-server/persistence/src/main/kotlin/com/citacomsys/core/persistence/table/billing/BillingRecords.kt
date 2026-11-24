package com.citacomsys.core.persistence.table.billing

import com.citacomsys.core.persistence.table.tenant.Workspaces
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object BillingRecordsTable : UUIDTable("billing_records") {
    val workspaceId = reference("workspace_id", Workspaces.id)
    val amountMinor = long("amount_minor")
    val currency = varchar("currency", 3)
    val referenceCode = varchar("reference_code", 20)
    val status = varchar("status", 20)
    val summary = varchar("summary", 100)

    val paymentRecipientName = varchar("payment_recipient_name", 255)
    val paymentTarget = varchar("payment_target", 255)
    val paymentChannel = varchar("payment_channel", 20)

    val type = varchar("type", 20)
    val reason = varchar("reason", 20)
    val origin = varchar("origin", 20)

    val quote = text("quote") // JSONB in DB
    val paymentSubmission = text("payment_submission").nullable()

    val fulfillmentAttempts = integer("fulfillment_attempts").default(0)
    val lastFulfillmentError = text("last_fulfillment_error").nullable()
    val fulfilledAt = timestamp("fulfilled_at").nullable()

    val createdBy = varchar("created_by", 255).nullable()
    val verifiedBy = varchar("verified_by", 255).nullable()

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val expiresAt = timestamp("expires_at").nullable()

    val subscriptionVersion = integer("subscription_version").nullable()
}
