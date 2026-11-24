package com.citacomsys.pharmacy.service

import com.citacomsys.api.workspace.service.ResourceCheckResult
import com.citacomsys.api.workspace.service.WorkspaceResourceManager
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.notification.AlertProcessor
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.module.server.customer.exposedTable.CustomerTable
import com.citacomsys.pharmacy.api.model.TherapeuticClassification
import com.citacomsys.pharmacy.exposedsql.PharmacyProductsTable
import com.citacomsys.sales.exposedsql.SaleItemsTable
import com.citacomsys.sales.exposedsql.SalesTable
import com.citacomsys.types.ProductModule
import com.citacomsys.types.ResourceType
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@Serializable
data class PendingRefillTargetRecord(
    val workspaceId: String,
    val saleId: String,
    val userId: String,
    val customerName: String,
    val customerPhone: String?,
    val productName: String
)

/**
 * Specialized processor for chronic medication refill reminders.
 */
class PatientRefillAlertProcessor(
    private val smsService: SmsService,
    private val resourceManager: WorkspaceResourceManager,
    private val workspaceRepository: WorkspaceRepository,
    private val clock: Clock = Clock.System
) : AlertProcessor {
    private val logger = LoggerFactory.getLogger(PatientRefillAlertProcessor::class.java)

    override val processorId: String = "PATIENT_REFILL"

    override fun shouldProcess(modules: Set<ProductModule>): Boolean {
        return modules.contains(ProductModule.CUSTOMER_CARE_SMS)
    }

    override suspend fun process(workspaceId: String) {
        val nowInstant = clock.now()
        val twentyFiveDaysAgo = nowInstant.minus(25.days)
        val startTimeWindow = Instant.fromEpochMilliseconds(twentyFiveDaysAgo.toEpochMilliseconds() - (1000 * 60 * 60 * 12))
        val endTimeWindow = Instant.fromEpochMilliseconds(twentyFiveDaysAgo.toEpochMilliseconds() + (1000 * 60 * 60 * 12))

        val pendingRefills = getPendingChronicRefills(
            workspaceId = workspaceId,
            startTimeWindow = startTimeWindow,
            endTimeWindow = endTimeWindow
        )

        if (pendingRefills.isNotEmpty()) {
            val status = resourceManager.checkAvailability(workspaceId, ResourceType.SMS)
            if (status is ResourceCheckResult.Depleted) {
                logger.warn("Workspace $workspaceId has exhausted their SMS quota. Suppressing refill alerts.")
                return
            }

            val workspace = workspaceRepository.get(workspaceId).getOrNull() ?: return

            pendingRefills.forEach { refillItem ->
                if (refillItem.customerPhone.isNullOrBlank()) return@forEach

                val message = "Hi ${refillItem.customerName}, your prescription for ${refillItem.productName} is due for a refill soon. Please reply or contact us to confirm your preparation pack."

                try {
                    smsService.send(
                        userId = SystemActors.SYSTEM_ID,
                        organizationId = workspace.organizationId,
                        workspaceId = workspaceId,
                        to = refillItem.customerPhone,
                        body = message
                    )
                } catch (e: Exception) {
                    logger.error("Failed to enqueue automated patient care refill reminder for sale ${refillItem.saleId}", e)
                }
            }
        }
    }

    private val chronicConditions = listOf(
        TherapeuticClassification.CARDIOVASCULAR.dbValue,
        TherapeuticClassification.DIABETES.dbValue
    )

    private suspend fun getPendingChronicRefills(
        workspaceId: String,
        startTimeWindow: Instant,
        endTimeWindow: Instant
    ): List<PendingRefillTargetRecord> = dbQuery {
        val tenantUuid = UUID.fromString(workspaceId)

        val relationalJoin = SalesTable
            .join(
                CustomerTable,
                JoinType.INNER,
                onColumn = SalesTable.customerId,
                otherColumn = CustomerTable.id
            )
            .join(
                SaleItemsTable,
                JoinType.INNER,
                onColumn = SalesTable.id,
                otherColumn = SaleItemsTable.saleId
            )
            .join(
                PharmacyProductsTable,
                JoinType.INNER,
                onColumn = SaleItemsTable.productId,
                otherColumn = PharmacyProductsTable.productId
            )

        relationalJoin
            .select(
                SalesTable.workspaceId,
                SalesTable.id,
                SalesTable.createdBy,
                CustomerTable.fullName,
                CustomerTable.phone,
                SaleItemsTable.productName
            )
            .where {
                val cond = (SalesTable.workspaceId eq tenantUuid) and
                        (SalesTable.createdAt greaterEq startTimeWindow) and
                        (SalesTable.createdAt lessEq endTimeWindow) and
                        (CustomerTable.isAlertsConsented eq true) and
                        (CustomerTable.phone neq "")
                
                val types = (PharmacyProductsTable.requiresPrescription eq true) or
                        (PharmacyProductsTable.therapeuticClass inList chronicConditions)
                
                cond and types
            }
            .orderBy(SalesTable.createdAt to SortOrder.ASC)
            .map { row ->
                PendingRefillTargetRecord(
                    workspaceId = row[SalesTable.workspaceId].toString(),
                    saleId = row[SalesTable.id].toString(),
                    userId = row[SalesTable.createdBy].toString(),
                    customerName = row[CustomerTable.fullName],
                    customerPhone = row[CustomerTable.phone],
                    productName = row[SaleItemsTable.productName]
                )
            }
    }
}
