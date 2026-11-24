package com.citacomsys.device.infra.repository

import com.citacomsys.api.device.node.DeviceRepository
import com.citacomsys.api.device.node.WorkstationSecurityContext
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.WorkstationBindings
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.types.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock

internal class ExposedDeviceRepository : DeviceRepository {

    private fun ResultRow.toTechInfo() = HardwareTechInfo(
        hardwareId = this[HardwareRegistry.hardwareId],
        fingerprint = this[HardwareRegistry.fingerprint],
        model = this[HardwareRegistry.model],
        osName = this[HardwareRegistry.osName],
        architecture = this[HardwareRegistry.architecture],
        masterStatus = this[HardwareRegistry.masterStatus],
        securityReason = this[HardwareRegistry.securityReason],
        updatedAt = this[HardwareRegistry.updatedAt]
    )

    private fun ResultRow.toUserDeviceInfo() = UserDeviceInfo(
        hardwareId = this[WorkstationBindings.hardwareId],
        status = this[WorkstationBindings.status],
        displayName = this[WorkstationBindings.displayName] ?: "",
        lastIp = this[WorkstationBindings.lastIp],
        lastUsedAt = this[WorkstationBindings.lastUsedAt],
    )

    private fun toDeviceInfo(row: ResultRow): UserDeviceInfo {
        return UserDeviceInfo(
            hardwareId = row[HardwareRegistry.hardwareId],
            status = row.getOrNull(WorkstationBindings.status) ?:
            if (row[HardwareRegistry.masterStatus] == HardwareMasterStatus.ACTIVE)
                DeviceStatus.ACTIVE else DeviceStatus.LOCKED,
            displayName = row.getOrNull(WorkstationBindings.displayName)
                ?: row[HardwareRegistry.displayName] ?: row[HardwareRegistry.model] ?: "Workstation",
            lastIp = row.getOrNull(WorkstationBindings.lastIp),
            lastUsedAt = row.getOrNull(WorkstationBindings.lastUsedAt)
                ?: row[HardwareRegistry.updatedAt]
        )
    }

    override suspend fun getHardware(orgId: String, hardwareId: String): HardwareTechInfo? =
        dbQuery {
            HardwareRegistry.selectAll()
                .where {
                    HardwareRegistry.hardwareId eq hardwareId and
                            (HardwareRegistry.organizationId eq orgId.toUuid())
                }
                .singleOrNull()?.toTechInfo()
        }

