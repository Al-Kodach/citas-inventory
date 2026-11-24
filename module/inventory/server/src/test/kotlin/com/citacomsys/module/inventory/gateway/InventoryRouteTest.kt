package com.citacomsys.module.inventory.gateway

import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.module.inventory.model.StockTransferPayload
import com.citacomsys.module.inventory.service.StockBatchService
import com.citacomsys.test.BaseRouteTest
import com.citacomsys.test.TestDataFactory
import com.citacomsys.types.Money
import com.citacomsys.module.inventory.request.RestockRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.time.Clock

class InventoryRouteTest : BaseRouteTest() {

    private val stockService = mockk<StockBatchService>()

    private val inventoryModule = module {
        single { stockService }
        single { mockk<com.citacomsys.feature.api.telemetry.TelemetryAuditor>(relaxed = true) }
        single { mockk<com.citacomsys.core.security.service.SecurityThrottlerService>(relaxed = true) }
        single { mockk<com.citacomsys.inventory.api.user.UserRepository>(relaxed = true) }
        single { mockk<SmsService>(relaxed = true) }
    }

    @Test
    fun `restock returns success`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = inventoryModule,
        routeBlock = { inventory() }
    ) {
        coEvery { stockService.restock(any(), any(),) } returns Result.success(Unit)

        val request = RestockRequest(
            id = "batch-id",
            workspaceId = "workspace-id",
            branchId = "branch-id",
            productId = "product-id",
            batchNumber = "BATCH001",
            expiryDate = null,
            costPrice = Money(100L),
            sellingPrice = Money(150L),
            quantity = 10,
            actorId = "admin-id",
            actorName = "Admin",
            createdAt = Clock.System.now()
        )

        val jsonClient = createJsonClient()
        val response = jsonClient.post("/api/v1/inventory/stocks/restock") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `stock transfer returns success when authorized`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = inventoryModule,
        routeBlock = { inventory() }
    ) {
        val payload = StockTransferPayload(
            id = "transfer-id",
            batchId = "source-batch-id",
            productId = "product-id",
            sourceBranchId = "branch-a",
            destinationBranchId = "branch-b",
            quantity = 5,
            reason = "Stock redistribution",
            actorId = "admin-id"
        )

        // Mock the required Action Token Principal
        mockActionPrincipal = com.citacomsys.core.security.token.jwt.ActionPrincipal(
            userId = "admin-id",
            username = "admin",
            workspaceId = "workspace-id",
            role = "ADMIN",
            sessionId = "session-id",
            action = com.citacomsys.types.ActionConfirmationPurpose.TRANSFER_STOCK
        )

        coEvery { stockService.executeTransfer(any()) } returns Result.success(Unit)

        val jsonClient = createJsonClient()
        val response = jsonClient.put("/api/v1/inventory/transfer") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `adjust stock returns success`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = inventoryModule,
        routeBlock = { inventory() }
    ) {
        val request = com.citacomsys.module.inventory.request.AdjustStockRequest(
            batchId = "batch-id",
            workspaceId = "workspace-id",
            branchId = "branch-id",
            productId = "product-id",
            quantity = 5,
            reason = "Correction"
        )

        coEvery { stockService.adjust(any()) } returns Result.success(Unit)

        val jsonClient = createJsonClient()
        val response = jsonClient.put("/api/v1/inventory/stocks/adjustStock") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `update price returns success`() = runRouteTest(
        testUser = TestDataFactory.adminUser,
        koinModule = inventoryModule,
        routeBlock = { inventory() }
    ) {
        val request = com.citacomsys.module.inventory.request.PriceChangeRequest(
            batchId = "batch-id",
            newPrice = Money(200L),
            type = com.citacomsys.module.inventory.request.PriceType.SELLING,
            actorId = "admin-id",
            branchId = "branch-id",
            createdAt = Clock.System.now()
        )

        coEvery { stockService.updatePrice(any(), any()) } returns Unit

        val jsonClient = createJsonClient()
        val response = jsonClient.put("/api/v1/inventory/stocks/batch-id/price") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
