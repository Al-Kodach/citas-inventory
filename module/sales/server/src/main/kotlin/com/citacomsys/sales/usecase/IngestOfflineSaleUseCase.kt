package com.citacomsys.sales.usecase

import com.citacomsys.api.sales.Payment
import com.citacomsys.api.sales.Sale
import com.citacomsys.api.sales.SaleItem
import com.citacomsys.api.sales.SaleRepository
import com.citacomsys.api.sales.SaleStatus
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.module.inventory.repository.StockMovementRepository
import com.citacomsys.sales.common.SalesComputer
import com.citacomsys.shared.sync.SyncApiResponse
import com.citacomsys.shared.sync.SyncFailure
import com.citacomsys.types.Money
import com.citacomsys.types.generateUUID
import mu.KotlinLogging
import kotlin.time.Clock.System.now

private val logger = KotlinLogging.logger {}

/**
 * Handles the atomic ingestion of sales created on the client while offline.
 */
internal class IngestOfflineSaleUseCase(
    private val saleRepo: SaleRepository,
    private val stockRepo: StockMovementRepository,
    private val userRepo: UserPort,
    private val computer: SalesComputer,
    private val auditor: TelemetryAuditor
) {

    suspend fun execute(
        sale: CreateSaleRequest,
        workspaceId: String
    ): SyncApiResponse {
        val succeededIds = mutableListOf<String>()
        val failures = mutableListOf<SyncFailure>()

        val result = processSingleRequest(sale, workspaceId)
        result.onSuccess {
            succeededIds.add(it)
        }
        result.onFailure {
            failures.add(SyncFailure(sale.id, it.message ?: "Unknown Error"))
            auditor.logApplicationEvent(
                action = "OFFLINE_SALE_INGESTION_FAILED",
                summary = "Failed to ingest offline sale ${sale.id}: ${it.message}",
                entityType = "SALE",
                entityId = sale.id,
                level = TelemetryLevel.ERROR
            )
        }

        return SyncApiResponse(succeededIds, failures)
    }

    private suspend fun processSingleRequest(
        request: CreateSaleRequest,
        workspaceId: String
    ): Result<String> = runCatching {
        if (saleRepo.existsById(request.id)) {
            return@runCatching request.id
        }

        dbQuery {
            val attendant = userRepo.findById(request.actorId).getOrNull()

            val finalSnapshots = request.items.map { item ->
                computer.computeItemFulfillment(
                    workspaceId = workspaceId,
                    productId = item.productId,
                    quantity = item.quantity,
                    quoteUnitPrice = item.cachedUnitPrice ?: throw Exception("Missing price"),
                    shouldLock = true
                )
            }

            val totalAmount = finalSnapshots.fold(Money.zero()) { acc, i -> acc + i.totalAmount }
            val totalVariance =
                finalSnapshots.fold(Money.zero()) { acc, i -> acc + i.priceVariance }
            val totalQuantity = finalSnapshots.sumOf { it.fulfilledQuantity }

            val allMovements = computer.buildMovements(
                workspaceId = workspaceId,
                saleId = request.id,
                branchId = request.branchId,
                items = finalSnapshots
            )
            stockRepo.saveAll(allMovements)

            saleRepo.save(
                Sale(
                    id = request.id,
                    workspaceId = workspaceId,
                    totalAmount = totalAmount,
                    branchId = request.branchId,
                    priceVariance = totalVariance,
                    totalItems = totalQuantity,
                    status = SaleStatus.COMPLETED,
                    createdAt = request.createdAt ?: now(),
                    createdBy = attendant?.id ?: "offline",
                )
            )

            val saleItems = finalSnapshots.flatMap { comp ->
                comp.batches.map { sold ->
                    val individualBatchVariance =
                        (sold.unitPrice - sold.expectedCostPrice) * sold.quantity

                    SaleItem(
                        id = generateUUID(),
                        saleId = request.id,
                        productId = comp.productId,
                        productName = comp.productName,
                        batchId = sold.batchId,
                        batchNumber = sold.batchNumber,
                        quantity = sold.quantity,
                        soldUnitPrice = sold.unitPrice,
                        priceVariance = individualBatchVariance,
                        expiryDate = sold.expiryDate
                    )
                }
            }
            saleRepo.saveItems(saleItems).getOrThrow()

            val payments = request.payments.map {
                Payment(
                    id = generateUUID(),
                    saleId = request.id,
                    amount = it.amount,
                    method = it.method,
                    reference = it.reference,
                    createdAt = now()
                )
            }
            saleRepo.savePayment(payments)

            auditor.logApplicationEvent(
                action = "OFFLINE_SALE_INGESTED",
                summary = "Offline sale ${request.id} ingested. Total: $totalAmount, Items: $totalQuantity, Attendant: ${request.actorName}",
                entityType = "SALE",
                entityId = request.id,
                level = TelemetryLevel.INFO
            )

            request.id
        }
    }
}
