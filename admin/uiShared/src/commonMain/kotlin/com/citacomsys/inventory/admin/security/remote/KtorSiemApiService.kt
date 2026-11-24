package com.citacomsys.inventory.admin.security.remote

import com.citacomsys.feature.api.telemetry.SiemApiService
import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class KtorSiemApiService(
    client: HttpClient
) : SiemApiService, BaseApi(client) {

    override suspend fun getDeviceTimeline(deviceId: String): List<TraceTimelineEntry> {
        return executeRequest { client.get("api/v1/admin/siem/events/device/$deviceId") }
    }

    override suspend fun getGlobalSecurityFeed(limit: Int): List<TraceTimelineEntry> {
        return executeRequest {
            client.get("api/v1/admin/siem/events/feed") {
                parameter("limit", limit)
            }
        }
    }

    override suspend fun getRequestTrace(requestId: String): List<TraceTimelineEntry> {
        return executeRequest { client.get("api/v1/admin/siem/trace/$requestId") }
    }
}