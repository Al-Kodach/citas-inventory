package com.citacomsys.module.inventory

import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.module.inventory.repository.StockBatchRepository
import com.citacomsys.types.BatchStockSnapshot
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

class AlertProcessingEngineTest {

    private val inventoryRepo = mockk<StockBatchRepository>()
    private val notificationService = mockk<NotificationService>()
    private val userRepository = mockk<UserRepository>()
    
    // Subclass for testing protected methods
    private class TestAlertEngine(
        inventoryRepo: StockBatchRepository,
        notificationService: NotificationService,
        userRepository: UserRepository
    ) : AlertProcessingEngine(inventoryRepo, notificationService, userRepository) {
        suspend fun testProcessTenantExpiryAlerts(organizationId: String) {
            processTenantExpiryAlerts(workspaceId)
        }
    }

    private val engine = TestAlertEngine(inventoryRepo, notificationService, userRepository)

    @Test
    fun `processTenantExpiryAlerts dispatches alerts for critical batches`() = runTest {
        val workspaceId = "workspace-1"
        val today = Clock.System.now()
        val expiryDate = today.toLocalDateTime(TimeZone.UTC).date // Expiring today

        val mockBatch = BatchStockSnapshot(
            batchId = "b1",
            batchNumber = "B123",
            branchId = "branch-1",
            productId = "p1",
            productName = "Aspirin",
            brandName = "Bayer",
            unitOfMeasure = "Bottle",
            availableQuantity = 100,
            initialQuantity = 100,
            unitPrice = com.citacomsys.types.Money(100L),
            expiryDate = expiryDate,
            batchCreatedAt = today.minus(kotlin.time.Duration.parse("100d")),
            versionTimestamp = today
        )

        coEvery { inventoryRepo.getExpiringBatches(workspaceId, 90) } returns listOf(mockBatch)
        coEvery { userRepository.getAdminEmails(workspaceId) } returns listOf("admin@test.com")
        coEvery { userRepository.findOwner(workspaceId) } returns mockk(relaxed = true) {
            every { phone } returns com.citacomsys.types.PhoneNumber("0240000000")
            every { id } returns "owner-1"
        }
        coEvery { notificationService.sendEmailDigest(workspaceId, any(), any()) } returns true
        coEvery { notificationService.sendManagementSms(workspaceId, any(), any(), any()) } returns true

        engine.testProcessTenantExpiryAlerts(workspaceId)

        coVerify {
            notificationService.sendEmailDigest(workspaceId, listOf("admin@test.com"), any())
            notificationService.sendManagementSms(workspaceId, "0240000000", "owner-1", any())
        }
    }
}
