package com.citacomsys.core.notification.service

import com.citacomsys.types.PhoneNumber

/**
 * Organization-wide identity and security notifications.
 * Reflects the "Hub" level context.
 */
interface IdentityNotificationPort {
    suspend fun sendWorkspaceProvision(
        email: String?,
        phone: PhoneNumber?,
        businessName: String,
        businessCode: String,
        productKey: String,
        recoveryCode: String
    )

    /**
     * Sent when a full company (Organization) is registered.
     */
    suspend fun sendOrganizationWelcomeEmail(
        email: String?,
        phone: PhoneNumber?,
        orgName: String,
        businessCode: String,
        ownerName: String
    )

    /**
     * Sent when an organization is pre-registered by an admin.
     */
    suspend fun sendPreRegistrationNotice(
        email: String?,
        phone: PhoneNumber?,
        orgName: String,
        businessCode: String,
        adminName: String,
        userId: String,
        tempPassword: String
    )

    /**
     * Triggered when a device is compromised and locked at the Hub level.
     * Denies access across all industrial workspaces and the management hub.
     */
    suspend fun sendGlobalDeviceLockAlert(
        email: String?,
        phone: PhoneNumber?,
        userDeviceName: String,
        reason: String,
        organizationName: String
    )

    suspend fun sendSecurityAlert(
        email: String?,
        phone: PhoneNumber?,
        organizationId: String,
        workspaceId: String?,
        action: String,
        summary: String
    )
}

/**
 * Industrial execution and operational alerts.
 * Reflects the "Workspace" level context.
 */
interface OperationalNotificationPort {
    suspend fun sendEmailDigest(
        organizationId: String,
        adminEmails: List<String>,
        alerts: List<ExpiringBatchRecord>
    ): Boolean

    suspend fun sendManagement(
        organizationId: String,
        workspaceId: String,
        phone: PhoneNumber?,
        email: String?,
        userId: String,
        message: String
    ): Boolean
}

/**
 * Combined gateway for backward compatibility during refactoring.
 */
interface NotificationService : IdentityNotificationPort, OperationalNotificationPort

data class ExpiringBatchRecord(
    val productName: String,
    val batchNumber: String,
    val expiryDate: String,
    val daysRemaining: Int,
    val availableQuantity: Int
)
