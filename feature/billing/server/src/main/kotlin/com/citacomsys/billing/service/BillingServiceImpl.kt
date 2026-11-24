package com.citacomsys.billing.service

import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.BillingAction
import com.citacomsys.api.billing.BillingQuote
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.BillingRepository
import com.citacomsys.api.billing.PayloadType
import com.citacomsys.api.billing.PaymentChannel
import com.citacomsys.api.billing.PaymentSubmission
import com.citacomsys.api.billing.SmsTopupBillingPayload
import com.citacomsys.api.billing.SubscriptionBillingPayload
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.platform.uiModel.DiscountUiState
import com.citacomsys.api.platform.uiModel.InvoiceLineUiState
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.platform.uiModel.TaxUiState
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.billing.engine.BillingExpiryPolicy
import com.citacomsys.billing.engine.BillingJobState
import com.citacomsys.billing.engine.BillingReferenceGenerator
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.BackgroundJobQueue
import com.citacomsys.core.notification.EnqueueRequest
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.notification.service.SseService
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.shared.displayName
import com.citacomsys.shared.event.BillingEvent
import com.citacomsys.shared.event.SseMessage
import com.citacomsys.types.BillingOrigin
import com.citacomsys.types.BillingReason
import com.citacomsys.types.BillingStatus
import com.citacomsys.types.BillingType
import com.citacomsys.types.EntityType
import com.citacomsys.types.PriceLineItem
import com.citacomsys.types.SubscriptionStatus
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

internal const val RENEWAL_WINDOW_DAYS = 14

