package com.citacomsys.organization.infra.repository

import com.citacomsys.api.organization.*
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.api.organization.model.WorkstationBindingDto
import com.citacomsys.api.workspace.model.Workspace
import com.citacomsys.api.workspace.model.WorkspaceAccount
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.api.workspace.model.WorkspaceSubscription
import com.citacomsys.kernel.model.*
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.billing.BillingRecordsTable
import com.citacomsys.core.persistence.table.tenant.*
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.Money
import com.citacomsys.types.PriceLineItem
import com.citacomsys.types.ProductModule
import com.citacomsys.types.jvm.JvmMoney
import com.citacomsys.types.jvm.toJvm
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID
import kotlin.time.Clock

internal class ExposedOrganizationRepository : OrganizationRepository {

    override suspend fun get(id: String): Result<Organization> = runCatching {
        dbQuery {
            Organizations.selectAll()
                .where { Organizations.id eq id.toUuid() }
                .singleOrNull()?.toOrganization()
                ?: throw Exception("Organization not found")
        }
    }

    override suspend fun existsByBusinessCode(code: String): Boolean = dbQuery {
        Organizations.selectAll().where { Organizations.businessCode eq code }.count() > 0
    }

    override suspend fun existsByName(name: String): Boolean = dbQuery {
        Organizations.selectAll().where { Organizations.name eq name }.count() > 0
    }

    override suspend fun findByBusinessCode(code: String): Result<Organization> = runCatching {
        dbQuery {
            Organizations.selectAll().where { Organizations.businessCode eq code }
                .singleOrNull()?.toOrganization() ?: throw Exception("Business code not found")
        }
    }

    override suspend fun create(
        name: String,
        email: String?,
        phone: String?,
        businessCode: String,
        fleetDna: String,
        brandingConfig: BrandingConfig?
    ): Result<Organization> = runCatching {
        val id = Organizations.insertAndGetId {
            it[this.name] = name
            it[this.email] = email
            it[this.phone] = phone
            it[this.businessCode] = businessCode
            it[this.fleetDna] = fleetDna
            it[this.createdAt] = Clock.System.now()
            it[this.updatedAt] = Clock.System.now()
        }

        brandingConfig?.let {
            saveBrand(id.value, it)
        }

        Organizations.selectAll()
            .where { Organizations.id eq id }.single().toOrganization()
    }

