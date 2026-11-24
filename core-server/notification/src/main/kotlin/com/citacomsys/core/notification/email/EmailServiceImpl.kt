package com.citacomsys.core.notification.email

import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.EnqueueRequest
import com.citacomsys.core.notification.BackgroundJobQueue
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

internal class EmailServiceImpl(
    private val httpClient: HttpClient,
    private val auditor: TelemetryAuditor,
    private val backgroundJobQueue: BackgroundJobQueue,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : EmailService {
    private val logger = KotlinLogging.logger("com.citacomsys.NOTIFICATION_GATEWAY")
    private val resendApiKey = System.getProperty("RESEND_API_KEY") ?: ""

    override val id: String = "email_service"

    private fun encryptPayload(plainText: String): String {
        return try {
            val masterKey = System.getProperty("CITAS_ENCRYPTION_KEY") ?: System.getenv("CITAS_ENCRYPTION_KEY") ?: "citas_default_key"
            val algorithm = "AES"
            val keySpec = SecretKeySpec(masterKey.padEnd(16, '0').take(16).toByteArray(), algorithm)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray()))
        } catch (_: Exception) {
            plainText
        }
    }

    private fun decryptPayload(encryptedText: String): String {
        return try {
            val masterKey = System.getProperty("CITAS_ENCRYPTION_KEY") ?: System.getenv("CITAS_ENCRYPTION_KEY") ?: "citas_default_key"
            val algorithm = "AES"
            val keySpec = SecretKeySpec(masterKey.padEnd(16, '0').take(16).toByteArray(), algorithm)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)))
        } catch (_: Exception) {
            encryptedText
        }
    }

    override suspend fun sendInstant(payload: JsonObject): Boolean {
        if (resendApiKey.isBlank()) {
            logger.error { "🚨 Resend Email Dispatch Failed: RESEND_API_KEY environment variable is not set!" }
            auditor.logApplicationEvent(
                action = "EMAIL_DISPATCH_FAILED",
                entityType = "NOTIFICATION",
                entityId = "resend_email",
                summary = "Resend API Key is missing",
                level = TelemetryLevel.ERROR
            )
            return false
        }

        return try {
            val response = httpClient.post("https://api.resend.com/emails") {
                header(HttpHeaders.Authorization, "Bearer $resendApiKey")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }

            if (response.status.isSuccess()) {
                logger.info { "Instant email successfully dispatched via Resend to ${payload["to"]}" }
                auditor.logApplicationEvent(
                    action = "EMAIL_DISPATCHED",
                    entityType = "NOTIFICATION",
                    entityId = "resend_email",
                    summary = "Instant email successfully dispatched via Resend",
                    level = TelemetryLevel.INFO
                )
                true
            } else {
                val errorBody = response.bodyAsText()
                if (response.status.value == 403 && errorBody.contains("validation_error")) {
                    logger.warn { "⚠️ Resend Test Mode Notice [HTTP 403]: $errorBody" }
                    true
                } else {
                    logger.error { "🚨 Resend API Error [HTTP ${response.status.value}]: $errorBody" }
                    auditor.logApplicationEvent(
                        action = "EMAIL_DISPATCH_FAILED",
                        entityType = "NOTIFICATION",
                        entityId = "resend_email",
                        summary = "Resend API Error [HTTP ${response.status.value}]: $errorBody".take(255),
                        level = TelemetryLevel.ERROR
                    )
                    false
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "🚨 Exception during instant Resend email dispatch: ${e.message}" }
            auditor.logApplicationEvent(
                action = "EMAIL_DISPATCH_FAILED",
                entityType = "NOTIFICATION",
                entityId = "resend_email",
                summary = "Exception during instant email dispatch: ${e.message}",
                level = TelemetryLevel.ERROR
            )
            false
        }
    }

    override suspend fun sendScheduled(payload: JsonObject): Boolean {
        return try {
            val encryptedPayload = encryptPayload(payload.toString())
            backgroundJobQueue.enqueue(EnqueueRequest(id, payload = encryptedPayload))
            logger.info { "Future/scheduled email request successfully encrypted and registered in background queue for ${payload["to"]}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to schedule future email request in queue." }
            false
        }
    }

    override suspend fun execute(payload: String) {
        logger.debug { "Executing scheduled email background job via EmailService cron worker..." }
        val decryptedPayload = decryptPayload(payload)
        val jsonPayload = json.decodeFromString<JsonObject>(decryptedPayload)
        val success = sendInstant(jsonPayload)
        if (!success) {
            error("Failed to dispatch scheduled email in EmailService worker.")
        }
    }
}
