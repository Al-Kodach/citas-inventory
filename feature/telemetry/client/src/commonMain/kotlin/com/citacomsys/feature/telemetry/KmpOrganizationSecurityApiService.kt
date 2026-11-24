package com.citacomsys.feature.telemetry

import com.citacomsys.feature.api.telemetry.OrganizationSecurityApiService
import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class KmpOrganizationSecurityApiService(
    client: HttpClient
) : BaseApi(client), OrganizationSecurityApiService {
    
    private val endpoint = "api/v1/organization/monitoring/audit-feed"

    override suspend fun getAuditFeed(limit: Int): List<AuditTimelineEntry> {
        return executeRequest {
            client.get(endpoint) {
                parameter("limit", limit)
            }
        }
    }
}
