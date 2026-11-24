package com.citacomsys.feature.telemetry.server.exposed

import org.jetbrains.exposed.v1.json.jsonb

//  Identity & Access Table
object IdentityAccessLogs : BaseSiemTable("identity_access_logs") {
    val userId = varchar("user_id", 50).index()
    val action = varchar("action", 100)
    val outcome = varchar("outcome", 50)
    val failureReason = text("failure_reason").nullable()
}

// Endpoint & Device Binding Table
object EndpointHostLogs : BaseSiemTable("endpoint_host_logs") {
    val osName = varchar("os_name", 50)
    val architecture = varchar("architecture", 20)
    val action = varchar("action", 100)
    val fingerprintHash = varchar("fingerprint_hash", 255)
}

// High-Level Business Application Audit
object ApplicationAuditLogs : BaseSiemTable("application_audit_logs") {
    val actorId = varchar("actor_id", 50).index()
    val entityType = varchar("entity_type", 50).index()
    val entityId = varchar("entity_id", 50).index()
    val action = varchar("action", 100)
    val summary = text("summary")
    val payloadChanges = jsonb<kotlinx.serialization.json.JsonElement>("payload_changes", kotlinx.serialization.json.Json.Default, kotlinx.serialization.json.JsonElement.serializer()).nullable()
}

// Network / Perimeter Logs
object NetworkSecurityLogs : BaseSiemTable("network_security_logs") {
    val clientIp = varchar("client_ip", 64).index()
    val endpointRoute = varchar("endpoint_route", 255)
    val triggerEvent = varchar("trigger_event", 100)
    val blockedDurationSeconds = integer("blocked_duration_seconds").nullable()
}

// Infrastructure & Uptime logs
object OperationalHealthLogs : BaseSiemTable("operational_health_logs") {
    val component = varchar("component", 100).index()
    val message = text("message")
    val executionTimeMs = long("execution_time_ms").nullable()
    val stackTrace = text("stack_trace").nullable()
}
