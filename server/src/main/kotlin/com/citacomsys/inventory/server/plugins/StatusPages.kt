package com.citacomsys.inventory.server.plugins

import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.domain.result.UserAuthenticationException
import com.citacomsys.core.logging.logException
import com.citacomsys.core.security.toAuditRequestContext
import com.citacomsys.core.security.service.SecurityBreachException
import com.citacomsys.core.security.token.TokenException
import com.citacomsys.shared.error.AuthenticationException
import com.citacomsys.shared.error.AuthorizationException
import com.citacomsys.shared.error.DuplicateRecordException
import com.citacomsys.shared.error.ForeignKeyViolationException
import com.citacomsys.shared.error.GatewayException
import com.citacomsys.shared.error.OptimisticLockException
import com.citacomsys.shared.error.PersistenceException
import com.citacomsys.shared.error.ValidationException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

val RequestStartTimeKey = AttributeKey<Long>("RequestStartTime")

fun Application.configureStatusPages() {
    val auditor: TelemetryAuditor by inject()

    intercept(ApplicationCallPipeline.Monitoring) {
        call.attributes.put(RequestStartTimeKey, System.currentTimeMillis())
        proceed()
    }

    // Single, unified StatusPages installation
    install(StatusPages) {

        exception<SecurityBreachException> { call, cause ->
            // Log to SIEM first
            logToSiemAsync(auditor, call, cause)

            call.response.headers.append(HttpHeaders.RetryAfter, cause.retryAfterSeconds.toString())
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf(
                    "error" to cause.message.toString(),
                    "retry_after_seconds" to cause.retryAfterSeconds.toString()
                )
            )
        }

        exception<UserAuthenticationException> { call, cause ->
            logAndRespond(auditor, call, HttpStatusCode.Unauthorized, cause.result.message, cause)
        }

        exception<AuthorizationException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.Forbidden,
                cause.message ?: "Access denied",
                cause
            )
        }

        exception<AuthenticationException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.Unauthorized,
                cause.message ?: "Authentication error",
                cause
            )
        }

        exception<TokenException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.Unauthorized,
                cause.message ?: "Invalid token",
                cause
            )
        }

        exception<NoSuchElementException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.NotFound,
                cause.message ?: "Resource not found",
                cause
            )
        }

        exception<NotFoundException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.NotFound,
                cause.message ?: "Resource not found",
                cause
            )
        }

        exception<IllegalStateException> { call, cause ->
            if (cause.message == "Security scope validation broken") {
                // Reroute workspace violations to 403 Forbidden
                logAndRespond(
                    auditor,
                    call,
                    HttpStatusCode.Forbidden,
                    cause.message ?: "Access denied",
                    cause
                )
            } else {
                // Internal server state exceptions map to 500 Internal Server Error
                logAndRespond(
                    auditor,
                    call,
                    HttpStatusCode.InternalServerError,
                    cause.message ?: "Internal server error occurred",
                    cause
                )
            }
        }

        exception<OptimisticLockException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.Conflict,
                "Record modified by someone else.",
                cause
            )
        }

        exception<DuplicateRecordException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.Conflict,
                cause.message ?: "Duplicate resource",
                cause
            )
        }

        exception<ForeignKeyViolationException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.BadRequest,
                cause.message ?: "Invalid reference",
                cause
            )
        }

        exception<PersistenceException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.InternalServerError,
                cause.message ?: "Storage error",
                cause
            )
        }

        exception<NumberFormatException> { call, cause ->
            logAndRespond(auditor, call, HttpStatusCode.BadRequest, "Invalid number format", cause)
        }

        exception<IllegalArgumentException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.BadRequest,
                cause.message ?: "Invalid argument",
                cause
            )
        }

        exception<BadRequestException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.BadRequest,
                cause.message ?: "Bad request",
                cause
            )
        }

        exception<ValidationException> { call, cause ->
            val message = if (cause.errors.isNotEmpty()) {
                cause.errors.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            } else {
                "Validation failed"
            }
            logAndRespond(auditor, call, HttpStatusCode.BadRequest, message, cause)
        }

        exception<SecurityException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.Unauthorized,
                cause.message ?: "Unauthorized",
                cause
            )
        }

        exception<GatewayException> { call, cause ->
            logAndRespond(
                auditor,
                call,
                HttpStatusCode.ServiceUnavailable,
                cause.message ?: "Gateway error",
                cause
            )
        }

        // Generic fallback for any completely unhandled system exception
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled error: ${cause.message}" }
            logToSiemAsync(auditor, call, cause)

            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "An unexpected system error occurred. Reference trace logged securely.")
            )
        }
    }
}

/**
 * Enhanced helper function that sends logs to the standard application console
 * AND offloads secure logging safely to SIEM/Database system asynchronously.
 */
private suspend fun logAndRespond(
    auditor: TelemetryAuditor,
    call: ApplicationCall,
    statusCode: HttpStatusCode,
    message: String,
    cause: Throwable
) {
    cause.printStackTrace()
    if (statusCode.value >= 500) {
        logger.error(cause) { "🚨 CRITICAL SERVER ERROR (${statusCode.value}): $message" }
    } else {
        logger.warn { "Warning (${statusCode.value}): $message" }
    }

    // Asynchronously log to SIEM so database operations don't delay client response
    logToSiemAsync(auditor, call, cause)

    call.respond(statusCode, message)
}

/**
 * Isolated logic to calculate time metrics, pull request contexts, and dispatch to SIEM thread pool
 */
private fun logToSiemAsync(auditor: TelemetryAuditor, call: ApplicationCall, cause: Throwable) {
    val startTime = call.attributes.getOrNull(RequestStartTimeKey)
    val executionTime = startTime?.let { System.currentTimeMillis() - it }

    // Using Ktor's application coroutine scope or Dispatchers.IO ensures it finishes even if call ends
    CoroutineScope(Dispatchers.IO).launch {
        auditor.logException(
            component = "KGlobalPipelineInterceptor",
            exception = cause,
            executionTime = executionTime
        )
    }
}
