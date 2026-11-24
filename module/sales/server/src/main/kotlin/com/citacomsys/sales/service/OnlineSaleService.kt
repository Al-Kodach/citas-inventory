package com.citacomsys.sales.service

import com.citacomsys.module.api.customer.CustomerLedgerRequest
import com.citacomsys.module.api.customer.CustomerService
import com.citacomsys.module.api.customer.CustomerTransactionType
import com.citacomsys.api.sales.Payment
import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.api.sales.Sale
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.SaleRepository
import com.citacomsys.api.sales.SaleStatus
import com.citacomsys.api.sales.SalesRecord
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.PreviewResponse
import com.citacomsys.api.sales.dto.PreviewSaleRequest
import com.citacomsys.api.sales.dto.SaleItemRequest
import com.citacomsys.api.sales.dto.SaleItemSnapshot
import com.citacomsys.api.sales.dto.SalePreview
import com.citacomsys.api.sales.dto.SaleResult
import com.citacomsys.api.sales.dto.SaleSnapshot
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.security.hashing.HashUtils
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.module.inventory.repository.StockMovementRepository
import com.citacomsys.sales.common.SalesComputer
import com.citacomsys.shared.error.InsufficientStockException
import com.citacomsys.types.Money
import com.citacomsys.types.generateUUID
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.ktor.server.plugins.NotFoundException
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.time.Clock.System.now
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

private val logger = KotlinLogging.logger("com.citacomsys.OnlineSaleService")

/**
 * Server-side implementation of the SaleService for real-time, online transactions.
 */