    private fun saveBrand(
        id: UUID,
        brandingConfig: BrandingConfig
    ) {
        AppBrandingTable.upsert {
            it[this.id] = id
            it[appName] = brandingConfig.appName
            it[primaryColor] = brandingConfig.primaryColor
            it[accentColor] = brandingConfig.accentColor
            it[tertiaryColor] = brandingConfig.tertiaryColor
            it[errorColor] = brandingConfig.errorColor
            it[paletteStyle] = brandingConfig.paletteStyle
            it[logoUrl] = brandingConfig.logoUrl
            it[defaultDarkMode] = brandingConfig.defaultDarkMode
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun updateOwnerId(organizationId: String, ownerId: String) {
        Organizations.update({ Organizations.id eq organizationId.toUuid() }) {
            it[this.ownerId] = ownerId.toUuid()
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun updateProfile(
        organizationId: String,
        name: String,
        email: String?,
        phone: String?
    ) {
        Organizations.update({ Organizations.id eq organizationId.toUuid() }) {
            it[this.name] = name
            it[this.email] = email
            it[this.phone] = phone
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun getRuntimeConfig(organizationId: String): OrganizationRuntimeConfig =
        dbQuery {
            val orgId = organizationId.toUuid()
            val orgRow = Organizations.selectAll().where { Organizations.id eq orgId }.single()

            val workspaces = Workspaces
                .innerJoin(WorkspaceResources).select(
                    WorkspaceResources.tier,
                    Workspaces.deploymentMode,
                    Workspaces.productType,
                    Workspaces.status,
                    Workspaces.id,
                    Workspaces.name
                )
                .where { Workspaces.organizationId eq orgId }
                .map { wsRow ->
                    val wsUuid = wsRow[Workspaces.id].value
                    val branches =
                        BranchesTable.selectAll().where { BranchesTable.workspaceId eq wsUuid }
                            .map { bRow ->
                                BranchSummary(
                                    id = bRow[BranchesTable.id].value.toString(),
                                    name = bRow[BranchesTable.name],
                                    locationCode = bRow[BranchesTable.locationCode],
                                    isActive = bRow[BranchesTable.isActive]
                                )
                            }

                    WorkspaceSummary(
                        id = wsUuid.toString(),
                        name = wsRow[Workspaces.name],
                        type = wsRow[Workspaces.productType],
                        status = wsRow[Workspaces.status].name,
                        packageTier = wsRow[WorkspaceResources.tier],
                        deploymentMode = wsRow[Workspaces.deploymentMode],
                        branches = branches
                    )
                }

            // Aggregate Metrics
            var totalWorkstations = 0
            var totalBranches = 0
            var totalStaff = 0
            var monthlyRecurringMinor = 0L

            workspaces.forEach { ws ->
                val wsUuid = UUID.fromString(ws.id)

                val usage = WorkspaceResourceUsageTable.selectAll()
                    .where { WorkspaceResourceUsageTable.workspaceId eq wsUuid }
                    .singleOrNull()

                totalWorkstations += usage?.get(WorkspaceResourceUsageTable.deviceCount) ?: 0
                totalBranches += usage?.get(WorkspaceResourceUsageTable.branchCount) ?: 0
                totalStaff += usage?.get(WorkspaceResourceUsageTable.userCount) ?: 0

                val sub = WorkspaceSubscriptionsTable.selectAll()
                    .where { WorkspaceSubscriptionsTable.workspaceId eq wsUuid }
                    .singleOrNull()

                monthlyRecurringMinor += sub?.get(WorkspaceSubscriptionsTable.recurringAmountMinor)
                    ?: 0L
            }

            // Global Balance & Billed to Date (Rollup across all workspace accounts)
            val balanceSum = WorkspaceAccountsTable.select(WorkspaceAccountsTable.balance.sum())
                .where { WorkspaceAccountsTable.organizationId eq orgId }
                .singleOrNull()?.get(WorkspaceAccountsTable.balance.sum())
                ?: java.math.BigDecimal.ZERO

            val billedTotalSum = BillingRecordsTable.select(BillingRecordsTable.amountMinor.sum())
                .where { BillingRecordsTable.workspaceId inList workspaces.map { UUID.fromString(it.id) } }
                .singleOrNull()?.get(BillingRecordsTable.amountMinor.sum()) ?: 0L

            val brandingConfig = AppBrandingTable.selectAll().where { AppBrandingTable.id eq orgId }
                .singleOrNull()?.let {
                    BrandingConfig(
                        appName = it[AppBrandingTable.appName],
                        primaryColor = it[AppBrandingTable.primaryColor],
                        accentColor = it[AppBrandingTable.accentColor],
                        tertiaryColor = it[AppBrandingTable.tertiaryColor],
                        paletteStyle = it[AppBrandingTable.paletteStyle],
                        errorColor = it[AppBrandingTable.errorColor],
                        logoUrl = it[AppBrandingTable.logoUrl],
                        defaultDarkMode = it[AppBrandingTable.defaultDarkMode]
                    )
                }

            OrganizationRuntimeConfig(
                organizationId = organizationId,
                name = orgRow[Organizations.name],
                businessCode = orgRow[Organizations.businessCode],
                fleetDna = orgRow[Organizations.fleetDna] ?: orgRow[Organizations.id].toString(),
                status = orgRow[Organizations.status],
                metrics = OrganizationMetrics(
                    totalBalance = Money(
                        balanceSum.movePointRight(2).toLong(),
                        "GHS"
                    ).toJvm().formatted(),
                    totalWorkstations = totalWorkstations,
                    totalActiveBranches = totalBranches,
                    totalStaffAccounts = totalStaff,
                    totalMonthlyRecurring = Money(monthlyRecurringMinor, "GHS").toJvm().formatted(),
                    totalBilledToDate = Money(billedTotalSum, "GHS").toJvm().formatted()
                ),
                workspaces = workspaces,
                branding = brandingConfig
            )
        }

    override suspend fun updateStatus(orgId: String, status: OrgStatus) {
        dbQuery {
            Organizations.update({ Organizations.id eq orgId.toUuid() }) {
                it[this.status] = status.name
            }
        }
    }

    override suspend fun getWorkspaceDetails(
        workspaceId: String,
        organizationId: String
    ): WorkspaceDetails = dbQuery {
        val wsUuid = workspaceId.toUuid()
        val orgUuid = organizationId.toUuid()

        val wsRow = Workspaces.selectAll()
            .where { (Workspaces.id eq wsUuid) and (Workspaces.organizationId eq orgUuid) }
            .singleOrNull()
            ?: error("Workspace $workspaceId not found in organization $organizationId")

        val accountRow = WorkspaceAccountsTable.selectAll()
            .where { WorkspaceAccountsTable.workspaceId eq wsUuid }
            .singleOrNull()

        val resourceRow = WorkspaceResources.selectAll()
            .where { WorkspaceResources.workspaceId eq wsUuid }
            .singleOrNull()

        val subRow = WorkspaceSubscriptionsTable.selectAll()
            .where { WorkspaceSubscriptionsTable.workspaceId eq wsUuid }
            .singleOrNull()

        val usageRow = WorkspaceResourceUsageTable.selectAll()
            .where { WorkspaceResourceUsageTable.workspaceId eq wsUuid }
            .singleOrNull()

        val modules = WorkspaceModules.selectAll()
            .where { WorkspaceModules.workspaceId eq wsUuid }
            .map { it[WorkspaceModules.module] }
            .toSet()

        val branches = BranchesTable.selectAll()
            .where { BranchesTable.workspaceId eq wsUuid }
            .map { bRow ->
                BranchSummary(
                    id = bRow[BranchesTable.id].value.toString(),
                    name = bRow[BranchesTable.name],
                    locationCode = bRow[BranchesTable.locationCode],
                    isActive = bRow[BranchesTable.isActive]
                )
            }

        WorkspaceDetails(
            workspaceId = wsRow[Workspaces.id].value.toString(),
            organizationId = wsRow[Workspaces.organizationId].toString(),
            name = wsRow[Workspaces.name],
            type = wsRow[Workspaces.productType],
            status = wsRow[Workspaces.status],
            account = accountRow?.toAccount(),
            resource = resourceRow?.toResource(modules),
            subscription = subRow?.toSubscription(),
            usage = usageRow?.toUsage(),
            branches = branches
        )
    }

    private fun ResultRow.toAccount() = WorkspaceAccount(
        workspaceId = this[WorkspaceAccountsTable.workspaceId].toString(),
        organizationId = this[WorkspaceAccountsTable.organizationId].toString(),
        balance = JvmMoney.fromDb(
            this[WorkspaceAccountsTable.balance],
            this[WorkspaceAccountsTable.currencyCode]
        ),
        smsBalance = this[WorkspaceAccountsTable.smsBalance],
        deploymentMode = DeploymentMode.valueOf(this[WorkspaceAccountsTable.deploymentMode]),
        paymentProviderCustomerId = this[WorkspaceAccountsTable.paymentProviderCustomerId],
        defaultPaymentMethodId = this[WorkspaceAccountsTable.defaultPaymentMethodId],
        updatedAt = this[WorkspaceAccountsTable.updatedAt]
    )

    private fun ResultRow.toResource(modules: Set<ProductModule>) = WorkspaceResource(
        packageId = this[WorkspaceResources.packageId],
        name = this[WorkspaceResources.packageName],
        tier = this[WorkspaceResources.tier],
        modules = modules,
        maxUsers = this[WorkspaceResources.maxUsers],
        maxDevice = this[WorkspaceResources.maxDevice],
        maxBranches = this[WorkspaceResources.maxBranches],
        maxProductsLimit = this[WorkspaceResources.maxProductsLimit],
        maxActiveBatchesLimit = this[WorkspaceResources.maxActiveBatchesLimit],
        historyRetentionMonths = this[WorkspaceResources.retentionMonths],
        updatedAt = this[WorkspaceResources.updatedAt]
    )

    private fun ResultRow.toSubscription() = WorkspaceSubscription(
        workspaceId = this[WorkspaceSubscriptionsTable.workspaceId].toString(),
        status = this[WorkspaceSubscriptionsTable.status],
        billingCycle = this[WorkspaceSubscriptionsTable.billingCycle],
        recurringAmount = Money(
            this[WorkspaceSubscriptionsTable.recurringAmountMinor],
            this[WorkspaceSubscriptionsTable.currencyCode]
        ),
        lineItems = try {
            Json.decodeFromString<List<PriceLineItem>>(this[WorkspaceSubscriptionsTable.lineItemsJson])
        } catch (_: Exception) {
            emptyList()
        },
        startedAt = this[WorkspaceSubscriptionsTable.startedAt],
        endedAt = this[WorkspaceSubscriptionsTable.endedAt],
        updatedAt = this[WorkspaceSubscriptionsTable.updatedAt],
        subscriptionVersion = this[WorkspaceSubscriptionsTable.version].toLong()
    )

    private fun ResultRow.toUsage() = WorkspaceResourcesUsage(
        productCountUsed = this[WorkspaceResourceUsageTable.productCountUsed],
        activeBatchesCount = this[WorkspaceResourceUsageTable.activeBatchesCount],
        deviceCount = this[WorkspaceResourceUsageTable.deviceCount],
        userCount = this[WorkspaceResourceUsageTable.userCount],
        branchCount = this[WorkspaceResourceUsageTable.branchCount],
        updatedAt = this[WorkspaceResourceUsageTable.updatedAt]
    )

    private fun ResultRow.toOrganization() = Organization(
        id = this[Organizations.id].value.toString(),
        name = this[Organizations.name],
        email = this[Organizations.email],
        phone = this[Organizations.phone],
        businessCode = this[Organizations.businessCode],
        ownerId = this[Organizations.ownerId]?.toString(),
        fleetDna = this[Organizations.fleetDna] ?: this[Organizations.id].toString(),
        status = OrgStatus.valueOf(this[Organizations.status]),
        createdAt = this[Organizations.createdAt],
        updatedAt = this[Organizations.updatedAt]
    )

    override suspend fun getWorkstationBindings(organizationId: String): List<WorkstationBindingDto> =
        dbQuery {
            val orgUuid = organizationId.toUuid()
            WorkstationBindings.selectAll().where { WorkstationBindings.orgId eq orgUuid }.map {
                WorkstationBindingDto(
                    id = it[WorkstationBindings.id].toString(),
                    hardwareId = it[WorkstationBindings.hardwareId],
                    workspaceId = it[WorkstationBindings.workspaceId].toString(),
                    orgId = it[WorkstationBindings.orgId].toString(),
                    displayName = it[WorkstationBindings.displayName] ?: "",
                    workspaceName = it[Workspaces.name],
                    status = it[WorkstationBindings.status],
                    lastIp = it[WorkstationBindings.lastIp],
                    lastUsedAt = it[WorkstationBindings.lastUsedAt],
                    createdAt = it[WorkstationBindings.createdAt]
                )
            }
        }

    override suspend fun revokeWorkstationBinding(bindingId: String) {
        dbQuery {
            WorkstationBindings.update({ WorkstationBindings.id eq bindingId.toUuid() }) {
                it[this.status] = DeviceStatus.REVOKED
            }
        }
    }
}
