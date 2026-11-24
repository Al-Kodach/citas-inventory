package com.citacomsys.core.notification

import com.citacomsys.core.notification.service.ExpiringBatchRecord
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.types.PhoneNumber
import org.slf4j.LoggerFactory

/**
 * 💻 DEVELOPMENT LOOP GATEWAY (100% Free / Offline-Safe)
 */
internal class DevLocalNotificationService : NotificationService {
    private val logger = LoggerFactory.getLogger(DevLocalNotificationService::class.java)

    override suspend fun sendEmailDigest(
        organizationId: String,
        adminEmails: List<String>,
        alerts: List<ExpiringBatchRecord>
    ): Boolean {
        logger.info("=== [DEV FILE GATEWAY] OUTBOUND EMAIL DIGEST TRIGGERED ===")
        logger.info("Organization Target: $organizationId")
        logger.info("Destination Addrs: ${adminEmails.joinToString()}")
        return true
    }

    override suspend fun sendManagement(
        organizationId: String,
        workspaceId: String,
        phone: PhoneNumber?,
        email: String?,
        userId: String,
        message: String
    ): Boolean {
        logger.info("=== [DEV FILE GATEWAY] SMS DISPATCHED ===")
        logger.info("Organization: $organizationId | Workspace: $workspaceId")
        logger.info("Destination Addr: $phone")
        logger.info("SMS Payload: \"$message\"")
        return true
    }

    override suspend fun sendSecurityAlert(
        email: String?,
        phone: PhoneNumber?,
        organizationId: String,
        workspaceId: String?,
        action: String,
        summary: String
    ) {
        logger.info("=== [DEV FILE GATEWAY] SECURITY EMAIL DISPATCHED ===")
        logger.info("Organization: $organizationId | Workspace: $workspaceId")
        logger.info("Destination Addr: ${phone?.mask}")
        logger.info("Action Trace: $action")
    }

    override suspend fun sendWorkspaceProvision(
        email: String?,
        phone: PhoneNumber?,
        businessName: String,
        businessCode: String,
        productKey: String,
        recoveryCode: String
    ) {
        logger.info("=== [DEV FILE GATEWAY] WELCOME EMAIL DISPATCHED ===")
        logger.info("Destination Addr: $email")
        logger.info("Business: $businessName ($businessCode)")
    }

    override suspend fun sendOrganizationWelcomeEmail(
        email: String?,
        phone: PhoneNumber?,
        orgName: String,
        businessCode: String,
        ownerName: String
    ) {
        logger.info("=== [DEV FILE GATEWAY] ORG WELCOME EMAIL DISPATCHED ===")
        logger.info("Destination Addr: ${phone?.value} | Org: $orgName ($businessCode)")
        logger.info("Org: $orgName ($businessCode) | Owner: $ownerName")
    }

    override suspend fun sendPreRegistrationNotice(
        email: String?,
        phone: PhoneNumber?,
        orgName: String,
        businessCode: String,
        username: String,
        userId: String,
        tempPassword: String
    ) {
        logger.info("=== [DEV FILE GATEWAY] PRE-REGISTRATION NOTICE EMAIL DISPATCHED ===")
        logger.info("Destination Addr: $email | Org: $orgName ($businessCode)")
        logger.info("Credentials: Username = $username | Temp Password = $tempPassword")
    }

    override suspend fun sendGlobalDeviceLockAlert(
        email: String?,
        phone: PhoneNumber?,
        userDeviceName: String,
        reason: String,
        organizationName: String
    ) {
        logger.warn("=== [DEV FILE GATEWAY] GLOBAL DEVICE LOCK ALERT DISPATCHED ===")
        logger.warn("Destination: ${phone?.mask} | Device: $userDeviceName | Reason: $reason")
    }
}