internal class OnlineSaleService(
    private val stockRepo: StockMovementRepository,
    private val saleRepo: SaleRepository,
    private val userRepo: UserPort,
    private val computer: SalesComputer,
    private val notificationService: NotificationService,
    private val workspaceRepository: WorkspaceRepository,
    private val customerService: CustomerService,
    private val auditor: TelemetryAuditor
) {

    private val quotes: LoadingCache<String, SaleSnapshot?> =
        Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build(CacheLoader { null })


    suspend fun previewSale(
        request: PreviewSaleRequest,
        workspaceId: String
    ): Result<PreviewResponse> = runCatching {
        val itemSnapshots = try {
            request.items.map {
                computer.computeItemFulfillment(workspaceId, it.productId, it.quantity, null, false)
            }
        } catch (e: InsufficientStockException) {
            return@runCatching PreviewResponse.Error(
                message = "Insufficient stock",
                details = e.details.mapValues { it.value }
            )
        }

        val liveInvHash = computer.getInventoryHash(workspaceId, request.items.map { it.productId })

        val totalAmount = itemSnapshots.fold(Money.zero()) { acc, i -> acc + i.totalAmount }
        val quoteId = generateUUID()
        val expiresAt = now().plus(120.toDuration(DurationUnit.SECONDS))

        val snapshot = SaleSnapshot(
            workspaceId = workspaceId,
            id = quoteId,
            items = itemSnapshots,
            totalAmount = totalAmount,
            totalQuantity = itemSnapshots.sumOf { it.requestedQuantity },
            expiresAt = expiresAt,
            requestHash = generateHash(request.items),
            inventoryHash = liveInvHash
        )
        quotes.put(quoteId, snapshot)

        PreviewResponse.Success(
            SalePreview(
                quoteId = quoteId,
                totalAmount = totalAmount,
                expiresAt = expiresAt,
                items = itemSnapshots
            )
        )
    }


    suspend fun createSale(
        request: CreateSaleRequest,
        workspaceId: String,
        orgId: String
    ): Result<SaleResult> {
        return runCatching {
            val quoteId = request.quoteId ?: throw IllegalArgumentException("QuoteId is required.")
            val cachedQuote =
                quotes.getIfPresent(quoteId) ?: return@runCatching SaleResult.QuoteExpired

            if (cachedQuote.workspaceId != workspaceId) return@runCatching SaleResult.InvalidContext
            if (cachedQuote.expiresAt < now()) {
                quotes.invalidate(quoteId)
                return@runCatching SaleResult.QuoteExpired
            }

            if (generateHash(request.items) != cachedQuote.requestHash) {
                return@runCatching SaleResult.QuoteMismatch
            }

            val liveInvHash =
                computer.getInventoryHash(workspaceId, request.items.map { it.productId })

            logger.debug { "Was there a change in inventory since previewInvoice? ${liveInvHash != cachedQuote.inventoryHash}" }

            val finalItems = if (cachedQuote.inventoryHash == liveInvHash) {
                cachedQuote.items
            } else {
                logger.warn { "Inventory shifted for quote $quoteId. Healing fulfillment." }
                validateAndFulfill(cachedQuote, workspaceId)
            }

            val actor = userRepo.findById(request.actorId).getOrThrow()
            val saleId = request.id

            val movements = computer.buildMovements(
                workspaceId = workspaceId,
                saleId = saleId,
                branchId = request.branchId,
                items = finalItems
            )
            stockRepo.saveAll(movements)

            val totalAmount = finalItems.fold(Money.zero()) { acc, i -> acc + i.totalAmount }
            val totalLoss = finalItems.fold(Money.zero()) { acc, i -> acc + i.priceVariance }
            val sale = Sale(
                id = saleId,
                workspaceId = workspaceId,
                customerId = request.customerId,
                totalAmount = totalAmount,
                priceVariance = totalLoss,
                branchId = request.branchId,
                totalItems = cachedQuote.totalQuantity,
                status = SaleStatus.COMPLETED,
                createdAt = now(),
                createdBy = actor.id
            )

            saleRepo.save(sale).getOrThrow()
            val saleItems = finalItems.map { it.toRecord(saleId) }
            saleRepo.saveItems(saleItems).getOrThrow()

            val payments = request.payments.map { p ->
                Payment(
                    generateUUID(),
                    saleId,
                    p.amount,
                    p.method,
                    p.reference,
                    now()
                )
            }

            saleRepo.savePayment(payments)

            // FINANCIAL LEDGER INTEGRATION: If sold on credit, record in customer ledger
            val customerId = request.customerId
            if (customerId != null) {
                // Record Entry #1: Full Credit Purchase
                request.payments.filter { it.method == PaymentMethod.CREDIT }
                    .forEach { creditPayment ->
                        customerService.recordTransaction(
                            customerId = customerId,
                            request = CustomerLedgerRequest(
                                workspaceId = workspaceId,
                                amountMinor = creditPayment.amount.amountMinor,
                                currency = creditPayment.amount.currencyCode,
                                type = CustomerTransactionType.CREDIT_PURCHASE,
                                saleId = saleId,
                                reference = creditPayment.reference,
                                note = "Credit sale #$saleId processed at POS",
                                actorId = request.actorId,
                                branchId = request.branchId,
                                actorName = request.actorName
                            )
                        )
                    }

                // Record Entry #2: Initial Payment / Upfront Deposit if present
                val initialDeposit = request.initialDeposit
                if (initialDeposit != null && initialDeposit.amount.amountMinor > 0) {
                    customerService.recordTransaction(
                        customerId = customerId,
                        request = CustomerLedgerRequest(
                            workspaceId = workspaceId,
                            amountMinor = initialDeposit.amount.amountMinor,
                            currency = initialDeposit.amount.currencyCode,
                            type = CustomerTransactionType.PAYMENT,
                            method = initialDeposit.method.name,
                            saleId = saleId,
                            reference = initialDeposit.reference,
                            note = "Upfront cash deposit paid at POS checkout (Receipt #$saleId)",
                            actorId = request.actorId,
                            branchId = request.branchId,
                            actorName = request.actorName
                        )
                    )
                }
            }

            quotes.invalidate(quoteId)

            auditor.logApplicationEvent(
                action = "ONLINE_SALE_CREATED",
                summary = "Online sale $saleId created for amount $totalAmount (Items: ${cachedQuote.totalQuantity}) by ${request.actorName}",
                entityType = "SALE",
                entityId = saleId,
                level = TelemetryLevel.INFO
            )

            if (sale.totalAmount.amountMinor >= 100000) {
                try {
                    val workspace = workspaceRepository.get(workspaceId).getOrThrow()
                    val organizationId = workspace.organizationId
                    val owner = userRepo.findOwner(organizationId)
                    if (owner != null) {
                        notificationService.sendSecurityAlert(
                            organizationId = organizationId,
                            workspaceId = workspaceId,
                            phone = owner.phone,
                            email = owner.email,
                            action = "HIGH_VALUE_SALE",
                            summary = "[SECURITY ALERT] High-value sale processed: ${sale.totalAmount.formatted} by ${actor.name}."
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to send high value notification" }
                }
            }

            SaleResult.Success(saleId)
        }
    }

    private suspend fun validateAndFulfill(
        snapshot: SaleSnapshot,
        workspaceId: String
    ): List<SaleItemSnapshot> {
        val revalidatedSnapshots = mutableListOf<SaleItemSnapshot>()
        val details = mutableMapOf<String, String>()

        for (item in snapshot.items) {
            try {
                val revalidatedItem = computer.computeItemFulfillment(
                    workspaceId = workspaceId,
                    productId = item.productId,
                    quantity = item.requestedQuantity,
                    quoteUnitPrice = null,
                    shouldLock = true
                )
                revalidatedSnapshots.add(revalidatedItem)
            } catch (e: InsufficientStockException) {
                details.putAll(e.details)
            }
        }

        if (details.isNotEmpty()) {
            throw InsufficientStockException("Insufficient stock available for fulfillment", details)
        }

        return revalidatedSnapshots
    }

    private fun generateHash(items: List<SaleItemRequest>): String {
        val payload = items.sortedBy { it.productId }
            .joinToString("|") { "${it.productId}:${it.quantity}" }
        return HashUtils.sha256(payload)
    }

    suspend fun getSales(
        workspaceId: String,
        branchId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): List<Sale> {
        return saleRepo.getSales(
            workspaceId = workspaceId,
            branchId = branchId,
            startDate = startDate,
            endDate = endDate
        )
    }

    suspend fun getSaleDetails(workspaceId: String, saleId: String): SaleDetails {
        return saleRepo.getSaleDetails(workspaceId, saleId)
    }

    suspend fun getSaleRecord(workspaceId: String, saleId: String): SalesRecord {
        return saleRepo.getSaleRecord(workspaceId, saleId) ?:
        throw NotFoundException("SalesRecord not found for $saleId")
    }
}
