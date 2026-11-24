package com.citacomsys.core.notification.sms.notification

import com.citacomsys.api.workspace.service.ResourceCheckResult
import com.citacomsys.api.workspace.service.WorkspaceResourceManager
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.notification.BackgroundJobQueue
import com.citacomsys.core.notification.NotificationException
import com.citacomsys.core.notification.backgroundJobHandler.BackgroundJobHandler
import com.citacomsys.core.notification.backgroundJobHandler.enqueue
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.core.notification.sms.NotificationCrypto
import com.citacomsys.core.notification.sms.SmsAudit
import com.citacomsys.core.notification.sms.SmsGateway
import com.citacomsys.core.notification.sms.data.SmsAuditDataSource
import com.citacomsys.types.ResourceType
import com.citacomsys.types.SmsBillMode
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger("com.citacomsys.SmsService")

internal class SmsServiceImpl(
    private val notificationEnqueuer: BackgroundJobQueue,
    private val smsGateway: SmsGateway,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        classDiscriminator = "type"
    },
    private val auditRepository: SmsAuditDataSource,
    private val resourceManager: WorkspaceResourceManager,
) : SmsService, BackgroundJobHandler {

    override val id: String = "sms_service"
    override val priority: Int = 60

    private val logger = KotlinLogging.logger {}

    override suspend fun execute(payload: String) {
        logger.debug { "Executing SMS job with payload: [PROTECTED]" }

        val state = try {
            json.decodeFromString<SmsJobState>(payload)
        } catch (e: Exception) {
            logger.error(e) { "Failed to decode structural SMS job state from payload." }
            throw e
        }

        when (state) {
            is SmsJobState.New -> handleNewSend(state)
            is SmsJobState.PendingCheck -> handleStatusCheck(state)
        }
    }

    override suspend fun send(
        userId: String,
        organizationId: String,
        workspaceId: String,
        to: String,
        body: String,
    ) {
        if (workspaceId != SystemActors.SYSTEM_ID) {
            val check = resourceManager.checkAvailability(workspaceId, ResourceType.SMS)
            if (check is ResourceCheckResult.Depleted) {
                throw IllegalStateException(check.message)
            }
        }

        val cryptoPhone = NotificationCrypto.encrypt(to)
        val cryptoBody = NotificationCrypto.encrypt(body)

        val initialState = SmsJobState.New(
            userId = userId,
            organizationId = organizationId,
            workspaceId = workspaceId,
            encryptedPhone = cryptoPhone.cipherText,
            encryptedBody = cryptoBody.cipherText,
            iv = cryptoPhone.iv
        )

        val isSecurityAlert = body.contains("Security Alert", ignoreCase = true) ||
                              body.contains("Critical Alert", ignoreCase = true) ||
                              body.contains("locked out", ignoreCase = true)
        val isOtp = body.contains("OTP", ignoreCase = true) || body.contains("code", ignoreCase = true)

        val priorityVal = when {
            isSecurityAlert -> 120
            isOtp -> 100
            else -> 60
        }
        val correlation = when {
            isSecurityAlert -> "SECURITY_ALERT"
            isOtp -> "SMS_OTP"
            else -> "SMS_TRANSACTIONAL"
        }

        enqueue(
            notificationEnqueuer,
            payload = json.encodeToString<SmsJobState>(initialState),
            priority = priorityVal,
            correlationId = correlation
        )
    }

    private suspend fun handleNewSend(state: SmsJobState.New) {
        val workspaceId = state.workspaceId
        var billMode: SmsBillMode = SmsBillMode.SYSTEM

        val plainTextPhone = NotificationCrypto.decrypt(state.encryptedPhone, state.iv)
        val plainTextBody = NotificationCrypto.decrypt(state.encryptedBody, state.iv)

        logger.info { "Starting secure SMS send job sequence..." }

        if (workspaceId != SystemActors.SYSTEM_ID) {
            val check = resourceManager.checkAvailability(workspaceId, ResourceType.SMS)
            if (check is ResourceCheckResult.Depleted) {
                logger.warn { "SMS send aborted for workspace $workspaceId: ${check.message}" }
                auditRepository.save(
                    SmsAudit(
                        userId = state.userId,
                        organizationId = state.organizationId,
                        workspaceId = workspaceId,
                        messageId = UUID.randomUUID().toString(),
                        status = "INSUFFICIENT_FUNDS",
                        billedFrom = null
                    )
                )
                return
            }
        }

        val sendResult = smsGateway.send(plainTextPhone, plainTextBody)

        if (sendResult.status == 0) {
            try {
                billMode = resourceManager.consumeSms(workspaceId)
            } catch (e: Exception) {
                logger.error(e) { "Gateway accepted SMS but resource consumption failed for workspace $workspaceId" }
            }
        }

        val status = when (sendResult.status) {
            -1 -> "NETWORK_ERROR"
            0 -> "ACCEPTED"
            1 -> "INVALID_PHONE"
            6 -> "INVALID_SENDER"
            100 -> "AUTH_ERROR"
            else -> "UNKNOWN"
        }

        logger.info { "SMS sent with status $status (Billed via: $billMode)" }

        auditRepository.save(
            SmsAudit(
                messageId = sendResult.messageId,
                userId = state.userId,
                organizationId = state.organizationId,
                workspaceId = state.workspaceId,
                status = status,
                billedFrom = billMode
            )
        )

        when (sendResult.status) {
            -1 -> {
                throw RuntimeException("Temporary failure sending SMS: ${sendResult.statusDescription}")
            }

            0 -> {
                val nextState = SmsJobState.PendingCheck(
                    userId = state.userId,
                    organizationId = state.organizationId,
                    workspaceId = state.workspaceId,
                    messageId = sendResult.messageId,
                    encryptedPhone = state.encryptedPhone,
                    encryptedBody = state.encryptedBody,
                    iv = state.iv
                )

                throw NotificationException(
                    message = "SMS accepted, checking delivery status: ${sendResult.messageId}",
                    newPayload = json.encodeToString<SmsJobState>(nextState)
                )
            }

            else -> {
                logger.error { "Hubtel permanent failure ($status): ${sendResult.statusDescription}" }
            }
        }
    }

    private suspend fun handleStatusCheck(state: SmsJobState.PendingCheck) {
        logger.info { "Checking delivery status for ${state.messageId}" }

        val statusResult = smsGateway.getStatus(state.messageId)
        val response = statusResult.getOrThrow()

        when (response.status) {
            "Delivered", "Rejected", "Failed", "Undeliverable" -> {
                logger.info { "SMS reached final state: ${response.status}" }
                auditRepository.updateStatus(state.messageId, response.status)
            }

            "Sent", "Pending" -> {
                throw NotificationException(
                    message = "Delivery still pending for ${state.messageId}",
                    newPayload = json.encodeToString<SmsJobState>(state)
                )
            }
        }
    }
}
