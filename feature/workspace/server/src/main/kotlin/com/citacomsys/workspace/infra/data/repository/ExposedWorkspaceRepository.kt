package com.citacomsys.workspace.infra.data.repository

import com.citacomsys.api.workspace.model.*
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.*
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.*
import com.citacomsys.types.jvm.JvmMoney
import com.citacomsys.types.jvm.toJvm
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.time.Clock

internal class ExposedWorkspaceRepository : WorkspaceRepository {

    override suspend fun get(workspaceId: String): Result<Workspace> = runCatching {
        dbQuery {
            Workspaces.selectAll().where { Workspaces.id eq workspaceId.toUuid() }
                .singleOrNull()?.toWorkspace() ?: throw Exception("Workspace not found")
        }
    }

    override suspend fun findByIdentifier(identifier: String): Result<Workspace> = runCatching {
        dbQuery {
            val cleanId = identifier.lowercase().trim()
            Workspaces.selectAll().where {
                (Workspaces.productKey.lowerCase() eq cleanId) or (Workspaces.businessCode.lowerCase() eq cleanId)
            }.singleOrNull()?.toWorkspace() ?: throw Exception("Workspace not found")
        }
    }

    override suspend fun updateStatus(
        workspaceId: String,
        status: WorkspaceStatus,
        reason: String?
    ): Result<Unit> = runCatching {
        dbQuery {
            val count = Workspaces.update({ Workspaces.id eq workspaceId.toUuid() }) {
                it[this.status] = status
                if (reason != null && status == WorkspaceStatus.SUSPENDED) {
                    it[this.suspendedReason] = reason
                } else if (status == WorkspaceStatus.ACTIVE) {
                    it[this.suspendedReason] = null
                }
                it[updatedAt] = Clock.System.now()
            }
            if (count == 0) {
                throw NoSuchElementException("Workspace not found: $workspaceId")
            }
        }
    }

    override suspend fun findDuplicate(
        orgId: String,
        type: ProductType,
        name: String
    ): Workspace? {
        return Workspaces.selectAll()
            .where {
                (Workspaces.organizationId eq orgId.toUuid()) and
                        (Workspaces.productType eq type) and
                        (Workspaces.name.lowerCase() eq name.lowercase()) and
                        (Workspaces.status neq WorkspaceStatus.DELETED)
            }
            .singleOrNull()?.toWorkspace()
    }

    override suspend fun create(
        organizationId: String,
        name: String,
        packageTier: PackageTier,
        businessType: ProductType,
        productKey: String,
        deploymentMode: DeploymentMode,
        businessCode: String,
        address: Address,
        email: String?,
        phone: PhoneNumber?,
        status: WorkspaceStatus
    ): Result<Workspace> = runCatching {
        dbQuery {
            val id = Workspaces.insertAndGetId {
                it[this.organizationId] = organizationId.toUuid()
                it[this.name] = name
                it[this.productType] = businessType
                it[this.phone] = phone?.toJvm()?.e164
                it[this.address] = address
                it[this.email] = email
                it[this.status] = status
                it[this.deploymentMode] = deploymentMode
                it[this.productKey] = productKey
                it[this.businessCode] = businessCode
                it[this.createdAt] = Clock.System.now()
                it[this.updatedAt] = Clock.System.now()
            }
            Workspaces.selectAll().where { Workspaces.id eq id }.single().toWorkspace()
        }
    }

    override suspend fun findBrand(workspaceId: String): BrandingConfig? = dbQuery {
        AppBrandingTable.selectAll()
            .where { AppBrandingTable.id eq workspaceId.toUuid() }
            .singleOrNull()?.toBrand()
    }

    override suspend fun deleteBranding(workspaceId: String) {
        dbQuery {
            AppBrandingTable.deleteWhere { AppBrandingTable.id eq workspaceId.toUuid() }
        }
    }

