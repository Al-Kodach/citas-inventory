package com.citacomsys.core.remote

import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.error.SecurityErrorResponse
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.DeviceFingerprintMismatch
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.AuthChallengeContext
import io.github.aakira.napier.Napier
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

/**
 * Base exception for any errors occurring during API interactions.
 */
open class ApiException(
    override val message: String,
    open val httpStatusCode: HttpStatusCode? = null,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Represents client-side errors (4xx).
 */
open class ApiClientException(
    message: String,
    httpStatusCode: HttpStatusCode?,
    cause: Throwable? = null
) : ApiException(message, httpStatusCode, cause)

/**
 * Represents server-side errors (5xx).
 */
open class ApiServerException(
    message: String,
    httpStatusCode: HttpStatusCode?,
    cause: Throwable? = null
) : ApiException(message, httpStatusCode, cause)


open class ApiNotFoundException(
    cause: Throwable? = null,
    message: String = "Resource not found.",
) : ApiClientException(
    message,
    HttpStatusCode.NotFound,
    cause
)

/**
 * Validation errors are now received as flattened plain text from the server.
 */
class ApiValidationException(
    message: String,
    cause: Throwable? = null
) : ApiException(message, HttpStatusCode.BadRequest, cause)

open class ApiConflictException(
    message: String = "Resource conflict.",
    cause: Throwable? = null,
) : ApiClientException(
    message,
    HttpStatusCode.Conflict,
    cause
)

class OptimisticLockException(
    message: String = "Resource conflict.",
    cause: Throwable? = null
) : ApiException(message, HttpStatusCode.Conflict, cause)


/**
 * Represents 429 Too Many Requests.
 */
class ApiThrottlingException(
    message: String,
    val retryAfterSeconds: Int = 0,
    cause: Throwable? = null
) : ApiClientException(message, HttpStatusCode.TooManyRequests, cause)


class ResourceNotModify(
    message: String = "Resource conflict."
) : ApiClientException(
    message,
    HttpStatusCode.Conflict,
)

class ApiUnauthorizedException(
    message: String = "Unauthorized access.",
    cause: Throwable? = null
) : ApiClientException(
    message,
    HttpStatusCode.Unauthorized,
    cause
)

class ApiForbiddenException(
    message: String = "Access forbidden.",
    cause: Throwable? = null
) : ApiClientException(
    message,
    HttpStatusCode.Forbidden,
    cause
)

class ChallengeRequiredException(
    val challenge: AuthChallengeContext,
    message: String = challenge.description
) : ApiClientException(message, HttpStatusCode.Forbidden)

class DeviceFingerprintMismatchException(
    message: String = "Hardware fingerprint mismatch. Device re-attestation required.",
    cause: Throwable? = null
) : ApiClientException(message, HttpStatusCode.Forbidden, cause)

class ApiNetworkException(
    message: String = "Please check your internet connection.",
    cause: Throwable? = null
) : ApiClientException(
    message, null, cause
)

fun isWorkspaceOrOrganizationIdentityError(message: String): Boolean {
    return message.contains("workspace", ignoreCase = true) ||
            message.contains("workspace identity", ignoreCase = true) ||
            message.contains("workspace context required", ignoreCase = true) ||
            message.contains("workspace not found", ignoreCase = true) ||
            message.contains("trust (FleetDNA) not established", ignoreCase = true) ||
            message.contains("workstation hardware not registered", ignoreCase = true) ||
            message.contains("organization not found", ignoreCase = true) ||
            message.contains("invalid organization identity", ignoreCase = true) ||
            message.contains("workstation attestation", ignoreCase = true) ||
            message.contains("workstation signature", ignoreCase = true)
}


suspend fun handleUnauthenticated401(
    errorText: String,
    platformStore: PlatformRuntimeStore? = null,
    tokenStore: TokenStore? = null
) {
    if (isWorkspaceOrOrganizationIdentityError(errorText)) {
        Napier.w { "[Auth] Workspace/Organization identity 401 error detected ($errorText). Clearing stores." }
        platformStore?.clearAll()
        tokenStore?.clearUserSession()
        EventBus.publish(DeviceFingerprintMismatch)
    } else {
        Napier.e { "[Auth] 401 Session error ($errorText). Revoking user session." }
        tokenStore?.clearUserSession()
        EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
    }
}

fun mapToApiException(
    errorText: String,
    response: HttpResponse,
    metadata: HttpErrorMetadata
): ApiException {
    val status = response.status
    val message = errorText.trim().ifBlank { status.description }

    if (status == HttpStatusCode.Unauthorized) {
        return ApiUnauthorizedException(message)
    }

    return when (status) {
        HttpStatusCode.BadRequest -> ApiValidationException(message)
        HttpStatusCode.Unauthorized -> ApiUnauthorizedException(message)
        HttpStatusCode.Forbidden -> {
            val securityReason = response.headers["X-Security-Reason"]
            if (securityReason == "DEVICE_FINGERPRINT_MISMATCH") {
                DeviceFingerprintMismatchException(message)
            } else {
                try {
                    val challenge = Json.decodeFromString<AuthChallengeContext>(errorText)
                    ChallengeRequiredException(challenge)
                } catch (_: Exception) {
                    ApiForbiddenException(message)
                }
            }
        }
        HttpStatusCode.NotFound -> ApiNotFoundException(message = message)
        HttpStatusCode.Conflict -> ApiConflictException(message)
        HttpStatusCode.PreconditionFailed -> OptimisticLockException(message)
        HttpStatusCode.InternalServerError -> ApiServerException(message, status)
        HttpStatusCode.NotModified -> ResourceNotModify(message)
        HttpStatusCode.TooManyRequests -> {
            val retryAfterHeader = response.headers["Retry-After"]?.toIntOrNull() ?: 0

            try {
                val errorData = Json.decodeFromString<SecurityErrorResponse>(errorText)
                ApiThrottlingException(
                    message = errorData.error ?: message,
                    retryAfterSeconds = errorData.retry_after_seconds.takeIf { it > 0 }
                        ?: retryAfterHeader
                )
            } catch (_: Exception) {
                ApiThrottlingException(message, retryAfterHeader)
            }
        }

        else -> {
            if (status.value >= 500) {
                ApiServerException(message, status)
            } else if (status.value >= 400) {
                ApiClientException(message, status)
            } else {
                ApiException(message, status)
            }
        }
    }
}
