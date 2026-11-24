package com.citacomsys.organization.service

import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.table.user.Users
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.Money
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Serializable
data class OrganizationReportSummary(
    val organizationId: String,
    val totalWorkspacesCount: Long,
    val activeUsersCount: Long,
    val totalSalesCount: Long,
    val totalRevenue: Money,
    val stockValuation: Money,
    val expiringBatchesCount: Long
)

class OrganizationReportService {

    suspend fun getSummary(organizationId: String): OrganizationReportSummary = dbQuery {
        val orgUuid = runCatching { UUID.fromString(organizationId) }.getOrNull()
            ?: return@dbQuery OrganizationReportSummary(
                organizationId = organizationId,
                totalWorkspacesCount = 0,
                activeUsersCount = 0,
                totalSalesCount = 0,
                totalRevenue = Money.zero("GHS"),
                stockValuation = Money.zero("GHS"),
                expiringBatchesCount = 0
            )

        val workspacesCount = Workspaces.selectAll()
            .where { Workspaces.organizationId eq orgUuid }
            .count()

        val usersCount = Users.selectAll()
            .where { Users.organizationId eq orgUuid }
            .count()

        OrganizationReportSummary(
            organizationId = organizationId,
            totalWorkspacesCount = workspacesCount,
            activeUsersCount = usersCount,
            totalSalesCount = 0,
            totalRevenue = Money.zero("GHS"),
            stockValuation = Money.zero("GHS"),
            expiringBatchesCount = 0
        )
    }
}
