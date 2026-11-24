package com.citacomsys.module.inventory

import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.core.notification.AlertProcessor
import com.citacomsys.core.notification.service.ExpiringBatchRecord
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.module.inventory.repository.StockBatchRepository
import com.citacomsys.types.ProductModule
import kotlinx.datetime.*
import kotlinx.datetime.format.*
import org.slf4j.LoggerFactory
import kotlin.time.Clock

/**
 * Specialized processor for identifying and reporting inventory batches approaching expiry.
 */
class InventoryExpiryAlertProcessor(
    private val inventoryRepo: StockBatchRepository,
    private val notificationService: NotificationService,
    private val userRepository: UserPort,
    private val workspaceRepository: WorkspaceRepository,
    private val clock: Clock = Clock.System,
) : AlertProcessor {
    private val logger = LoggerFactory.getLogger(InventoryExpiryAlertProcessor::class.java)

    override val processorId: String = "INVENTORY_EXPIRY"

    override fun shouldProcess(modules: Set<ProductModule>): Boolean {
        return modules.contains(ProductModule.AUTOMATED_ALERTS)
    }

    override suspend fun process(workspaceId: String) {
        val allExpiringBatches = inventoryRepo.getExpiringBatches(workspaceId, thresholdDays = 90)
        
        val today = clock.now().toLocalDateTime(TimeZone.UTC).date

        val criticalBatches = allExpiringBatches.mapNotNull { batch ->
            val expiryDate = batch.expiryDate ?: return@mapNotNull null
            val createdAt = batch.batchCreatedAt.toLocalDateTime(TimeZone.UTC).date

            val totalLifespan = createdAt.daysUntil(expiryDate)
            val daysRemaining = today.daysUntil(expiryDate)

            val alertThreshold = if (totalLifespan <= 180) {
                (totalLifespan * 0.2).toInt().coerceAtLeast(1)
            } else {
                90
            }

            if (daysRemaining > alertThreshold) return@mapNotNull null

            ExpiringBatchRecord(
                productName = batch.productName,
                batchNumber = batch.batchNumber,
                expiryDate = expiryDate.format(
                    LocalDate.Format {
                        monthName(MonthNames.ENGLISH_ABBREVIATED)
                        char(' ')
                        day(Padding.ZERO)
                        chars(", ")
                        year()
                    }
                ),
                availableQuantity = batch.availableQuantity,
                daysRemaining = daysRemaining
            )
        }

        if (criticalBatches.isNotEmpty()) {
            val workspace = workspaceRepository.get(workspaceId).getOrNull() ?: return
            val organizationId = workspace.organizationId

            val adminEmails = userRepository.getAdminEmails(organizationId)
            if (adminEmails.isNotEmpty()) {
                notificationService.sendEmailDigest(organizationId, adminEmails, criticalBatches)
            }

            userRepository.findOwner(organizationId)?.let { owner ->
                val summaryText = "System Warning: You have ${criticalBatches.size} inventory line(s) approaching expiry. A detailed report was sent to your email."
                notificationService.sendSecurityAlert(
                    email = owner.email,
                    phone = owner.phone,
                    organizationId = organizationId,
                    workspaceId = workspaceId,
                    action = "INVENTORY_EXPIRY_WARNING",
                    summary = summaryText
                )
            }
        }
    }
}
