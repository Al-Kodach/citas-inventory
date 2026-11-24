package com.citacomsys.pharmacy.repository

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.pharmacy.exposedsql.PharmacyProductViewTable
import com.citacomsys.pharmacy.exposedsql.PharmacyProductsTable
import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.pharmacy.api.PharmacyProductRepository
import com.citacomsys.pharmacy.api.model.PharmacyProduct
import com.citacomsys.types.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

internal class ExposedPharmacyProductRepository(
    private val baseRepo: ProductRepository
) : ProductRepository by baseRepo, PharmacyProductRepository {


    override suspend fun getPharmacyProduct(id: String, workspaceId: String): PharmacyProduct? =
        dbQuery {
            PharmacyProductViewTable.selectAll()
                .where {
                    (PharmacyProductViewTable.id eq id.toUuid()) and
                            (PharmacyProductViewTable.workspaceId eq workspaceId.toUuid()) and
                            (PharmacyProductViewTable.originApp eq ProductType.PHARMACY)
                }
                .map { it.toPharmacyProduct() }
                .singleOrNull()
        }

    override suspend fun fetch(workspaceId: String, query: PageQuery): PageResult<ProductSnapshot> {
        return PharmacyProductViewTable.queryPage(
            pageQuery = query,
            searchColumns = listOf(
                PharmacyProductViewTable.name,
                PharmacyProductViewTable.brandName,
                PharmacyProductViewTable.genericName, 
                PharmacyProductViewTable.sku,
                PharmacyProductViewTable.barcode
            ),
            filterBuilder = {
                this.andWhere { PharmacyProductViewTable.workspaceId eq workspaceId.toUuid() }
                this
            },
            sortMap = mapOf(
                "id" to PharmacyProductViewTable.name,
                "stock" to PharmacyProductViewTable.totalAvailable
            ),
            defaultSort = PharmacyProductViewTable.name,
            mapper = { it.toProductSnapshot() }
        )
    }

    override suspend fun updatePharmacyProduct(
        product: PharmacyProduct,
        workspaceId: String
    ) = dbQuery {
        saveBaseIdentity(product)
        savePharmacyProduct(product)
    }

    override suspend fun savePharmacyProduct(product: PharmacyProduct): Unit = dbQuery {
        PharmacyProductsTable.upsert {
            it[productId] = product.id.toUuid()
            it[workspaceId] = product.workspaceId.toUuid()
            it[genericName] = product.genericName
            it[dosageForm] = product.dosageForm
            it[strength] = product.strength
            it[requiresPrescription] = product.requiresPrescription
            it[therapeuticClass] = product.therapeuticClass
            it[fdaNumber] = product.fdaNumber
        }
    }

    private fun ResultRow.toProductSnapshot() = ProductSnapshot(
        id = this[PharmacyProductViewTable.id].toString(),
        name = this[PharmacyProductViewTable.name],
        brandName = this[PharmacyProductViewTable.brandName],
        sku = this[PharmacyProductViewTable.sku],
        barcode = this[PharmacyProductViewTable.barcode],
        unitOfMeasure = this[PharmacyProductViewTable.unitOfMeasure],
        reorderLevel = this[PharmacyProductViewTable.reorderLevel],
        totalAvailable = this[PharmacyProductViewTable.totalAvailable],
        type = this[PharmacyProductViewTable.originApp],
        workspaceId = this[PharmacyProductViewTable.workspaceId].toString(),
        currentPrice = this[PharmacyProductViewTable.currentPriceAmount]?.let {
            Money(
                it,
                this[PharmacyProductViewTable.currentPriceCurrency]!!
            )
        },
        nextExpiry = this[PharmacyProductViewTable.nextExpiry],
        versionTimestamp = this[PharmacyProductViewTable.versionTimestamp]
    )

    private fun ResultRow.toPharmacyProduct(): PharmacyProduct =
        PharmacyProduct(
            id = this[PharmacyProductViewTable.id].toString(),
            workspaceId = this[PharmacyProductViewTable.workspaceId].toString(),
            sku = this[PharmacyProductViewTable.sku],
            barcode = this[PharmacyProductViewTable.barcode],
            name = this[PharmacyProductViewTable.name],
            brandName = this[PharmacyProductViewTable.brandName],
            description = this[PharmacyProductViewTable.description],
            unitOfMeasure = this[PharmacyProductViewTable.unitOfMeasure],
            category = this[PharmacyProductViewTable.category],
            reorderLevel = this[PharmacyProductViewTable.reorderLevel],
            type = this[PharmacyProductViewTable.originApp],
            isActive = this[PharmacyProductViewTable.isActive],
            createdAt = this[PharmacyProductViewTable.createdAt],
            updatedAt = this[PharmacyProductViewTable.updatedAt],

            genericName = this[PharmacyProductViewTable.genericName],
            dosageForm = this[PharmacyProductViewTable.dosageForm],
            strength = this[PharmacyProductViewTable.strength],
            requiresPrescription = this[PharmacyProductViewTable.requiresPrescription],
            therapeuticClass = this[PharmacyProductViewTable.therapeuticClass],
            fdaNumber = this[PharmacyProductViewTable.fdaNumber]
        )
}
