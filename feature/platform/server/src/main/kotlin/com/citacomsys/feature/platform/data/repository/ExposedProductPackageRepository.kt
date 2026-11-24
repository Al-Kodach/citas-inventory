package com.citacomsys.feature.platform.data.repository

import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.model.PackageLimits
import com.citacomsys.api.platform.model.ProductPackage
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.repository.PackageBillingTermRepository
import com.citacomsys.api.platform.repository.ProductPackageRepository
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.packages.PackageBillingTerms
import com.citacomsys.core.persistence.table.packages.ProductPackageLimits
import com.citacomsys.core.persistence.table.packages.ProductPackageModules
import com.citacomsys.core.persistence.table.packages.ProductPackages
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.BillingCycle
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.Money
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock

internal class ExposedProductPackageRepository : ProductPackageRepository,
    PackageBillingTermRepository {

    override suspend fun getPackageDetails(packageId: String): Result<ProductPackageDetails> =
        dbQuery {
            val packageRow = ProductPackages.selectAll()
                .where { ProductPackages.id eq packageId }
                .singleOrNull()
                ?: return@dbQuery Result.failure(IllegalArgumentException("Package not found: $packageId"))

            val limitsRow = ProductPackageLimits.selectAll()
                .where { ProductPackageLimits.packageId eq packageId }
                .singleOrNull()
                ?: return@dbQuery Result.failure(IllegalArgumentException("Limits not found for package: $packageId"))

            val billingTerms = PackageBillingTerms.selectAll()
                .where { PackageBillingTerms.packageId eq packageId }
                .map { it.toBillingTerm() }

            val modules = ProductPackageModules.selectAll()
                .where { ProductPackageModules.packageId eq packageId }
                .map { it[ProductPackageModules.module] }
                .toSet()

            Result.success(
                ProductPackageDetails(
                    packageInfo = ProductPackage(
                        id = packageRow[ProductPackages.id],
                        name = packageRow[ProductPackages.name],
                        tier = packageRow[ProductPackages.packageTier],
                        smsQuotaPerCycle = packageRow[ProductPackages.smsQuotaPerCycle],
                        description = packageRow[ProductPackages.description],
                        recommended = packageRow[ProductPackages.recommended],
                        isPublic = packageRow[ProductPackages.isPublic],
                        createdAt = packageRow[ProductPackages.createdAt],
                        updatedAt = packageRow[ProductPackages.updatedAt]
                    ),
                    limits = PackageLimits(
                        packageId = limitsRow[ProductPackageLimits.packageId],
                        maxUsers = limitsRow[ProductPackageLimits.maxUsers],
                        maxDevice = limitsRow[ProductPackageLimits.maxDevice],
                        maxBranches = limitsRow[ProductPackageLimits.maxBranches],
                        maxProductsLimit = limitsRow[ProductPackageLimits.maxProductsLimit],
                        maxActiveBatchesLimit = limitsRow[ProductPackageLimits.maxActiveBatchesLimit],
                        historyRetentionMonths = limitsRow[ProductPackageLimits.historyRetentionMonths]
                    ),
                    billingTerms = billingTerms,
                    modules = modules
                )
            )
        }

    override suspend fun getAllPublic(): List<ProductPackageDetails> = dbQuery {
        val packageIds = ProductPackages.selectAll()
            .where { ProductPackages.isPublic eq true }
            .map { it[ProductPackages.id] }
        packageIds.mapNotNull { getPackageDetails(it).getOrNull() }
    }

    override suspend fun save(details: ProductPackageDetails): Result<ProductPackageDetails> =
        dbQuery {
            ProductPackages.upsert {
                it[id] = details.packageInfo.id
                it[name] = details.packageInfo.name
                it[packageTier] = details.packageInfo.tier
                it[smsQuotaPerCycle] = details.packageInfo.smsQuotaPerCycle
                it[description] = details.packageInfo.description
                it[recommended] = details.packageInfo.recommended
                it[isPublic] = details.packageInfo.isPublic
                it[updatedAt] = Clock.System.now()
            }

            ProductPackageLimits.upsert {
                it[packageId] = details.limits.packageId
                it[maxUsers] = details.limits.maxUsers
                it[maxDevice] = details.limits.maxDevice
                it[maxBranches] = details.limits.maxBranches
                it[maxProductsLimit] = details.limits.maxProductsLimit
                it[maxActiveBatchesLimit] = details.limits.maxActiveBatchesLimit
                it[historyRetentionMonths] = details.limits.historyRetentionMonths
            }

            ProductPackageModules.deleteWhere { ProductPackageModules.packageId eq details.packageInfo.id }
            if (details.modules.isNotEmpty()) {
                ProductPackageModules.batchInsert(details.modules) { module ->
                    this[ProductPackageModules.packageId] = details.packageInfo.id
                    this[ProductPackageModules.module] = module
                }
            }

            Result.success(details)
        }

    private fun ResultRow.toBillingTerm() = PackageBillingTerm(
        id = this[PackageBillingTerms.id].toString(),
        packageId = this[PackageBillingTerms.packageId],
        deploymentMode = DeploymentMode.valueOf(this[PackageBillingTerms.deploymentMode]),
        billingCycle = this[PackageBillingTerms.billingCycle],
        priceAmount = Money(
            this[PackageBillingTerms.priceAmount],
            this[PackageBillingTerms.currencyCode]
        ),
        campaignId = this[PackageBillingTerms.campaignId]?.toString(),
        isDefault = this[PackageBillingTerms.isDefault],
        createdAt = this[PackageBillingTerms.createdAt],
        updatedAt = this[PackageBillingTerms.updatedAt]
    )

    override suspend fun getTerm(
        packageId: String,
        billingCycle: BillingCycle
    ): PackageBillingTerm = dbQuery {
        PackageBillingTerms.selectAll()
            .where {
                PackageBillingTerms.packageId eq packageId and
                        (PackageBillingTerms.billingCycle eq billingCycle)
            }
            .singleOrNull()
            ?.toBillingTerm()
            ?: error("Billing term not found")
    }

    override suspend fun getById(termId: String): PackageBillingTerm? = dbQuery {
        PackageBillingTerms.selectAll()
            .where { PackageBillingTerms.id eq termId.toUuid() }
            .singleOrNull()?.toBillingTerm()
    }

    override suspend fun getTerms(packageId: String): List<PackageBillingTerm> {
        return PackageBillingTerms.selectAll()
            .where { PackageBillingTerms.packageId eq packageId }
            .map { it.toBillingTerm() }
    }

    override suspend fun saveTerm(term: PackageBillingTerm) {
        PackageBillingTerms.upsert {
            it[id] = term.id.toUuid()
            it[packageId] = term.packageId
            it[deploymentMode] = term.deploymentMode.name
            it[billingCycle] = term.billingCycle
            it[priceAmount] = term.priceAmount.amountMinor
            it[currencyCode] = term.priceAmount.currencyCode
            it[campaignId] = term.campaignId?.toUuid()
            it[isDefault] = term.isDefault
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun deleteTerm(termId: String) {
        PackageBillingTerms.deleteWhere { PackageBillingTerms.id eq termId.toUuid() }
    }
}
