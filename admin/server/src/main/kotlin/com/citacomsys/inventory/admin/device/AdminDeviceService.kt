package com.citacomsys.inventory.admin.device

import com.citacomsys.api.device.node.WorkstationSecurityContext
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.WorkstationBindings
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.types.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock

internal class AdminDeviceService(
    private val auditor: TelemetryAuditor
) {

    /**
     * Fetches a paginated and searchable list of workstations using the generic [queryPage] extension.
     */
    suspend fun fetch(query: PageQuery): PageResult<DeviceSummary> {
        return HardwareRegistry
            .leftJoin(WorkstationBindings)
            .queryPage(
                pageQuery = query,
                searchColumns = listOf(
                    WorkstationBindings.workspaceName,
                    WorkstationBindings.displayName,
                    HardwareRegistry.model,
                    HardwareRegistry.hardwareId
                ),
                filterBuilder = {
                    // Apply optional master status filtering
                    query.filters?.get("masterStatus")?.let { status ->
                        runCatching { HardwareMasterStatus.valueOf(status.uppercase()) }.getOrNull()
                            ?.let {
                                this.andWhere { HardwareRegistry.masterStatus eq it }
                            }
                    }
                    this
                },
                sortMap = mapOf(
                    "lastUsedAt" to WorkstationBindings.lastUsedAt,
                    "ID" to WorkstationBindings.displayName,
                    "status" to WorkstationBindings.status
                ),
                defaultSort = WorkstationBindings.lastUsedAt,
                mapper = { row ->
                    DeviceSummary(
                        hardwareId = row[HardwareRegistry.hardwareId],
                        model = row[HardwareRegistry.model],
                        osName = row[HardwareRegistry.osName],
                        name = row[HardwareRegistry.displayName] ?: "",
                        masterStatus = row[HardwareRegistry.masterStatus],
                        lastIp = row.getOrNull(WorkstationBindings.lastIp),
                        lastUsedAt = row.getOrNull(WorkstationBindings.lastUsedAt)
                            ?: row[HardwareRegistry.createdAt]
                    )
                }
            )
    }

    /**
     * Retrieves full cryptographic and hardware details for a specific device.
     */
    suspend fun getDevice(deviceId: String): WorkstationSecurityContext = dbQuery {
        val hardwareRow = HardwareRegistry.selectAll()
            .where { HardwareRegistry.hardwareId eq deviceId }
            .singleOrNull() ?: error("Hardware $deviceId not found")

        val bindingRow = WorkstationBindings.selectAll()
            .where { WorkstationBindings.hardwareId eq deviceId }
            .limit(1) // Admin view: show one binding (Hub context might need to list all)
            .singleOrNull()

        WorkstationSecurityContext(
            tech = HardwareTechInfo(
                hardwareId = hardwareRow[HardwareRegistry.hardwareId],
                fingerprint = hardwareRow[HardwareRegistry.fingerprint],
                model = hardwareRow[HardwareRegistry.model],
                osName = hardwareRow[HardwareRegistry.osName],
                displayName = hardwareRow[HardwareRegistry.displayName],
                architecture = hardwareRow[HardwareRegistry.architecture],
                masterStatus = hardwareRow[HardwareRegistry.masterStatus],
                securityReason = hardwareRow[HardwareRegistry.securityReason],
                updatedAt = hardwareRow[HardwareRegistry.updatedAt]
            ),
            userView = bindingRow?.let { row ->
                UserDeviceInfo(
                    hardwareId = row[WorkstationBindings.hardwareId],
                    displayName = row[WorkstationBindings.displayName] ?: "",
                    status = row[WorkstationBindings.status],
                    lastIp = row[WorkstationBindings.lastIp],
                    lastUsedAt = row[WorkstationBindings.lastUsedAt],
                )
            }
        )
    }

    /**
     * Administrative Operation: Update the display ID of a device.
     */
    suspend fun renameWorkstation(bindingId: String, newName: String): Result<Unit> = runCatching {
        dbQuery {
            val updated =
                WorkstationBindings.update({
                    WorkstationBindings.id eq bindingId.toUuid()
                }) {
                    it[displayName] = newName
                }
            if (updated == 0) throw Exception("Workstation binding not found")

            auditor.logApplicationEvent(
                action = "WORKSTATION_RENAMED",
                entityType = EntityType.DEVICE.name,
                entityId = bindingId,
                summary = "Admin renamed workstation to '$newName'",
                level = TelemetryLevel.INFO
            )
        }
    }

    /**
     * Administrative Operation: Revoke (Lock) a device.
     */
    suspend fun lockHardwareGlobally(
        hardwareId: String,
        orgId: String,
        reason: String = "Revoked by Administrator"
    ): Result<Unit> = runCatching {
        dbQuery {
            HardwareRegistry.update(
                {
                    (HardwareRegistry.hardwareId eq hardwareId) and
                            (HardwareRegistry.organizationId eq orgId.toUuid())
                }
            ) {
                it[masterStatus] = HardwareMasterStatus.REVOKED
                it[securityReason] = reason
                it[updatedAt] = Clock.System.now()
            }

            // ALSO update all WorkstationBindings for this hardwareId to REVOKED!
            WorkstationBindings.update(
                { WorkstationBindings.hardwareId eq hardwareId }
            ) {
                it[status] = DeviceStatus.REVOKED
            }

            auditor.logApplicationEvent(
                action = "DEVICE_REVOKED_GLOBALLY",
                entityType = EntityType.DEVICE.name,
                entityId = hardwareId,
                summary = "Revoked device $hardwareId globally across all workstation bindings for org $orgId",
                level = TelemetryLevel.WARNING
            )
        }
    }
}
