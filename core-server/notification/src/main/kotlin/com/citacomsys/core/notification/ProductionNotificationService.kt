package com.citacomsys.core.notification

import com.citacomsys.core.notification.email.EmailService
import com.citacomsys.core.notification.service.ExpiringBatchRecord
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.types.PhoneNumber
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import mu.KotlinLogging

internal class ProductionNotificationService(
    private val smsService: SmsService,
    private val emailService: EmailService
) : NotificationService {
    private val defaultFromAddress: String
        get() = System.getProperty("RESEND_FROM_EMAIL")
            ?: "Citacom Systems <support@citacomsys.com>"

    private val logger = KotlinLogging.logger("com.citacomsys.NOTIFICATION_GW")

    private val fraudAdvisoryHtml = """
        <div style='background-color: #fffbeb; border: 1px solid #fef3c7; padding: 12px; border-radius: 4px; margin-top: 20px; font-size: 13px; color: #b45309;'>
        <strong>🛡️ Fraud & Security Advisory:</strong> Citacom Systems staff will never ask for your password, verification codes, or PIN over phone calls or chat. If you suspect unauthorized activity, contact your system administrator immediately.
        </div>
    """.trimIndent()

    private fun wrapProfessionalEmail(
        recipientName: String?,
        bodyHtml: String,
        headerTitle: String = "Citas Industrial Platform"
    ): String {
        val greeting = if (!recipientName.isNullOrBlank()) "Hello <strong>$recipientName</strong>," else "Dear Valued Administrator,"
        return buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>")
            append("<body style='margin: 0; padding: 0; background-color: #f4f6f8; font-family: Arial, sans-serif;'>")
            append("<table role='presentation' border='0' cellpadding='0' cellspacing='0' width='100%' style='background-color: #f4f6f8; padding: 40px 0;'>")
            append("<tr><td align='center'>")
            append("<table role='presentation' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.05);'>")
            
            // Header
            append("<tr><td align='center' style='background-color: #0d9488; padding: 45px 20px 35px 20px;'>")
            append("<h1 style='color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 0.5px;'>$headerTitle</h1>")
            append("<p style='color: #ccfbf1; margin: 5px 0 0 0; font-size: 14px;'>Enterprise Inventory & Mesh Cloud</p>")
            append("</td></tr>")

            // Body Content
            append("<tr><td style='padding: 40px 30px; color: #333333; font-size: 15px; line-height: 1.6;'>")
            append("<p style='margin-top: 0; font-size: 16px;'>$greeting</p>")
            append(bodyHtml)
            append(fraudAdvisoryHtml)
            append("</td></tr>")

            // Footer Platform Info (Strictly Citacom Systems LTD)
            append("<tr><td align='center' style='background-color: #f8fafc; padding: 25px 30px; color: #64748b; font-size: 12px; border-top: 1px solid #e2e8f0;'>")
            append("<p style='margin: 0 0 5px 0;'><strong>Citacom Systems LTD</strong> • Mission-Critical Retail & Pharmacy Infrastructure</p>")
            append("<p style='margin: 0 0 5px 0;'>Accra, Ghana • Support: support@citacomsys.com</p>")
            append("<p style='margin: 15px 0 0 0; color: #94a3b8;'>© 2026 Citacom Systems LTD. All rights reserved.</p>")
            append("</td></tr>")

            append("</table></td></tr></table></body></html>")
        }
    }

    override suspend fun sendEmailDigest(
        organizationId: String,
        adminEmails: List<String>,
        alerts: List<ExpiringBatchRecord>
    ): Boolean {
        var emailSent = false
        if (adminEmails.isNotEmpty()) {
            val bodyContent = buildString {
                append("<p>The system has identified <strong>${alerts.size}</strong> inventory line items that are approaching expiry within the next 90 days. Please take immediate action to manage stock rotation or supplier returns.</p>")
                append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%; border-color: #ddd; margin-top: 15px;'>")
                append("<tr style='background-color: #f5f5f5;'><th>Product Name</th><th>Batch #</th><th>Expiry Date</th><th>Qty Left</th></tr>")

                alerts.forEach { batch ->
                    append("<tr>")
                    append("<td><strong>${batch.productName}</strong></td>")
                    append("<td>${batch.batchNumber}</td>")
                    append("<td style='color: #c62828;'>${batch.expiryDate}</td>")
                    append("<td>${batch.availableQuantity}</td>")
                    append("</tr>")
                }

                append("</table>")
                append("<p style='font-size: 12px; color: #777; margin-top: 20px;'>This is an automated operational notification generated by Citas Inventory Engine.</p>")
            }

            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { adminEmails.forEach { add(it) } }
                put("subject", "🚨 Action Required: ${alerts.size} Batches Expiring Soon")
                put("html", wrapProfessionalEmail("Administrator", bodyContent))
            }

            emailSent = emailService.sendScheduled(jsonPayload)
        }
        return emailSent
    }

    override suspend fun sendManagement(
        organizationId: String,
        workspaceId: String,
        phone: PhoneNumber?,
        email: String?,
        userId: String,
        message: String
    ): Boolean {
        phone ?: return false

        if (!email.isNullOrBlank()) {
            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { add(email) }
                put("subject", "Citas Management Notification")
                put("html", wrapProfessionalEmail("User", "<p>$message</p>"))
            }
            return emailService.sendScheduled(jsonPayload)
        } else {
            return try {
                val secureMessage = "$message\n\n[Fraud Warning: Never share verification codes with anyone.]"
                smsService.send(
                    organizationId = organizationId,
                    userId = userId,
                    workspaceId = workspaceId,
                    to = phone.value,
                    body = secureMessage
                )
                true
            } catch (e: Exception) {
                logger.error(e) { "Failed to send management SMS to ${phone.mask}" }
                false
            }
        }
    }

    override suspend fun sendGlobalDeviceLockAlert(
        email: String?,
        phone: PhoneNumber?,
        userDeviceName: String,
        reason: String,
        organizationName: String
    ) {
        var emailSent = false
        if (!email.isNullOrBlank()) {
            val bodyContent = buildString {
                append("<p>This is a critical security notification regarding <strong>$organizationName</strong>.</p>")
                append("<div style='background-color: #fef2f2; padding: 20px; border-radius: 4px; border-left: 5px solid #d32f2f; margin: 25px 0;'>")
                append("<p style='margin: 0;'><strong>Affected Device:</strong> <code style='font-size: 14px;'>$userDeviceName</code></p>")
                append("<p style='margin: 10px 0 0 0;'><strong>Security Status:</strong> <span style='color: #d32f2f; font-weight: bold;'>REVOKED & REVOKED</span></p>")
                append("<p style='margin: 10px 0 0 0;'><strong>Reason:</strong> $reason</p>")
                append("</div>")
                append("<p><strong>What this means:</strong> This device is now blocked from accessing any industrial workspace OR the Organization Hub. All active sessions on this hardware have been terminated.</p>")
                append("<p>If you believe this was done in error, please contact your System Administrator immediately.</p>")
                append("<p style='font-size: 12px; color: #999; margin-top: 40px;'>Citas Security Kernel • $organizationName Hub Enforcement</p>")
            }

            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { add(email) }
                put("subject", "🚨 CRITICAL: Hardware Locked Out - $organizationName")
                put("html", wrapProfessionalEmail("System Administrator", bodyContent))
            }

            emailSent = emailService.sendInstant(jsonPayload)
        }

        if (!emailSent) {
            val smsMessage = "Critical Alert: Device $userDeviceName locked out. Reason: $reason"
            sendManagement("", "", phone, email, "system", smsMessage)
        }
    }

    override suspend fun sendSecurityAlert(
        email: String?,
        phone: PhoneNumber?,
        organizationId: String,
        workspaceId: String?,
        action: String,
        summary: String
    ) {
        var emailSent = false
        if (!email.isNullOrBlank()) {
            val bodyContent = buildString {
                append("<p>A high-priority security event has been detected in your environment.</p>")
                append("<div style='background-color: #f8f8f8; padding: 15px; border-radius: 4px; margin: 20px 0;'>")
                append("<p style='margin: 0;'><strong>Action:</strong> $action</p>")
                append("<p style='margin: 10px 0 0 0;'><strong>Details:</strong> $summary</p>")
                append("</div>")
                append("<p>If you did not authorize this action, please log in to your dashboard immediately to investigate or contact system support.</p>")
                append("<p style='font-size: 12px; color: #777; margin-top: 30px;'>Citas Security Enforcement Engine</p>")
            }

            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { add(email) }
                put("subject", "🚨 SECURITY ALERT: $action")
                put("html", wrapProfessionalEmail("Administrator", bodyContent))
            }

            emailSent = emailService.sendInstant(jsonPayload)
        }

        if (!emailSent) {
            val smsMessage = "Security Alert: $action - $summary"
            sendManagement(organizationId, workspaceId ?: "", phone, email, "system", smsMessage)
        }
    }

    override suspend fun sendWorkspaceProvision(
        email: String?,
        phone: PhoneNumber?,
        businessName: String,
        businessCode: String,
        productKey: String,
        recoveryCode: String
    ) {
        var emailSent = false
        if (!email.isNullOrBlank()) {
            val bodyContent = buildString {
                append("<h2>Workspace Provisioned: $businessName</h2>")
                append("<p>Your workspace has been successfully provisioned on the Citas platform.</p>")
                append("<p>Product Key: <code>$productKey</code></p>")
                append("<p>Recovery Code: <code>$recoveryCode</code></p>")
            }
            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { add(email) }
                put("subject", "Workspace Provisioned - $businessName")
                put("html", wrapProfessionalEmail("Administrator", bodyContent))
            }
            emailSent = emailService.sendScheduled(jsonPayload)
        }

        if (!emailSent) {
            val smsMessage = "Workspace $businessName provisioned. Code: $businessCode, Key: $productKey"
            sendManagement("", "", phone, email, "system", smsMessage)
        }
    }

    override suspend fun sendOrganizationWelcomeEmail(
        email: String?,
        phone: PhoneNumber?,
        orgName: String,
        businessCode: String,
        ownerName: String
    ) {
        var emailSent = false
        if (!email.isNullOrBlank()) {
            val bodyContent = buildString {
                append("<h2 style='color: #0d9488;'>Your Citas Hub is Completed!</h2>")
                append("<p>Your organization <strong>$orgName</strong> has been successfully registered on the Citas Industrial Platform.</p>")
                append("<div style='background-color: #f0fdf4; padding: 15px; border-radius: 6px; margin: 20px 0; border: 1px solid #bbf7d0;'>")
                append("<p style='margin: 0; font-size: 16px;'><strong>Business Access Code:</strong> <code style='font-size: 18px; color: #0d9488;'>$businessCode</code></p>")
                append("</div>")
                append("<p>You can now log in to your Mother Hub or satellite nodes using your credentials and access code.</p>")
                append("<p style='font-size: 12px; color: #777; margin-top: 30px;'>Citas Cloud Onboarding Engine</p>")
            }

            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { add(email) }
                put("subject", "🎉 Welcome to Citas Platform - $orgName")
                put("html", wrapProfessionalEmail(ownerName, bodyContent))
            }

            emailSent = emailService.sendScheduled(jsonPayload)
        }

        if (!emailSent) {
            val smsMessage = "Welcome to $orgName! Business Code: $businessCode"
            sendManagement("", "", phone, email, ownerName, smsMessage)
        }
    }

    override suspend fun sendPreRegistrationNotice(
        email: String?,
        phone: PhoneNumber?,
        orgName: String,
        businessCode: String,
        adminName: String,
        userId: String,
        tempPassword: String
    ) {
        var emailSent = false
        if (!email.isNullOrBlank()) {
            val bodyContent = buildString {
                append("<h2>Pre-Registration Notice - $orgName</h2>")
                append("<p>We have received your registration for <strong>$orgName</strong>. Your account is currently pending verification.</p>")
                append("<p>Username: <strong>$adminName</strong></p>")
                append("<p>Temporary Password: <strong>$tempPassword</strong></p>")
                append("<p>Business Code: <strong>$businessCode</strong></p>")
            }

            val jsonPayload = buildJsonObject {
                put("from", defaultFromAddress)
                putJsonArray("to") { add(email) }
                put("subject", "Pre-Registration - $orgName")
                put("html", wrapProfessionalEmail(adminName, bodyContent))
            }

            emailSent = emailService.sendScheduled(jsonPayload)
        }

        if (!emailSent) {
            val smsMessage = "Welcome to $orgName! Username: $adminName, Temp Pass: $tempPassword, Code: $businessCode. Keep this safe."
            sendManagement(
                organizationId = "",
                workspaceId = "",
                phone = phone,
                email = email,
                userId = adminName,
                message = smsMessage
            )
        }
    }
}