    override suspend fun registerHardware(orgId: String, info: HardwareTechInfo): Unit = dbQuery {
        HardwareRegistry.upsert {
            it[hardwareId] = info.hardwareId
            it[organizationId] = orgId.toUuid()
            it[masterStatus] = info.masterStatus
            it[securityReason] = info.securityReason
            it[fingerprint] = info.fingerprint
            it[model] = info.model
            it[osName] = info.osName
            it[architecture] = info.architecture
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun updateHardwareStatus(
        orgId: String,
        hardwareId: String,
        status: HardwareMasterStatus,
        reason: String?,
        lastIp: String?,
    ): Unit = dbQuery {
        HardwareRegistry.update({
            HardwareRegistry.hardwareId eq hardwareId and
                    (HardwareRegistry.organizationId eq orgId.toUuid())
        }) {
            it[masterStatus] = status
            it[securityReason] = reason
            it[this.lastIp] = lastIp
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun getDeviceInfo(
        orgId: String,
        hardwareId: String
    ): UserDeviceInfo? = dbQuery {
        HardwareRegistry.selectAll()
            .where {
                (HardwareRegistry.hardwareId eq hardwareId) and
                        (HardwareRegistry.masterStatus eq HardwareMasterStatus.ACTIVE) and
                        (HardwareRegistry.organizationId eq orgId.toUuid())
            }
            .singleOrNull()?.let { toDeviceInfo(it) }
    }

    override suspend fun getDeviceSummary(orgId: String, hardwareId: String): DeviceSummary? = dbQuery {
        val info = getHardware(orgId, hardwareId) ?: return@dbQuery null

        val bindings = WorkstationBindings.selectAll()
            .where {
                WorkstationBindings.hardwareId eq hardwareId and
                        (WorkstationBindings.orgId eq orgId.toUuid())
            }
            .map { it.toBinding() }
            .sortedByDescending { it.lastUsedAt }

        DeviceSummary(
            hardwareId = info.hardwareId,
            name = info.displayName ?: info.model ?: "Workstation",
            model = info.model,
            osName = info.osName,
            masterStatus = info.masterStatus,
            lastIp = bindings.firstOrNull()?.lastIp,
            lastUsedAt = bindings.firstOrNull()?.lastUsedAt ?: info.updatedAt,
            bindings = bindings
        )
    }

    override suspend fun getAllDevicesForOrg(orgId: String): List<UserDeviceInfo> = dbQuery {
        (HardwareRegistry leftJoin WorkstationBindings)
            .selectAll()
            .where {
                HardwareRegistry.organizationId eq orgId.toUuid() and
                        (HardwareRegistry.masterStatus eq HardwareMasterStatus.ACTIVE)
            }
            .map { row -> toDeviceInfo(row) }
            .distinctBy { it.hardwareId }
    }

    override suspend fun getBindings(
        orgId: String,
        identity: String,
        hardwareId: String
    ): List<WorkstationBinding> = dbQuery {
        WorkstationBindings.selectAll()
            .where {
                (WorkstationBindings.hardwareId eq identity) or
                        (WorkstationBindings.workspaceId eq identity.toUuid()) and
                        (WorkstationBindings.orgId eq orgId.toUuid())
            }
            .map { it.toBinding() }
    }

    override suspend fun getBinding(
        orgId: String,
        bindingId: String
    ): WorkstationBinding? = dbQuery {
        WorkstationBindings.selectAll()
            .where {
                (WorkstationBindings.id eq bindingId.toUuid()) and
                        (WorkstationBindings.orgId eq orgId.toUuid())
            }
            .singleOrNull()?.toBinding()
    }

    override suspend fun saveBinding(
        orgId: String,
        hardwareId: String,
        binding: WorkstationBinding
    ): Unit = dbQuery {
        WorkstationBindings.upsert {
            it[id] = binding.bindingId.toUuid()
            it[this.hardwareId] = hardwareId
            it[workspaceId] = binding.workspaceId.toUuid()
            it[this.orgId] = orgId.toUuid()
            it[workspaceName] = binding.workspaceName
            it[status] = binding.status
            it[lastIp] = binding.lastIp
            it[lastUsedAt] = binding.lastUsedAt
        }
    }

    override suspend fun updateDeviceInfo(
        orgId: String,
        deviceInfo: UserDeviceInfo
    ): Unit = dbQuery {
        WorkstationBindings.update({
            WorkstationBindings.hardwareId eq deviceInfo.hardwareId and
                    (WorkstationBindings.orgId eq orgId.toUuid())
        }) {
            it[displayName] = deviceInfo.displayName
            it[status] = deviceInfo.status
            it[lastIp] = deviceInfo.lastIp
            it[lastUsedAt] = Clock.System.now()
        }

        HardwareRegistry.update({
            HardwareRegistry.hardwareId eq deviceInfo.hardwareId and
                    (HardwareRegistry.organizationId eq orgId.toUuid())
        }) {
            it[displayName] = deviceInfo.displayName
            it[updatedAt] = Clock.System.now()
            it[displayName] = deviceInfo.displayName
            it[masterStatus] = deviceInfo.status.toMasterStatus
            it[lastIp] = deviceInfo.lastIp
            it[lastUsedAt] = Clock.System.now()
        }
    }

    override suspend fun updateBindingUsage(orgId: String, bindingId: String, ip: String): Unit =
        dbQuery {
            WorkstationBindings.update({
                WorkstationBindings.id eq bindingId.toUuid() and
                        (HardwareRegistry.organizationId eq orgId.toUuid())
            }
            ) {
                it[lastUsedAt] = Clock.System.now()
                it[lastIp] = ip
            }

            HardwareRegistry.update({
                HardwareRegistry.hardwareId eq bindingId and
                        (HardwareRegistry.organizationId eq orgId.toUuid())
            }) {
                it[updatedAt] = Clock.System.now()
            }
        }

    override suspend fun revokeBinding(orgId: String, bindingId: String): Unit = dbQuery {
        WorkstationBindings.update({
            WorkstationBindings.id eq bindingId.toUuid() and
                    (WorkstationBindings.orgId eq orgId.toUuid())
        }
        ) {
            it[status] = DeviceStatus.REVOKED
        }
    }

    override suspend fun relinkBinding(orgId: String, bindingId: String): Unit = dbQuery {
        WorkstationBindings.update({
            WorkstationBindings.id eq bindingId.toUuid() and
                    (WorkstationBindings.orgId eq orgId.toUuid())
        }
        ) {
            it[status] = DeviceStatus.ACTIVE
            it[lastUsedAt] = Clock.System.now()
        }
    }

    override suspend fun listBindings(orgId: String, workspaceId: String): List<UserDeviceInfo> =
        dbQuery {
            WorkstationBindings.selectAll()
                .where {
                    WorkstationBindings.workspaceId eq workspaceId.toUuid() and
                            (WorkstationBindings.status eq DeviceStatus.ACTIVE) and
                            (HardwareRegistry.organizationId eq orgId.toUuid())
                }
                .map { it.toUserDeviceInfo() }
        }

    override suspend fun getSecurityContext(
        orgId: String,
        workspaceId: String,
        hardwareId: String
    ): WorkstationSecurityContext = dbQuery {
        val hardware = HardwareRegistry.selectAll()
            .where {
                HardwareRegistry.hardwareId eq hardwareId and
                        (HardwareRegistry.organizationId eq orgId.toUuid())
            }
            .singleOrNull() ?: throw NoSuchElementException("Hardware not registered")

        val binding = WorkstationBindings.selectAll()
            .where {
                (WorkstationBindings.hardwareId eq hardwareId) and
                        (WorkstationBindings.workspaceId eq workspaceId.toUuid())
            }
            .singleOrNull()

        WorkstationSecurityContext(
            tech = hardware.toTechInfo(),
            userView = binding?.toUserDeviceInfo()
        )
    }

    private fun ResultRow.toBinding() = WorkstationBinding(
        bindingId = this[WorkstationBindings.id].toString(),
        workspaceId = this[WorkstationBindings.workspaceId].toString(),
        workspaceName = this[WorkstationBindings.workspaceName],
        status = this[WorkstationBindings.status],
        boundAt = this[WorkstationBindings.createdAt],
        lastIp = this[WorkstationBindings.lastIp],
        lastUsedAt = this[WorkstationBindings.lastUsedAt]
    )
}