    override suspend fun saveBrand(
        workspaceId: String,
        brandingConfig: BrandingConfig
    ) {
        dbQuery {
            AppBrandingTable.upsert {
                it[this.id] = workspaceId.toUuid()
                it[appName] = brandingConfig.appName
                it[primaryColor] = brandingConfig.primaryColor
                it[accentColor] = brandingConfig.accentColor
                it[tertiaryColor] = brandingConfig.tertiaryColor
                it[paletteStyle] = brandingConfig.paletteStyle
                it[errorColor] = brandingConfig.errorColor
                it[logoUrl] = brandingConfig.logoUrl
                it[defaultDarkMode] = brandingConfig.defaultDarkMode
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun getAccount(workspaceId: String): WorkspaceAccount = dbQuery {
        WorkspaceAccountsTable.selectAll()
            .where { WorkspaceAccountsTable.workspaceId eq workspaceId.toUuid() }
            .singleOrNull()?.toAccount() ?: error("Account not found")
    }

    override suspend fun saveAccount(workspaceId: String, account: WorkspaceAccount) = dbQuery {
        WorkspaceAccountsTable.upsert {
            it[organizationId] = account.organizationId.toUuid()
            it[this.workspaceId] = workspaceId.toUuid()
            it[balance] = account.balance.toJvm().amount
            it[currencyCode] = account.balance.currencyCode
            it[smsBalance] = account.smsBalance
            it[deploymentMode] = account.deploymentMode.name
            it[paymentProviderCustomerId] = account.paymentProviderCustomerId
            it[defaultPaymentMethodId] = account.defaultPaymentMethodId
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun decrementSmsBalance(workspaceId: String) = dbQuery {
        WorkspaceAccountsTable.update({ WorkspaceAccountsTable.workspaceId eq workspaceId.toUuid() }) {
            it[smsBalance] = smsBalance minus 1
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun updateDeployMode(
        workspaceId: String,
        deploymentMode: DeploymentMode
    ) = dbQuery {
        WorkspaceAccountsTable.update({ WorkspaceAccountsTable.workspaceId eq workspaceId.toUuid() }) {
            it[this.deploymentMode] = deploymentMode.name
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun getSubscription(workspaceId: String): WorkspaceSubscription = dbQuery {
        WorkspaceSubscriptionsTable.selectAll()
            .where { WorkspaceSubscriptionsTable.workspaceId eq workspaceId.toUuid() }
            .singleOrNull()?.toSubscription() ?: error("Subscription not found for $workspaceId")
    }

    override suspend fun saveSubscription(
        workspaceId: String,
        subscription: WorkspaceSubscription
    ) = dbQuery {
        WorkspaceSubscriptionsTable.upsert {
            it[this.workspaceId] = workspaceId.toUuid()
            it[billingCycle] = subscription.billingCycle
            it[status] = subscription.status
            it[recurringAmountMinor] = subscription.recurringAmount.amountMinor
            it[currencyCode] = subscription.recurringAmount.currencyCode
            it[lineItemsJson] = Json.encodeToString<List<PriceLineItem>>(subscription.lineItems)
            it[startedAt] = subscription.startedAt
            it[endedAt] = subscription.endedAt
            it[version] = subscription.subscriptionVersion.toInt()
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun updateIdentity(
        workspaceId: String,
        name: String?,
        email: String?,
        phone: String?
    ) = dbQuery {
        Workspaces.update({ Workspaces.id eq workspaceId.toUuid() }) {
            if (name != null) it[this.name] = name
            if (email != null) it[this.email] = email
            if (phone != null) it[this.phone] = phone
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }


    private fun ResultRow.toWorkspace() = Workspace(
        id = this[Workspaces.id].value.toString(),
        organizationId = this[Workspaces.organizationId].toString(),
        name = this[Workspaces.name],
        businessType = this[Workspaces.productType],
        productKey = this[Workspaces.productKey],
        businessCode = this[Workspaces.businessCode],
        status = this[Workspaces.status],
        deploymentMode = this[Workspaces.deploymentMode],
        address = this[Workspaces.address],
        email = this[Workspaces.email],
        phone = this[Workspaces.phone]?.let { PhoneNumber(it) },
        createdAt = this[Workspaces.createdAt],
        updatedAt = this[Workspaces.updatedAt]
    )


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

    private fun ResultRow.toBrand() = BrandingConfig(
        appName = this[AppBrandingTable.appName],
        logoUrl = this[AppBrandingTable.logoUrl],
        primaryColor = this[AppBrandingTable.primaryColor],
        accentColor = this[AppBrandingTable.accentColor],
        tertiaryColor = this[AppBrandingTable.tertiaryColor],
        paletteStyle = this[AppBrandingTable.paletteStyle],
        errorColor = this[AppBrandingTable.errorColor],
        defaultDarkMode = this[AppBrandingTable.defaultDarkMode],
        updatedAt = this[AppBrandingTable.updatedAt]
    )

    private fun ResultRow.toSubscription() = WorkspaceSubscription(
        workspaceId = this[WorkspaceSubscriptionsTable.workspaceId].toString(),
        status = this[WorkspaceSubscriptionsTable.status],
        billingCycle = this[WorkspaceSubscriptionsTable.billingCycle],
        recurringAmount = Money(
            this[WorkspaceSubscriptionsTable.recurringAmountMinor],
            this[WorkspaceSubscriptionsTable.currencyCode]
        ),
        lineItems = Json.decodeFromString<List<PriceLineItem>>(this[WorkspaceSubscriptionsTable.lineItemsJson]),
        startedAt = this[WorkspaceSubscriptionsTable.startedAt],
        endedAt = this[WorkspaceSubscriptionsTable.endedAt],
        updatedAt = this[WorkspaceSubscriptionsTable.updatedAt],
        subscriptionVersion = this[WorkspaceSubscriptionsTable.version].toLong()
    )
}
