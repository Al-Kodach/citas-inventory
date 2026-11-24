package com.citacomsys.feature.user.server.adaptor

import com.citacomsys.core.persistence.table.user.PlatformAdmins
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.inventory.api.user.model.PlatformAdmin
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.UserStatus
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

internal class ExposedPlatformAdminRepository : PlatformAdminRepository {
    override suspend fun findById(id: String): Result<PlatformAdmin> = dbQuery {
        val uuid = UUID.fromString(id)
        PlatformAdmins.selectAll().where { PlatformAdmins.id eq uuid }
            .map { row ->
                PlatformAdmin(
                    id = row[PlatformAdmins.id].toString(),
                    name = row[PlatformAdmins.name],
                    username = row[PlatformAdmins.username],
                    phone = row[PlatformAdmins.phone],
                    email = row[PlatformAdmins.email],
                    passwordHash = row[PlatformAdmins.passwordHash],
                    role = row[PlatformAdmins.role],
                    status = row[PlatformAdmins.status],
                    activeHardwareId = row[PlatformAdmins.activeHardwareId],
                    mfaEnabled = row[PlatformAdmins.mfaEnabled]
                )
            }.singleOrNull()?.let { Result.success(it) } ?: Result.failure(IllegalArgumentException("Platform admin not found: $id"))
    }

    override suspend fun findByIdentifier(identity: String): Result<PlatformAdmin> = dbQuery {
        PlatformAdmins.selectAll().where {
            (PlatformAdmins.username eq identity) or
            (PlatformAdmins.phone eq PhoneNumber(identity).normalized) or
            (PlatformAdmins.email eq identity)
        }.map { row ->
            PlatformAdmin(
                id = row[PlatformAdmins.id].toString(),
                name = row[PlatformAdmins.name],
                username = row[PlatformAdmins.username],
                phone = row[PlatformAdmins.phone],
                email = row[PlatformAdmins.email],
                passwordHash = row[PlatformAdmins.passwordHash],
                role = row[PlatformAdmins.role],
                status = row[PlatformAdmins.status],
                activeHardwareId = row[PlatformAdmins.activeHardwareId],
                mfaEnabled = row[PlatformAdmins.mfaEnabled]
            )
        }.singleOrNull()?.let { Result.success(it) } ?: Result.failure(IllegalArgumentException("Platform admin not found for identifier: $identity"))
    }

    override suspend fun updateActiveHardware(adminId: String, hardwareId: String) {
        dbQuery {
            val uuid = UUID.fromString(adminId)
            PlatformAdmins.update({ PlatformAdmins.id eq uuid }) {
                it[PlatformAdmins.activeHardwareId] = hardwareId
            }
        }
    }

    override suspend fun updateStatus(adminId: String, status: UserStatus) {
        dbQuery {
            val uuid = UUID.fromString(adminId)
            PlatformAdmins.update({ PlatformAdmins.id eq uuid }) {
                it[PlatformAdmins.status] = status
            }
        }
    }
}
