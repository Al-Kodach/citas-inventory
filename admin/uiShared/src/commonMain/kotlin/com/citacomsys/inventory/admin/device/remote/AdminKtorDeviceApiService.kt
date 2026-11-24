package com.citacomsys.inventory.admin.device.remote

import com.citacomsys.admin.api.device.AdminDeviceApiService
import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

internal class AdminKtorDeviceApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/admin/workstations"
) : AdminDeviceApiService, BaseApi(client) {
    override suspend fun fetch(query: PageQuery): PageResult<DeviceSummary> {
        return executeRequest {
            client.post(endpoint) {
                setBody(query)
            }
        }
    }

    override suspend fun fetchEvents(deviceId: String): List<TraceTimelineEntry> {
       return executeRequest { client.get("$endpoint/$deviceId/events") }
    }

    override suspend fun getDevice(deviceId: String): HardwareTechInfo {
        return executeRequest { client.get("$endpoint/$deviceId") }
    }

    override suspend fun revokeDevice(deviceId: String): Result<Unit> = runCatching {
        executeRequest { client.delete("$endpoint/$deviceId") }
    }

    override suspend fun renameDevice(
        deviceId: String,
        newName: String
    ): Result<Unit> = runCatching {
        executeRequest {
            client.put("$endpoint/$deviceId/id") {
                setBody(mapOf("id" to newName))
            }
        }
    }
}