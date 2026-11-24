package com.citacomsys.device.infra.gateway

import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.api.device.node.DeviceRepository
import com.citacomsys.api.workspace.service.ResourceCheckResult
import com.citacomsys.api.workspace.service.WorkspaceResourceManager
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.security.plugin.resolveHardwareId
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.plugin.userPrincipal
import com.citacomsys.types.EntityType
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.ResourceType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.requireQueryParameter
import io.ktor.server.response.respond

internal class DeviceController(
    private val deviceRepository: DeviceRepository,
    private val resourceManager: WorkspaceResourceManager,
    private val sessionService: SessionService,
    private val telemetryAuditor: TelemetryAuditor
) {

    suspend fun getAllDevices(call: ApplicationCall) {
        val orgId = call.resolveOrganizationId
        val devices = call.transactionManager.runReadOnly {
            deviceRepository.getAllDevicesForOrg(orgId)
        }
        call.respond(devices)
    }

    suspend fun getDeviceBindings(call: ApplicationCall) {
        val orgId = call.resolveOrganizationId
        val hardwareId = call.parameters["id"] ?: error("Hardware id parameter missing")
        val bindings = call.transactionManager.runReadOnly {
            deviceRepository.getBindings(orgId, hardwareId, hardwareId)
        }
        call.respond(bindings)
    }

    suspend fun getDeviceSummary(call: ApplicationCall) {
        val orgId = call.resolveOrganizationId
        val hardwareId = call.parameters["id"] ?: error("Hardware id parameter missing")
        val deviceSummary = call.transactionManager.runReadOnly {
            deviceRepository.getDeviceSummary(orgId, hardwareId) ?: error("Device not found")
        }
        call.respond(deviceSummary)
    }

    suspend fun getBindings(call: ApplicationCall) {
        val workspaceId = call.resolvedWorkspaceId
        val orgId = call.resolveOrganizationId
        val devices = call.transactionManager.runReadOnly {
            deviceRepository.listBindings(orgId, workspaceId)
        }
        call.respond(devices)
    }

    suspend fun renameDevice(call: ApplicationCall) {
        val name = call.requireQueryParameter("name")
        val principal = call.userPrincipal()
        val deviceId = call.resolveHardwareId()
        val orgId = call.resolveOrganizationId

        call.transactionManager.runInTransaction {
            val device = deviceRepository.getDeviceInfo(orgId, deviceId)
                ?: error("Workstation binding not found")

            deviceRepository.updateDeviceInfo(
                orgId,
                device.copy(
                    displayName = name,
                    lastIp = call.request.local.remoteAddress
                )
            )

            telemetryAuditor.logApplicationEvent(
                action = "WORKSTATION_RENAMED",
                entityType = EntityType.DEVICE.name,
                entityId = deviceId,
                summary = "Workstation renamed to '$name' by user ${principal.userId}",
                level = TelemetryLevel.INFO
            )
        }
        call.respond(HttpStatusCode.OK)
    }

    suspend fun getSessions(call: ApplicationCall) {
        val hardwareId = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Hardware ID missing"
        )
        val orgId = call.resolveOrganizationId
        val sessions = call.transactionManager.runReadOnly {
            sessionService.getDeviceSessions(orgId, hardwareId)
        }
        call.respond(sessions)
    }

    suspend fun revokeSession(call: ApplicationCall) {
        val sessionId = call.parameters["sessionId"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Session ID missing"
        )
        sessionService.revokeSession(sessionId, call.userPrincipal().userId)
        call.respond(HttpStatusCode.OK)
    }

    suspend fun revokeDeviceGlobally(call: ApplicationCall) {
        val hardwareId = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Hardware ID missing"
        )
        val orgId = call.resolveOrganizationId
        val principal = call.userPrincipal()

        call.transactionManager.runInTransaction {
            deviceRepository.updateHardwareStatus(
                orgId = orgId,
                hardwareId = hardwareId,
                status = HardwareMasterStatus.REVOKED,
                reason = "Globally revoked by administrator ${principal.userId}",
                lastIp = call.request.local.remoteAddress
            )

            sessionService.revokeDevice(deviceId = hardwareId)

            val bindings = deviceRepository.getBindings(orgId, hardwareId, hardwareId)
            bindings.forEach { binding ->
                deviceRepository.revokeBinding(orgId, binding.bindingId)
                resourceManager.release(binding.workspaceId, ResourceType.DEVICE)
            }

            telemetryAuditor.logApplicationEvent(
                action = "DEVICE_REVOKED_GLOBALLY",
                entityType = EntityType.DEVICE.name,
                entityId = hardwareId,
                summary = "Revoked device $hardwareId globally and freed workstation slots across org $orgId",
                level = TelemetryLevel.WARNING
            )

        }
        call.respond(HttpStatusCode.OK)
    }

    suspend fun unbindWorkstation(call: ApplicationCall) {
        val bindingId = call.parameters["bindingId"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Binding ID missing"
        )
        val orgId = call.resolveOrganizationId

        call.transactionManager.runInTransaction {
            val binding = deviceRepository.getBinding(orgId, bindingId)
                ?: error("Workstation binding not found")

            deviceRepository.revokeBinding(orgId, bindingId)
            resourceManager.release(binding.workspaceId, ResourceType.DEVICE)

            telemetryAuditor.logApplicationEvent(
                action = "WORKSTATION_UNBOUND",
                entityType = EntityType.DEVICE.name,
                entityId = bindingId,
                summary = "Unbound workstation '${binding.bindingId}' from workspace ${binding.workspaceId}",
                level = TelemetryLevel.INFO
            )
        }
        call.respond(HttpStatusCode.OK)
    }

    suspend fun relinkWorkstation(call: ApplicationCall) {
        val bindingId = call.parameters["bindingId"] ?: return call.respond(
            HttpStatusCode.BadRequest,
            "Binding ID missing"
        )
        val orgId = call.resolveOrganizationId

        call.transactionManager.runInTransaction {
            val binding = deviceRepository.getBinding(orgId, bindingId)
                ?: error("Workstation binding not found")

            // Verify device capacity quota before relinking
            val checkResult = resourceManager.checkAvailability(binding.workspaceId, ResourceType.DEVICE)
            if (checkResult is ResourceCheckResult.Depleted) {
                error(checkResult.message)
            }

            deviceRepository.relinkBinding(orgId, bindingId)
            resourceManager.consume(binding.workspaceId, ResourceType.DEVICE)

            telemetryAuditor.logApplicationEvent(
                action = "WORKSTATION_RELINKED",
                entityType = EntityType.DEVICE.name,
                entityId = bindingId,
                summary = "Relinked device binding '${binding.bindingId}' to store ${binding.workspaceId}",
                level = TelemetryLevel.INFO
            )
        }
        call.respond(HttpStatusCode.OK)
    }
}
