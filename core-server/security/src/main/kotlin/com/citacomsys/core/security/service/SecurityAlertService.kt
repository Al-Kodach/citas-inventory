package com.citacomsys.core.security.service

import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.inventory.api.user.UserPort
import kotlinx.coroutines.*
import mu.KotlinLogging

/**
 * Service for dispatching high-priority, real-time security alerts
 * to organization owners and platform administrators.
 */
class SecurityAlertService(
    private val notificationService: NotificationService,
    private val userRepository: UserPort
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Sends an immediate security warning (via SMS/Email) to the organization owner.
     */
    suspend fun dispatchImmediateSecurityAlert(
        organizationId: String,
        workspaceId: String,
        action: String,
        summary: String
    ) {
        val owner = userRepository.findOwner(organizationId) ?: run {
            logger.warn { "Skipped security alert for organization $organizationId: Owner not found." }
            return
        }
        
        val message = "[SECURITY ALERT] ${action}: $summary. Please review your dashboard for details."
        
        try {
            coroutineScope {
                val smsJob = async {
                    notificationService.sendManagement(
                        organizationId = organizationId,
                        workspaceId = workspaceId,
                        phone = owner.phone,
                        email = owner.email,
                        userId = owner.id,
                        message = message
                    )
                }

                val emailJob = async {
                    notificationService.sendSecurityAlert(
                        email = owner.email,
                        phone = owner.phone,
                        organizationId = organizationId,
                        workspaceId = workspaceId,
                        action = action,
                        summary = summary
                    )
                    true
                }

                val results = awaitAll(smsJob, emailJob)
                val smsSuccess = results[0]
                val emailSuccess = results[1]

                if (smsSuccess || emailSuccess) {
                    logger.info { "Successfully dispatched real-time security alert to owner of organization $organizationId (SMS: $smsSuccess, Email: $emailSuccess)" }
                } else {
                    logger.error { "All notification channels failed to send security alert for organization $organizationId" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception while dispatching security alert for organization $organizationId" }
        }
    }
}