internal class BillingServiceImpl(
    private val quoteService: BillingQuoteService,
    private val billingRepository: BillingRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val userRepository: UserPort,
    private val auditor: TelemetryAuditor,
    private val notificationService: NotificationService,
    private val sseService: SseService,
    private val backgroundJobQueue: BackgroundJobQueue,
    private val clock: Clock = Clock.System
) : BillingService {
    private val logger = KotlinLogging.logger { }

    private val paymentTargetKey = "MOMO_COLLECTION_NUMBER"
    private val paymentRecipientKey = "MOMO_COLLECTION_NAME"

    private fun resolveLivePaymentInfo(): Pair<String, String> {
        val target = System.getProperty(paymentTargetKey)
            ?: System.getenv(paymentTargetKey)
            ?: "+233208169630"

        val recipient = System.getProperty(paymentRecipientKey)
            ?: System.getenv(paymentRecipientKey)
            ?: "Citacom Systems LTD"

        return target to recipient
    }

    private fun BillingRecord.withLivePaymentInfo(): BillingRecord {
        val (liveTarget, liveRecipient) = resolveLivePaymentInfo()
        return this.copy(
            paymentTarget = liveTarget,
            paymentRecipientName = liveRecipient
        )
    }

    private suspend fun generateInvoice(
        workspaceId: String,
        createdBy: String,
        type: BillingType,
        origin: BillingOrigin,
        reason: BillingReason,
        channel: PaymentChannel,
        quote: BillingQuote,
        expiresAt: Instant,
    ): BillingRecord {
        val subscription = workspaceRepository.getSubscription(workspaceId)
        val workspace = workspaceRepository.get(workspaceId).getOrThrow()
        val (liveTarget, liveRecipient) = resolveLivePaymentInfo()

        val existing = billingRepository.findPendingSubscriptionInvoice(
            workspaceId = workspaceId,
            version = subscription.subscriptionVersion,
            type = type
        )
        if (existing != null) return existing.withLivePaymentInfo()

        val record = BillingRecord(
            id = UUID.randomUUID().toString(),
            workspaceId = workspaceId,
            wsName = workspace.name,
            type = type,
            paymentTarget = liveTarget,
            paymentRecipientName = liveRecipient,
            amount = quote.total,
            referenceCode = BillingReferenceGenerator.generate(),
            status = BillingStatus.PENDING,
            origin = origin,
            reason = reason,
            quote = quote,
            paymentChannel = channel,
            summary = quote.description,
            subscriptionVersion = subscription.subscriptionVersion,
            createdBy = createdBy,
            createdAt = clock.now(),
            updatedAt = clock.now(),
            expiresAt = expiresAt
        )

        billingRepository.save(record)

        auditor.logApplicationEvent(
            action = "INVOICE_GENERATED",
            entityType = EntityType.PACKAGE.name,
            entityId = record.id,
            summary = "Invoice ${record.referenceCode} (${type.displayName()}) generated for ${workspace.name}",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = workspaceId,
            organizationIdOverride = workspace.organizationId
        )
        return record
    }

    override suspend fun invoicePreviewRequest(request: PreviewRequest): InvoicePreviewUiState {
        val quote = when (request) {
            is PreviewRequest.Subscription -> quoteService.generate(
                PkgProvisionRequest(
                    request.packageId,
                    request.billTermId
                )
            )

            is PreviewRequest.SmsTopup -> quoteService.generateSmsTopupQuote(request.pkgId)
        }

        val lineItems = when (val p = quote.payload) {
            is SubscriptionBillingPayload -> p.provisionedPackage.lineItems
            is SmsTopupBillingPayload -> p.resourcePriceBreakdown.lineItems
        }

        val validity = quote.promotionExpiresAt

        return buildPreview(quote, lineItems).copy(validUntil = validity)
    }

    override suspend fun ensureOnboardingInvoice(workspaceId: String): BillingRecord? {
        val subscription = workspaceRepository.getSubscription(workspaceId)

        if (subscription.status != SubscriptionStatus.TRIAL) return null
        val trialExpiry = subscription.endedAt
        if (trialExpiry < clock.now()) return null

        val quote = quoteService.generateRenewalQuote(workspaceId)

        return generateInvoice(
            workspaceId = workspaceId,
            createdBy = "SYSTEM",
            type = BillingType.TRIAL_CONVERSION,
            origin = BillingOrigin.SYSTEM_GENERATED,
            reason = BillingReason.TRIAL_CONVERSION,
            channel = PaymentChannel.MOBILE_MONEY,
            quote = quote,
            expiresAt = trialExpiry
        )
    }

    override suspend fun ensureRenewalInvoice(workspaceId: String): BillingRecord? = dbQuery {
        val sub = workspaceRepository.getSubscription(workspaceId)
        if (sub.status == SubscriptionStatus.TRIAL) return@dbQuery null

        val now = clock.now()
        val expiry = sub.endedAt
        val daysToExpiry = (expiry - now).inWholeDays

        if (daysToExpiry <= RENEWAL_WINDOW_DAYS) {
            val quote = quoteService.generateRenewalQuote(workspaceId)

            val invoice = generateInvoice(
                workspaceId = workspaceId,
                createdBy = "SYSTEM",
                type = BillingType.SUBSCRIPTION_RENEWAL,
                origin = BillingOrigin.SYSTEM_GENERATED,
                reason = BillingReason.SUBSCRIPTION_RENEWAL,
                channel = PaymentChannel.MOBILE_MONEY,
                quote = quote,
                expiresAt = now + BillingExpiryPolicy.RENEWAL_VALIDITY
            )

            sendNotificationToOwner(
                "Reminder: Subscription expires in $daysToExpiry days. Ref: ${invoice.referenceCode}. Amount: ${invoice.amount.formatted}",
                workspaceId
            )
            return@dbQuery invoice
        }
        null
    }

    override suspend fun createRenewalInvoice(
        workspaceId: String,
        userId: String,
        sessionId: String
    ): BillingRecord =
        generateInvoice(
            workspaceId = workspaceId,
            createdBy = userId,
            type = BillingType.SUBSCRIPTION_RENEWAL,
            origin = BillingOrigin.USER_REQUEST,
            reason = BillingReason.SUBSCRIPTION_RENEWAL,
            channel = PaymentChannel.MOBILE_MONEY,
            quote = quoteService.generateRenewalQuote(workspaceId),
            expiresAt = clock.now() + BillingExpiryPolicy.RENEWAL_VALIDITY
        )

    override suspend fun createUpgradeInvoice(
        workspaceId: String,
        request: PkgProvisionRequest,
        userId: String,
        sessionId: String?
    ): BillingRecord =
        generateInvoice(
            workspaceId = workspaceId,
            createdBy = userId,
            type = BillingType.SUBSCRIPTION_UPGRADE,
            origin = BillingOrigin.USER_REQUEST,
            reason = BillingReason.PACKAGE_UPGRADE,
            channel = PaymentChannel.MOBILE_MONEY,
            quote = quoteService.generate(request),
            expiresAt = clock.now() + BillingExpiryPolicy.UPGRADE_VALIDITY
        )

    override suspend fun fetch(): List<BillInvoice> = billingRepository.fetch().map {
        BillInvoice(
            id = it.id,
            workspaceId = it.workspaceId,
            tenantName = it.wsName,
            paymentTarget = it.paymentTarget,
            paymentRecipientName = it.paymentRecipientName,
            type = it.type,
            status = it.status,
            origin = it.origin,
            amount = it.amount,
            referenceCode = it.referenceCode,
            paymentChannel = it.paymentChannel,
            summary = it.summary,
        )
    }

    private fun buildPreview(
        quote: BillingQuote,
        lineItems: List<PriceLineItem>,
    ): InvoicePreviewUiState {
        val discounts = quote.discounts.map { DiscountUiState(it.description, it.amount.formatted) }
        val taxes = quote.taxes.map { TaxUiState(it.name, it.amount.formatted) }

        val uiLineItems = lineItems.map {
            InvoiceLineUiState(
                description = it.description,
                quantity = it.quantity,
                unitPrice = it.unitPriceAmount.formatted,
                totalAmount = it.totalAmount.formatted
            )
        }

        return InvoicePreviewUiState(
            lineItems = uiLineItems,
            title = quote.title,
            taxes = taxes,
            discounts = discounts,
            subtotal = quote.subtotal.formatted,
            total = quote.total.formatted
        )
    }

    override suspend fun get(id: String): BillingRecord =
        billingRepository.find(id) ?: error("BillRecord not found")

    override suspend fun createTrailConventionInvoice(
        workspaceId: String,
        provisionedPackage: ProvisionedPackage
    ) {
        val quote = quoteService.generatePackageQuote(
            provisionedPackage,
            type = BillingType.TRIAL_CONVERSION,
            title = "Trial Conversion",
            payloadType = PayloadType.RENEWAL
        )

        val sub = workspaceRepository.getSubscription(workspaceId)

        generateInvoice(
            workspaceId = workspaceId,
            createdBy = "SYSTEM",
            type = BillingType.TRIAL_CONVERSION,
            origin = BillingOrigin.SYSTEM_GENERATED,
            reason = BillingReason.TRIAL_CONVERSION,
            channel = PaymentChannel.MOBILE_MONEY,
            quote = quote,
            expiresAt = sub.endedAt
        )
    }

    override suspend fun createSmsTopupInvoice(
        workspaceId: String,
        packageId: String,
        userId: String,
        sessionId: String
    ): BillingRecord {
        val quote = quoteService.generateSmsTopupQuote(packageId)
        return generateInvoice(
            workspaceId = workspaceId,
            createdBy = userId,
            type = BillingType.SMS_TOPUP,
            channel = PaymentChannel.MOBILE_MONEY,
            origin = BillingOrigin.USER_REQUEST,
            reason = BillingReason.SMS_TOPUP,
            quote = quote,
            expiresAt = clock.now() + BillingExpiryPolicy.SMS_TOPUP_VALIDITY
        )
    }

    override suspend fun getPaymentHistory(workspaceId: String): List<TransactionHistory> =
        dbQuery {
            billingRepository.findByTenant(workspaceId).sortedByDescending { it.createdAt }
                .map {
                    TransactionHistory(
                        id = it.id,
                        workspaceId = it.workspaceId,
                        referenceCode = it.referenceCode,
                        type = it.type,
                        status = it.status,
                        amount = it.amount,
                        summary = it.summary,
                        createdAt = it.createdAt
                    )
                }
        }

    override suspend fun paymentSubmitted(
        info: PaymentSubmission,
        workspaceId: String,
        userId: String,
        sessionId: String
    ) {
        val invoice = billingRepository.find(info.invoiceId) ?: run {
            auditor.logApplicationEvent(
                action = BillingAction.BILLING_PAYMENT_SUBMITTED.name,
                entityType = EntityType.PACKAGE.name,
                entityId = info.invoiceId,
                summary = "Failed payment submission: Invoice not found",
                level = TelemetryLevel.ERROR
            )
            error("No billing request found for id: ${info.invoiceId}")
        }

        if (invoice.status != BillingStatus.PENDING) error("Invoice is in status ${invoice.status}, cannot accept payment submission.")

        if (invoice.expiresAt?.let { it < clock.now() } == true) {
            auditor.logApplicationEvent(
                action = BillingAction.BILLING_PAYMENT_SUBMITTED.name,
                entityType = EntityType.PACKAGE.name,
                entityId = invoice.id,
                summary = "Payment submission rejected: Invoice ${invoice.referenceCode} has expired",
                level = TelemetryLevel.WARNING
            )
            throw IllegalStateException("Invoice has expired")
        }

        billingRepository.saveSubmission(info.copy(invoiceId = invoice.id))

        auditor.logApplicationEvent(
            action = BillingAction.BILLING_PAYMENT_SUBMITTED.name,
            entityType = EntityType.PACKAGE.name,
            entityId = invoice.id,
            summary = "Payment submitted for ${invoice.referenceCode} by user $userId. Transaction ID: ${info.transactionId}",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = workspaceId
        )

        sendNotificationToOwner(
            "Payment submitted for ${invoice.referenceCode}. We will verify shortly.",
            workspaceId
        )
    }

    override suspend fun rejectInvoice(
        invoiceId: String,
        reason: String,
        sessionId: String
    ): Result<Unit> = runCatching {
        val record = billingRepository.find(invoiceId) ?: error("Invoice not found")

        dbQuery {
            billingRepository.save(
                record.copy(
                    status = BillingStatus.REJECTED,
                    summary = "Rejected: $reason",
                    updatedAt = clock.now()
                )
            )

            auditor.logApplicationEvent(
                action = BillingAction.BILLING_REJECTED.name,
                entityType = EntityType.PACKAGE.name,
                entityId = record.id,
                summary = "Invoice ${record.referenceCode} rejected. Reason: $reason",
                level = TelemetryLevel.WARNING
            )

            sendNotificationToOwner(
                "Your payment for ${record.referenceCode} was rejected. Reason: $reason.",
                record.workspaceId
            )
            broadcastBillingUpdate(record.workspaceId, record.id, BillingStatus.REJECTED)
        }
    }

    override suspend fun getActiveInvoices(workspaceId: String): List<BillingRecord> = dbQuery {
        ensureRenewalInvoice(workspaceId)
        billingRepository.findActiveByTenant(workspaceId).map { it.withLivePaymentInfo() }
    }

    override suspend fun getInvoiceById(
        invoiceId: String,
        workspaceId: String
    ): BillingRecord {
        return billingRepository.find(invoiceId) ?: error("Invoice not found")
    }

    override suspend fun cancelInvoice(
        invoiceId: String,
        workspaceId: String,
        sessionId: String,
        reason: String
    ): Result<Unit> = dbQuery {
        val record = billingRepository.find(invoiceId)
            ?: return@dbQuery Result.failure(Exception("Invoice not found"))

        if (record.workspaceId != workspaceId) return@dbQuery Result.failure(Exception("Unauthorized"))
        if (record.origin != BillingOrigin.USER_REQUEST) return@dbQuery Result.failure(Exception("Only user-initiated invoices can be canceled."))
        if (record.status != BillingStatus.PENDING) return@dbQuery Result.failure(Exception("Only pending invoices can be canceled"))

        billingRepository.updateStatus(invoiceId, BillingStatus.CANCELED)

        auditor.logApplicationEvent(
            action = BillingAction.BILLING_CANCELED.name,
            entityType = EntityType.PACKAGE.name,
            entityId = invoiceId,
            summary = "Invoice ${record.referenceCode} canceled by user. Reason: $reason",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = workspaceId
        )

        Result.success(Unit)
    }

    override suspend fun verify(
        reference: String,
        userId: String,
        sessionId: String
    ): Result<Unit> = runCatching {
        val invoice = billingRepository.findByReference(reference) ?: error("Invoice not found")
        if (invoice.status in setOf(
                BillingStatus.FULFILLED,
                BillingStatus.PAID
            )
        ) return Result.success(Unit)

        billingRepository.save(
            invoice.copy(
                verifiedBy = userId,
                status = BillingStatus.PAID,
                updatedAt = clock.now()
            )
        )

        backgroundJobQueue.enqueue(
            EnqueueRequest(
                serviceId,
                payload = Json.encodeToString(BillingJobState.PendingFulfillment(invoiceId = invoice.id))
            )
        )

        auditor.logApplicationEvent(
            action = BillingAction.BILLING_VERIFIED.name,
            entityType = EntityType.PACKAGE.name,
            entityId = invoice.id,
            summary = "Payment for ${invoice.referenceCode} verified by $userId.",
            level = TelemetryLevel.INFO,
            workspaceIdOverride = invoice.workspaceId
        )

        sendNotificationToOwner(
            "Payment verified for ${invoice.referenceCode}. Resources will be updated shortly.",
            invoice.workspaceId
        )
    }

    private suspend fun sendNotificationToOwner(message: String, workspaceId: String) {
        val workspace = workspaceRepository.get(workspaceId).getOrThrow()
        userRepository.findOwner(workspace.organizationId)?.let { owner ->
            notificationService.sendManagement(
                organizationId = workspace.organizationId,
                workspaceId = workspaceId,
                phone = owner.phone,
                email = owner.email,
                userId = owner.id,
                message = message
            )
        }
    }

    private suspend fun broadcastBillingUpdate(
        workspaceId: String,
        invoiceId: String,
        status: BillingStatus
    ) {
        val workspace = runCatching { workspaceRepository.get(workspaceId).getOrNull() }.getOrNull()
        val orgId = workspace?.organizationId ?: SystemActors.SYSTEM_ID

        val billingMessage = SseMessage(
            event = BillingEvent.name,
            data = Json.encodeToString(BillingEvent(workspaceId)),
            topic = "BILLING",
            organizationId = orgId,
            workspaceId = workspaceId
        )
        sseService.broadcast(billingMessage)
    }
}