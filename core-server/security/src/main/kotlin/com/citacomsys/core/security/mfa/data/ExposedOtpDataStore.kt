package com.citacomsys.core.security.mfa.data

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.mfa.OtpRecord
import com.citacomsys.core.security.mfa.data.table.OtpTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

internal class ExposedOtpDataStore : OtpDataStore {

    private fun ResultRow.toOtpData(): OtpRecord {
        val rawReqId = this[OtpTable.providerRequestId]
        val isHubtel = rawReqId.startsWith("HUBTEL:")
        val prefix = if (isHubtel) "HUBTEL" else "OTP"
        val requestId = if (isHubtel) rawReqId.substringAfter("HUBTEL:") else rawReqId

        return OtpRecord(
            id = this[OtpTable.id].value.toString(),
            userId = this[OtpTable.userId].toString(),
            requestId = requestId,
            prefix = prefix,
            hashedCode = this[OtpTable.hashedCode],
            expiresAt = this[OtpTable.expiresAt],
            purpose = this[OtpTable.purpose],
            isUsed = this[OtpTable.usedAt] != null,
            isCancelled = this[OtpTable.cancelledAt] != null
        )
    }

    override suspend fun save(
        userId: UUID,
        purpose: String,
        prefix: String,
        requestId: String,
        hashedCode: String,
        expiresAt: Instant
    ): Unit = dbQuery {
        val storedRequestId = if (prefix == "HUBTEL") "HUBTEL:$requestId" else requestId
        OtpTable.insertAndGetId {
            it[OtpTable.userId] = userId
            it[OtpTable.purpose] = purpose
            it[OtpTable.hashedCode] = hashedCode
            it[OtpTable.providerRequestId] = storedRequestId
            it[OtpTable.expiresAt] = expiresAt
        }
    }

    override suspend fun findLatestPending(userId: String, purpose: String): OtpRecord? = dbQuery {
        OtpTable.selectAll()
            .where {
                (OtpTable.userId eq userId.toUuid()) and
                        (OtpTable.usedAt.isNull()) and
                        (OtpTable.cancelledAt.isNull()) and OtpTable.purpose.eq(purpose)
            }
            .orderBy(OtpTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.toOtpData()
    }

    override suspend fun markAsUsed(id: String): Unit = dbQuery {
        OtpTable.update({ OtpTable.id eq id.toUuid() }) {
            it[usedAt] = Clock.System.now()
        }
    }

    override suspend fun markAsCancelled(id: String): Unit = dbQuery {
        OtpTable.update({ OtpTable.id eq id.toUuid() }) {
            it[cancelledAt] = Clock.System.now()
        }
    }
}
