package com.citacomsys.feature.platform.data.repository

import com.citacomsys.api.platform.model.SmsPackage
import com.citacomsys.api.platform.repository.SmsPackageRepository
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.persistence.table.packages.SmsPackages
import com.citacomsys.types.Money
import com.citacomsys.types.jvm.JvmMoney
import com.citacomsys.types.jvm.toJvm
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

internal class ExposedSmsPackageRepository : SmsPackageRepository {
    private fun ResultRow.toSmsPackage() = SmsPackage(
        id = this[SmsPackages.id],
        units = this[SmsPackages.units],
        price = Money(this[SmsPackages.price], this[SmsPackages.currencyCode]),
        campaignId = this[SmsPackages.campaignId]?.toString(),
        createdAt = this[SmsPackages.createdAt],
        updatedAt = this[SmsPackages.updatedAt]
    )

    override suspend fun get(id: String): Result<SmsPackage> = runCatching {
        dbQuery {
            SmsPackages.selectAll().where {
                SmsPackages.id eq id
            }.singleOrNull()
                ?.toSmsPackage()
                ?: error("Package not found")
        }
    }

    override suspend fun getAll(): List<SmsPackage> {
        return dbQuery {
            SmsPackages.selectAll().map { it.toSmsPackage() }
        }
    }

    override suspend fun save(pkg: SmsPackage) {
        SmsPackages.insert {
            it[id] = pkg.id
            it[units] = pkg.units
            it[price] = pkg.price.amountMinor
            it[currencyCode] = pkg.price.currencyCode
            it[campaignId] = pkg.campaignId?.toUuid()
        }
    }

    override suspend fun getActive(): List<SmsPackage> = dbQuery {
        SmsPackages.selectAll().where {
            SmsPackages.active eq true
        }.map { it.toSmsPackage() }
    }
}