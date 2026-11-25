package com.citacomsys.feature.telemetry.server.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.datetime.timestamp

// Identity & Access Table
object IdentityAccessLogs : Table("identity_access_logs") {
    val id = long("id").autoIncrement()
    val organizationId = uuid("organization_id").nullable()
    val workspaceId = uuid("workspace_id").nullable()
    val userId = varchar("user_id", 50).index()
    val action = varchar("action", 100)
    val outcome = varchar("outcome", 50)
    val failureReason = text("failure_reason").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
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
object NetworkSecurityLogs : Table("network_security_logs") {
    val id = long("id").autoIncrement()
    val clientIp = varchar("client_ip", 64).index()
    val endpointRoute = varchar("endpoint_route", 255)
    val triggerEvent = varchar("trigger_event", 100)
    val blockedDurationSeconds = integer("blocked_duration_seconds").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

// Infrastructure & Uptime logs
object OperationalHealthLogs : Table("operational_health_logs") {
    val id = long("id").autoIncrement()
    val component = varchar("component", 100).index()
    val message = text("message")
    val executionTimeMs = long("execution_time_ms").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
