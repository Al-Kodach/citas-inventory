package com.citacomsys.pharmacy.service

import com.citacomsys.api.tenant.service.ResourceCheckResult
import com.citacomsys.api.tenant.service.WorkspaceResourceManager
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.module.inventory.repository.StockBatchRepository
import com.citacomsys.types.ResourceType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RefileAlertProcessingEngineTest {

    private val inventoryRepo = mockk<StockBatchRepository>()
    private val notificationService = mockk<NotificationService>()
    private val userRepository = mockk<UserRepository>()
    private val smsService = mockk<SmsService>()
    private val resourceManager = mockk<WorkspaceResourceManager>()

    @Test
    fun `processTenantPatientRefillReminders sends SMS for pending refills`() = runTest {
        val workspaceId = "workspace-1"
        
        // Use a spy or subclass to mock the internal database call
        val engine = spyk(RefileAlertProcessingEngine(
            inventoryRepo, notificationService, userRepository, smsService, resourceManager
        ), recordPrivateCalls = true)

        val mockRefill = PendingRefillTargetRecord(
            workspaceId = workspaceId,
            saleId = "sale-123",
            userId = "user-456",
            customerName = "John Doe",
            customerPhone = "0240000000",
            productName = "Amlodipine"
        )

        // Mock the "database" call result via spy
        coEvery { engine["getPendingChronicRefills"](workspaceId, any<kotlin.time.Instant>(), any<kotlin.time.Instant>()) } returns listOf(mockRefill)
        
        coEvery { resourceManager.checkAvailability(workspaceId, ResourceType.SMS) } returns ResourceCheckResult.Available
        coEvery { smsService.send(any(), any(), any(), any()) } just runs

        engine.processTenantPatientRefillReminders(tenantId)

        coVerify {
            smsService.send(
                userId = any(),
                tenantId = tenantId,
                to = "0240000000",
                body = match { it.contains("Amlodipine") && it.contains("John Doe") }
            )
        }
    }
}
