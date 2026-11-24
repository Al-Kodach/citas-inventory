package com.citacomsys.core.security.mfa

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.notification.email.EmailService
import com.citacomsys.core.notification.otp.OtpGateway
import com.citacomsys.core.security.mfa.data.OtpDataStore
import com.citacomsys.core.security.service.MfaAuthInfo
import com.citacomsys.core.security.service.MfaService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.security.service.SecurityTrackableEvent
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.types.MFAChannel
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.jvm.toJvm
import io.ktor.server.application.Application
import mu.KotlinLogging
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

private val logger = KotlinLogging.logger("MFA_SERVICE")

internal class MfaServiceImpl(
    private val otpDataStore: OtpDataStore,
    private val otpGateway: OtpGateway,
    private val emailService: EmailService,
    private val otpGenerator: OtpGeneratorService,
    private val securityThrottler: SecurityThrottlerService,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val auditor: TelemetryAuditor
) : MfaService {
    val isProd = System.getProperty("ENV")?.lowercase()?.startsWith("pro") == true

    private val defaultFromAddress: String
        get() = System.getProperty("RESEND_FROM_EMAIL")
            ?: "Citacom Systems <support@citacomsys.com>"

    override suspend fun authAccount(info: MfaAuthInfo): Result<MFAChannel> {
        return runCatching {
            securityThrottler.evaluateThreats(
                info.userId,
                SecurityTrackableEvent.OTP_REQUEST_RATE,
            )

            if (!info.email.isNullOrBlank()) {
                val (rawCode, hashedCode) = otpGenerator.generate()
                val expiryTime = Clock.System.now().plus(5, DateTimeUnit.MINUTE)
                val localRequestId = UUID.randomUUID().toString()

                otpDataStore.findLatestPending(info.userId, info.purpose.name)?.let {
                    otpDataStore.markAsCancelled(it.id)
                }

                otpDataStore.save(
                    userId = info.userId.toUuid(),
                    prefix = "OTP",
                    requestId = localRequestId,
                    purpose = info.purpose.name,
                    hashedCode = hashedCode,
                    expiresAt = expiryTime,
                )

                val (purposeSubject, purposeTitle, purposeContextText) = when (info.purpose) {
                    OtpPurpose.RESET_PASSWORD -> Triple(
                        "🚨 Citas Password Reset Code: $rawCode",
                        "Password Reset Request",
                        "Your one-time verification code to <strong>reset your password</strong> is:"
                    )
                    OtpPurpose.ACCOUNT_VERIFICATION -> Triple(
                        "🚨 Citas Account Activation Code: $rawCode",
                        "Account Activation",
                        "Your one-time verification code to <strong>activate your business account</strong> is:"
                    )
                    OtpPurpose.USER_VERIFICATION -> Triple(
                        "🚨 Citas Contact Verification Code: $rawCode",
                        "Contact Verification",
                        "Your one-time verification code to <strong>verify your contact details</strong> is:"
                    )
                    OtpPurpose.MFA_CHALLENGE -> Triple(
                        "🚨 Citas Login Security Code: $rawCode",
                        "Multi-Factor Login Verification",
                        "Your one-time verification code for <strong>multi-factor login authentication</strong> is:"
                    )
                    OtpPurpose.WORKSPACE_BINDING -> Triple(
                        "🚨 Citas New Device Access Code: $rawCode",
                        "New Device Fingerprint Authorization",
                        "Your one-time verification code to <strong>authorize access for a new device / fingerprint</strong> is:"
                    )
                    OtpPurpose.ENABLE_MFA -> Triple(
                        "🚨 Citas Security MFA Setup Code: $rawCode",
                        "MFA Security Setup",
                        "Your one-time verification code to <strong>enable Multi-Factor Authentication</strong> is:"
                    )
                }


                // Asynchronous background email dispatch (non-blocking for caller)
                applicationScope.launch {
                    runCatching {
                        val htmlBody = """
                            <!DOCTYPE html><html><head><meta charset='UTF-8'></head>
                            <body style='margin: 0; padding: 0; background-color: #f4f6f8; font-family: Arial, sans-serif;'>
                            <table role='presentation' border='0' cellpadding='0' cellspacing='0' width='100%' style='background-color: #f4f6f8; padding: 40px 0;'>
                            <tr><td align='center'>
                            <table role='presentation' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.05);'>
                            <tr><td align='center' style='background-color: #0d9488; padding: 35px 20px;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 0.5px;'>Citas Industrial Platform</h1>
                            <p style='color: #ccfbf1; margin: 5px 0 0 0; font-size: 14px;'>$purposeTitle</p>
                            </td></tr>
                            <tr><td style='padding: 40px 30px; color: #333333; font-size: 15px; line-height: 1.6;'>
                            <p style='margin-top: 0; font-size: 16px;'>Hello <strong>${info.username}</strong>,</p>
                            <p>$purposeContextText</p>
                            <div style='background-color: #f0fdf4; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; border: 1px solid #bbf7d0;'>
                            <span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #0d9488;'>$rawCode</span>
                            <h5>Do not share this code with anyone.</h5>
                            </div>
                            <p style='font-size: 13px; color: #666;'>This verification code expires in 5 minutes.</p>
                            <div style='background-color: #fffbeb; border: 1px solid #fef3c7; padding: 12px; border-radius: 4px; margin-top: 20px; font-size: 13px; color: #b45309;'>
                            <strong>🛡️ Fraud & Security Advisory:</strong> Citacom Systems staff will never ask for your password, verification codes, or PIN over phone calls or chat. If you suspect unauthorized activity, contact support immediately.
                            </div>
                            </td></tr>
                            <tr><td align='center' style='background-color: #f8fafc; padding: 25px 30px; color: #64748b; font-size: 12px; border-top: 1px solid #e2e8f0;'>
                            <p style='margin: 0 0 5px 0;'><strong>Citacom Systems LTD</strong> • Support: support@citacomsys.com</p>
                            <p style='margin: 10px 0 0 0; color: #94a3b8;'>© 2026 Citacom Systems LTD. All rights reserved.</p>
                            </td></tr>
                            </table></td></tr></table></body></html>
                        """.trimIndent()

                        val jsonPayload = buildJsonObject {
                            put("from", defaultFromAddress)
                            put("reply_to", "support@citacomsys.com")
                            put("to", info.email)
                            put("subject", purposeSubject)
                            put("html", htmlBody)
                            put("text", "Hello ${info.username}, $purposeContextText $rawCode. Do not share this code with anyone.")
                        }

                        val emailSent = if (isProd) {
                            emailService.sendInstant(jsonPayload)
                        } else {
                            logger.info("CODE: $rawCode")
                            logger.info { jsonPayload }
                            true
                        }

                        if (emailSent) {
                            auditor.logIdentityAccessEvent(
                                userId = info.userId,
                                action = "MFA_EMAIL_DISPATCH",
                                outcome = "SUCCESS"
                            )
                            logger.info { "Background Email OTP successfully sent to ${info.email}" }
                        } else {
                            auditor.logIdentityAccessEvent(
                                userId = info.userId,
                                action = "MFA_EMAIL_DISPATCH",
                                outcome = "FAILED"
                            )
                            logger.error { "Failed to send background email OTP to ${info.email}. Attempting Hubtel SMS fallback..." }
                            if (info.phone != null) {
                                send(info.userId, info.phone, info.purpose)
                            }
                        }
                    }.onFailure { e ->
                        auditor.logIdentityAccessEvent(
                            userId = info.userId,
                            action = "MFA_EMAIL_DISPATCH",
                            outcome = "FAILED"
                        )
                        logger.error(e) { "Failed to send background email OTP to ${info.email}" }
                        if (info.phone != null) {
                            send(info.userId, info.phone, info.purpose)
                        }
                    }
                }
                MFAChannel.EMAIL
            } else if (info.phone != null) {
                send(
                    userId = info.userId,
                    phone = info.phone,
                    purpose = info.purpose
                )
                MFAChannel.SMS
            } else {
                error("Either email or phone number is required for MFA authentication.")
            }
        }
    }

    override suspend fun send(
        userId: String,
        phone: PhoneNumber,
        purpose: OtpPurpose
    ): Result<Unit> = runCatching {
        securityThrottler.evaluateThreats(
            userId,
            SecurityTrackableEvent.OTP_REQUEST_RATE,
        )

        logger.info { "Initiating Hubtel official OTP API dispatch for user: ${phone.mask}" }

        otpDataStore.findLatestPending(userId, purpose.name)?.let {
            otpDataStore.markAsCancelled(it.id)
        }

        val (_, hashedCode) = otpGenerator.generate()
        val validatedPhone = phone.toJvm()
        val expiryTime = Clock.System.now().plus(5, DateTimeUnit.MINUTE)
        val localRequestId = UUID.randomUUID().toString()

        val senderId = System.getProperty("HUBTEL_SENDER_NAME") ?: "CITACOM"
        val hubtelResult = runCatching {
            otpGateway.sendOtp(
                senderId = senderId,
                phoneNumber = validatedPhone.e164.removePrefix("+"),
                countryCode = "GH"
            ).getOrThrow()
        }

        val sendResponse = hubtelResult.getOrNull()
        val providerRequestId = sendResponse?.data?.requestid ?: localRequestId
        val providerPrefix = if (sendResponse?.data != null) "HUBTEL" else "OTP"

        otpDataStore.save(
            userId = userId.toUuid(),
            prefix = providerPrefix,
            requestId = providerRequestId,
            purpose = purpose.name,
            hashedCode = hashedCode,
            expiresAt = expiryTime,
        )

        if (hubtelResult.isSuccess) {
            auditor.logIdentityAccessEvent(
                userId = userId,
                action = "MFA_HUBTEL_OTP_DISPATCH",
                outcome = "SUCCESS"
            )
            logger.info { "Hubtel OTP API successfully dispatched code for ${phone.mask}" }
        } else {
            auditor.logIdentityAccessEvent(
                userId = userId,
                action = "MFA_HUBTEL_OTP_DISPATCH",
                outcome = "FAILED"
            )
            logger.error(hubtelResult.exceptionOrNull()) { "Failed to send OTP via Hubtel OTP API for ${phone.mask}" }
        }

        auditor.logIdentityAccessEvent(
            userId = userId,
            action = "MFA_OTP_QUEUED",
            outcome = "SUCCESS"
        )

        securityThrottler.recordEvent(
            userId,
            SecurityTrackableEvent.OTP_REQUEST_RATE
        )
    }

    override suspend fun verify(
        userId: String,
        code: String,
        purpose: OtpPurpose
    ): Result<Unit> = runCatching {
        securityThrottler.evaluateThreats(
            userId,
            SecurityTrackableEvent.FAILED_OTP_ATTEMPT
        )

        val pendingOtp = otpDataStore.findLatestPending(userId, purpose.name)
            ?: run {
                securityThrottler.recordEvent(
                    userId,
                    SecurityTrackableEvent.FAILED_OTP_ATTEMPT
                )
                auditor.logIdentityAccessEvent(
                    userId = userId,
                    action = "MFA_OTP_VERIFY",
                    outcome = "NOT_FOUND"
                )
                throw IllegalArgumentException("No pending validation found.")
            }

        if (Clock.System.now() > pendingOtp.expiresAt) {
            auditor.logIdentityAccessEvent(
                userId = userId,
                action = "MFA_OTP_VERIFY",
                outcome = "EXPIRED"
            )
            throw IllegalStateException("Verification code has expired.")
        }

        val isHubtelGateway = pendingOtp.prefix == "HUBTEL"

        val isVerified = if (isHubtelGateway) {
            val gatewayVerified = otpGateway.verifyOtp(
                requestId = pendingOtp.requestId,
                prefix = "OTP",
                code = code
            ).getOrDefault(false)

            if (!gatewayVerified) {
                // Fallback check against local hash for dev/test override
                otpGenerator.hash(code) == pendingOtp.hashedCode
            } else true
        } else {
            otpGenerator.hash(code) == pendingOtp.hashedCode
        }

        if (!isVerified) {
            securityThrottler.recordEvent(
                userId,
                SecurityTrackableEvent.FAILED_OTP_ATTEMPT
            )
            auditor.logIdentityAccessEvent(
                userId = userId,
                action = "MFA_OTP_VERIFY",
                outcome = "INVALID_CODE"
            )
            throw IllegalArgumentException("Invalid verification code.")
        }

        otpDataStore.markAsUsed(pendingOtp.id)
        auditor.logIdentityAccessEvent(
            userId = userId,
            action = "MFA_OTP_VERIFY",
            outcome = "SUCCESS"
        )
    }
}
