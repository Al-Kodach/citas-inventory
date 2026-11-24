package com.citacomsys.feature.api.telemetry

import kotlinx.serialization.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@Serializable
data class TelemetryRequestContext(
    val requestId: String,
    val clientIp: String,
    val endpointRoute: String,
    val appFlavor: String,
    val userAgent: String,
    val organizationId: String?,
    val workspaceId: String? = null,
    val branchId: String? = null,
    val userId: String?,
    val deviceId: String?,
    val sessionId: String?
)

/**
 * A type-safe Coroutine Element that wraps our security telemetry context.
 */
class TelemetryRequestContextElement(
    val context: TelemetryRequestContext
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<TelemetryRequestContextElement>
}

/**
 * Accessor property to easily read the implicit TelemetryRequestContext from any CoroutineScope.
 */
val CoroutineContext.telemetryContext: TelemetryRequestContext?
    get() = this[TelemetryRequestContextElement.Key]?.context
