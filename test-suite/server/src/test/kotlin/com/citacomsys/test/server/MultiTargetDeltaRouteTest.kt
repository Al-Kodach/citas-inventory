package com.citacomsys.test.server

import com.citacomsys.module.inventory.gateway.multiTargetDeltaRoute
import com.citacomsys.module.inventory.service.BaseProductService
import com.citacomsys.module.inventory.service.StockBatchService
import com.citacomsys.types.BatchItem
import com.citacomsys.types.BranchCursor
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.MultiTargetDeltaRequest
import com.citacomsys.types.MultiTargetDeltaResponse
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ProductItem
import com.citacomsys.types.ProductType
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Clock

class MultiTargetDeltaRouteTest : BaseRouteTest() {

    @Test
    fun testMultiTargetDeltaEndpointReturnsAggregatedWorkspaceAndBranchChanges() {
        val mockProductService = mockk<BaseProductService>(relaxed = true)
        val mockStockBatchService = mockk<StockBatchService>(relaxed = true)

        val ws1 = UUID.randomUUID().toString()
        val br1 = UUID.randomUUID().toString()
        val br2 = UUID.randomUUID().toString()

        val testUser = User(
            id = "user-1",
            organizationId = UUID.randomUUID().toString(),
            name = "Test Admin",
            username = "testadmin",
            phone = PhoneNumber("233200000000"),
            isPhoneVerified = true,
            email = "admin@test.com",
            userType = UserType.TENANT,
            mfaEnabled = false,
            status = UserStatus.ACTIVE,
            role = "ADMIN",
            capabilities = setOf(UserCapability.MANAGE_INVENTORY)
        )

        val sampleProduct = ProductItem(
            id = "prod-1",
            workspaceId = ws1,
            name = "Paracetamol 500mg",
            brandName = "GSK",
            sku = "SKU-001",
            barcode = "12345678",
            unitOfMeasure = "Box",
            reorderLevel = 10,
            type = ProductType.RETAIL,
            versionTimestamp = Clock.System.now()
        )

        val sampleBatch = BatchItem(
            batchId = "batch-1",
            productId = "prod-1",
            workspaceId = ws1,
            branchId = br1,
            batchNumber = "B123",
            availableQuantity = 100,
            sellingPriceAmountMinor = 1500L,
            versionTimestamp = Clock.System.now()
        )

        coEvery {
            mockProductService.getProductItemChanges(any(), any(), eq(ws1), any())
        } returns ChangeListResponse(updated = listOf(sampleProduct), deleted = emptyList())

        coEvery {
            mockStockBatchService.getChangeList(any(), any(), eq(ws1), eq(br1))
        } returns ChangeListResponse(updated = listOf(sampleBatch), deleted = emptyList())

        coEvery {
            mockStockBatchService.getChangeList(any(), any(), eq(ws1), eq(br2))
        } returns ChangeListResponse(updated = emptyList(), deleted = emptyList())

        val koinModule = module {
            single { mockProductService }
            single { mockStockBatchService }
        }

        runRouteTest(
            testUser = testUser,
            koinModule = koinModule,
            routeBlock = {
                multiTargetDeltaRoute()
            }
        ) {
            val client = createClient {
                install(ClientContentNegotiation) {
                    json()
                }
            }

            val request = MultiTargetDeltaRequest(
                workspaces = mapOf(
                    ws1 to listOf(
                        BranchCursor(branchId = br1, lastBatchSyncAt = Clock.System.now()),
                        BranchCursor(branchId = br2, lastBatchSyncAt = Clock.System.now())
                    )
                )
            )

            val response = client.post("/api/v1/products/deltas") {
                contentType(ContentType.Application.Json)
                header("X-Organization-Id", testUser.organizationId)
                header("X-Hardware-Id", "hardware-id")
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body: MultiTargetDeltaResponse = response.body()

            assertEquals(1, body.workspaceDeltas.size)
            val wsDelta = body.workspaceDeltas[ws1]
            assertEquals(1, wsDelta?.products?.updated?.size)
            assertEquals("prod-1", wsDelta?.products?.updated?.get(0)?.id)

            assertEquals(2, wsDelta?.branchBatches?.size)
            assertEquals(1, wsDelta?.branchBatches?.get(br1)?.updated?.size)
            assertEquals("batch-1", wsDelta?.branchBatches?.get(br1)?.updated?.get(0)?.batchId)
            assertEquals(0, wsDelta?.branchBatches?.get(br2)?.updated?.size)
        }
    }
}
