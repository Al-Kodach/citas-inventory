package com.citacomsys.feature.api.telemetry

interface TelemetryAuditor {
    suspend fun logIdentity(event: IdentityLog): Result<Boolean>
    suspend fun logEndpoint(event: EndpointHostLog): Result<Boolean>
    suspend fun logApplication(event: ApplicationLog): Result<Boolean>
    suspend fun logNetworkSecurity(event: NetworkLog): Result<Boolean>
    suspend fun logOperational(event: OperationalLog): Result<Boolean>
}
