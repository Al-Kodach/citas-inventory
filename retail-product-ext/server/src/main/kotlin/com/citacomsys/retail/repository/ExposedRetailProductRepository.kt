package com.citacomsys.retail.repository

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.persistence.table.inventory.BaseProductsTable
import com.citacomsys.retail.exposedsql.RetailProductsTable
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.retail.api.RetailProductRepository
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.types.ProductType
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

internal class ExposedRetailProductRepository(
    private val baseRepo: ProductRepository,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ProductRepository by baseRepo, RetailProductRepository {

    override suspend fun getRetailProduct(id: String, workspaceId: String): RetailProduct? = dbQuery {
        val base = BaseProductsTable.selectAll()
            .where {
                (BaseProductsTable.id eq id.toUuid()) and
                        (BaseProductsTable.workspaceId eq workspaceId.toUuid()) and
                        (BaseProductsTable.productType eq ProductType.RETAIL)
            }
            .singleOrNull() ?: return@dbQuery null

        val ext = RetailProductsTable.selectAll()
            .where { RetailProductsTable.productId eq id.toUuid() }
            .singleOrNull()

        mapToRetailProduct(base, ext)
    }

    override suspend fun saveRetailProduct(product: RetailProduct): Unit = dbQuery {
        saveBaseIdentity(product)
        RetailProductsTable.upsert {
            it[productId] = product.id.toUuid()
            it[workspaceId] = product.workspaceId.toUuid()
            it[tags] = json.encodeToString(product.tags)
            it[attributes] = json.encodeToString(product.attributes)
        }
    }

    private fun mapToRetailProduct(base: ResultRow, ext: ResultRow?): RetailProduct {
        return RetailProduct(
            id = base[BaseProductsTable.id].toString(),
            workspaceId = base[BaseProductsTable.workspaceId].toString(),
            name = base[BaseProductsTable.name],
            sku = base[BaseProductsTable.sku],
            barcode = base[BaseProductsTable.barcode],
            category = base[BaseProductsTable.category],
            reorderLevel = base[BaseProductsTable.reorderLevel],
            brandName = base[BaseProductsTable.brandName],
            description = base[BaseProductsTable.description],
            unitOfMeasure = base[BaseProductsTable.unitOfMeasure],
            type = base[BaseProductsTable.productType],
            isActive = base[BaseProductsTable.isActive],
            createdAt = base[BaseProductsTable.createdAt],
            updatedAt = base[BaseProductsTable.updatedAt],
            tags = ext?.get(RetailProductsTable.tags)?.let { json.decodeFromString(it) } ?: emptyList(),
            attributes = ext?.get(RetailProductsTable.attributes)?.let { json.decodeFromString(it) } ?: emptyMap()
        )
    }
}
