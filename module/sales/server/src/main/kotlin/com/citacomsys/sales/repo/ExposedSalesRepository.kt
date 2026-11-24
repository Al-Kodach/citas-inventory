package com.citacomsys.sales.repo

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.api.sales.Payment
import com.citacomsys.api.sales.Sale
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.SaleItem
import com.citacomsys.api.sales.SaleRepository
import com.citacomsys.api.sales.SaleStatus
import com.citacomsys.api.sales.SalesRecord
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.sales.exposedsql.Payments
import com.citacomsys.sales.exposedsql.SaleItemsTable
import com.citacomsys.sales.exposedsql.SalesTable
import com.citacomsys.types.Money
import com.citacomsys.types.TopProductMetric
import com.citacomsys.types.TrendPoint
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.times
import java.util.UUID
import kotlin.time.Instant

internal class ExposedSalesRepository(
    private val quotaGuard: WorkspaceQuotaGuard
) : SaleRepository {

    override suspend fun save(sale: Sale): Result<Unit> = runCatching {
        val validatedAmount = sale.totalAmount

        SalesTable.insert {
            it[id] = sale.id.toUuid()
            it[workspaceId] = sale.workspaceId.toUuid()
            it[branchId] = sale.branchId.toUuid()
            it[customerId] = sale.customerId?.let { UUID.fromString(it) }
            it[totalAmount] = sale.totalAmount.amountMinor
            it[this.priceVariance] = sale.priceVariance.amountMinor
            it[currency] = validatedAmount.currencyCode
            it[totalItems] = sale.totalItems
            it[status] = sale.status
            it[createdAt] = sale.createdAt
            it[createdBy] = sale.createdBy.toUuid()
        }

        getById(sale.id).onFailure {
            error("Failed to save sale: Unknown error")
        }
    }

    override suspend fun saveItems(items: List<SaleItem>): Result<Unit> = runCatching {
        SaleItemsTable.batchInsert(items) { item ->
            this[SaleItemsTable.id] = item.id.toUuid()
            this[SaleItemsTable.saleId] = item.saleId.toUuid()
            this[SaleItemsTable.productId] = item.productId.toUuid()
            this[SaleItemsTable.quantity] = item.quantity
            this[SaleItemsTable.batchId] = item.batchId.toUuid()
            this[SaleItemsTable.batchNumber] = item.batchNumber
            this[SaleItemsTable.expiryDate] = item.expiryDate
            this[SaleItemsTable.productName] = item.productName
            this[SaleItemsTable.soldUnitPrice] = item.soldUnitPrice.amountMinor
            this[SaleItemsTable.soleUnitCurrencyCode] = item.soldUnitPrice.currencyCode
            this[SaleItemsTable.priceVariance] = item.priceVariance.amountMinor
        }
    }

    override suspend fun savePayment(payments: List<Payment>): Result<Unit> = runCatching {
        Payments.batchInsert(payments) { payment ->
            this[Payments.id] = payment.id.toUuid()
            this[Payments.saleId] = payment.saleId.toUuid()
            this[Payments.amount] = payment.amount.amountMinor
            this[Payments.currencyCode] = payment.amount.currencyCode
            this[Payments.method] = payment.method
            this[Payments.reference] = payment.reference
            this[Payments.createdAt] = payment.createdAt
        }
    }

    override suspend fun updateStatus(
        saleId: String,
        status: SaleStatus
    ): Result<Unit> = runCatching {
        SalesTable.update({ SalesTable.id eq saleId.toUuid() }) {
            it[SalesTable.status] = status
        }
    }

    override suspend fun getById(saleId: String): Result<Sale> = runCatching {
        SalesTable.selectAll()
            .where { SalesTable.id eq saleId.toUuid() }
            .map { it.toSale() }
            .singleOrNull() ?: throw NotFoundException("Sale not found")
    }


    override suspend fun geSales(workspaceId: String): List<Sale> {
        val window = quotaGuard.calculateActiveSalesWindow(workspaceId)

        val query = SalesTable.selectAll()
            .where { SalesTable.workspaceId eq workspaceId.toUuid() }
            
        return query
            .andWhere {
                if (window.isUnrestricted || window.oldestAllowedInstant == null) {
                    Op.TRUE
                } else {
                    SalesTable.createdAt greaterEq window.oldestAllowedInstant!!
                }
            }
            .map { it.toSale() }
    }

    override suspend fun getSales(
        workspaceId: String,
        branchId: String?,
        startDate: Instant?,
        endDate: Instant?
    ): List<Sale> {
        val window = quotaGuard.calculateActiveSalesWindow(workspaceId)

        val query = SalesTable.selectAll()
            .where { SalesTable.workspaceId eq workspaceId.toUuid() }
            
        return query
            .andWhere {
                if (window.isUnrestricted || window.oldestAllowedInstant == null) {
                    Op.TRUE
                } else {
                    SalesTable.createdAt greaterEq window.oldestAllowedInstant!!
                }
            }
            .andWhere {
                if (branchId != null) {
                    SalesTable.branchId eq branchId.toUuid()
                } else {
                    Op.TRUE
                }
            }
            .andWhere {
                if (startDate != null) {
                    SalesTable.createdAt.greaterEq(startDate)
                } else {
                    Op.TRUE
                }
            }
            .andWhere {
                if (endDate != null) {
                    SalesTable.createdAt.lessEq(endDate)
                } else {
                    Op.TRUE
                }
            }
            .map { it.toSale() }
    }

    override suspend fun getSaleDetails(
        saleId: String,
        workspaceId: String
    ): SaleDetails {
        val sale = getById(saleId).getOrThrow()

        val items = SaleItemsTable.selectAll()
            .where { SaleItemsTable.saleId eq saleId.toUuid() }

        val payments = Payments.selectAll()
            .where { Payments.saleId eq saleId.toUuid() }

        return SaleDetails(
            sale = sale,
            items = items.map { it.toDetailsSaleItem() },
            payments = payments.map { it.toPayment() }
        )
    }

    override suspend fun existsById(saleId: String): Boolean {
        return SalesTable.selectAll().where { SalesTable.id eq saleId.toUuid() }.count() > 0
    }

    override suspend fun getSaleRecord(
        saleId: String,
        workspaceId: String
    ): SalesRecord? {
        val sale = SalesTable.selectAll()
            .where { SalesTable.id eq saleId.toUuid() }
            .map { it.toSale() }
            .singleOrNull() ?: return null

        val items = SaleItemsTable.selectAll()
            .where { SaleItemsTable.saleId eq saleId.toUuid() }
            .map { it.toSaleItem() }

        val payments = Payments.selectAll()
            .where { Payments.saleId eq saleId.toUuid() }
            .map { it.toPayment() }

        return SalesRecord(
            sale = sale,
            items = items,
            payments = payments
        )
    }

    override suspend fun getTopSellingProducts(
        workspaceId: String,
        branchId: String?,
        startDate: Instant?,
        endDate: Instant?,
        limit: Int
    ): List<TopProductMetric> = dbQuery {
        val quantitySum = SaleItemsTable.quantity.sum()
        val totalRevenue =
            (SaleItemsTable.quantity.castTo(LongColumnType()) * SaleItemsTable.soldUnitPrice).sum()

        SalesTable.innerJoin(SaleItemsTable)
            .select(
                SaleItemsTable.productId,
                SaleItemsTable.productName,
                quantitySum,
                totalRevenue
            )
            .where { (SalesTable.workspaceId eq workspaceId.toUuid()) and (SalesTable.status eq SaleStatus.COMPLETED) }
            .andWhere {
                if (branchId != null) SalesTable.branchId eq branchId.toUuid() else Op.TRUE
            }
            .andWhere {
                if (startDate != null) SalesTable.createdAt greaterEq startDate else Op.TRUE
            }
            .andWhere {
                if (endDate != null) SalesTable.createdAt lessEq endDate else Op.TRUE
            }
            .groupBy(SaleItemsTable.productId, SaleItemsTable.productName)
            .orderBy(totalRevenue, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                TopProductMetric(
                    productId = row[SaleItemsTable.productId].toString(),
                    productName = row[SaleItemsTable.productName],
                    totalRevenueMinor = row[totalRevenue] ?: 0L,
                    unitsSold = row[quantitySum]?.toLong() ?: 0L
                )
            }
    }

    override suspend fun getRevenueTrend(
        workspaceId: String,
        branchId: String?,
        startDate: Instant?,
        endDate: Instant?
    ): List<TrendPoint> = dbQuery {
        val totalSum = SalesTable.totalAmount.sum()

        SalesTable
            .select(SalesTable.createdAt, totalSum)
            .where { (SalesTable.workspaceId eq workspaceId.toUuid()) and (SalesTable.status eq SaleStatus.COMPLETED) }
            .andWhere {
                if (branchId != null) SalesTable.branchId eq branchId.toUuid() else Op.TRUE
            }
            .andWhere {
                if (startDate != null) SalesTable.createdAt greaterEq startDate else Op.TRUE
            }
            .andWhere {
                if (endDate != null) SalesTable.createdAt lessEq endDate else Op.TRUE
            }
            .groupBy(SalesTable.createdAt)
            .orderBy(SalesTable.createdAt, SortOrder.ASC)
            .map { row ->
                val time = row[SalesTable.createdAt]
                TrendPoint(
                    label = time.toString().take(10), 
                    value = (row[totalSum] ?: 0L).toDouble(),
                    timestamp = time
                )
            }
    }

    private fun ResultRow.toSale(): Sale {
        return Sale(
            id = this[SalesTable.id].toString(),
            workspaceId = this[SalesTable.workspaceId].toString(),
            branchId = this[SalesTable.branchId].toString(),
            customerId = this[SalesTable.customerId]?.toString(),
            totalAmount = Money(
                this[SalesTable.totalAmount],
                currencyCode = this[SalesTable.currency]
            ),
            priceVariance = Money(
                this[SalesTable.priceVariance],
                currencyCode = this[SalesTable.currency]
            ),
            totalItems = this[SalesTable.totalItems],
            status = this[SalesTable.status],
            createdAt = this[SalesTable.createdAt],
            createdBy = this[SalesTable.createdBy].toString(),
        )
    }

    private fun ResultRow.toDetailsSaleItem(): SaleDetails.Item {
        return SaleDetails.Item(
            productId = this[SaleItemsTable.productId].toString(),
            productName = this[SaleItemsTable.productName],
            batchId = this[SaleItemsTable.batchId].toString(),
            quantity = this[SaleItemsTable.quantity],
            batchNumber = this[SaleItemsTable.batchNumber],
            expiryDate = this[SaleItemsTable.expiryDate],
            unitPrice = Money(
                this[SaleItemsTable.soldUnitPrice],
                currencyCode = this[SaleItemsTable.soleUnitCurrencyCode]
            )
        )
    }

    private fun ResultRow.toSaleItem(): SaleItem {
        return SaleItem(
            id = this[SaleItemsTable.id].toString(),
            saleId = this[SaleItemsTable.saleId].toString(),
            productId = this[SaleItemsTable.productId].toString(),
            batchId = this[SaleItemsTable.batchId].toString(),
            quantity = this[SaleItemsTable.quantity],
            productName = this[SaleItemsTable.productName],
            batchNumber = this[SaleItemsTable.batchNumber],
            expiryDate = this[SaleItemsTable.expiryDate],
            soldUnitPrice = Money(
                this[SaleItemsTable.soldUnitPrice],
                currencyCode = this[SaleItemsTable.soleUnitCurrencyCode]
            ),
            priceVariance = Money(
                this[SaleItemsTable.priceVariance],
                currencyCode = this[SaleItemsTable.soleUnitCurrencyCode]

            )
        )
    }

    private fun ResultRow.toPayment(): Payment {
        return Payment(
            id = this[Payments.id].toString(),
            saleId = this[Payments.saleId].toString(),
            amount = Money(
                this[Payments.amount],
                currencyCode = this[Payments.currencyCode]
            ),
            method = this[Payments.method],
            reference = this[Payments.reference],
            createdAt = this[Payments.createdAt]
        )
    }
}
