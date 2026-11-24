package com.citacomsys.feature.telemetry

import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.feature.api.telemetry.SecurityThreatSummary
import com.citacomsys.feature.api.telemetry.WorkspaceSecurityApiService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class KmpTenantSecurityApiService(
    client: HttpClient
) : BaseApi(client), WorkspaceSecurityApiService {

    private val endpoint = "api/v1/security/audit-feed"

    override suspend fun getAuditFeed(
        branchId: String?,
        limit: Int
    ): List<AuditTimelineEntry> {
        return executeRequest {
            client.get(endpoint) {
                parameter("branchId", branchId)
                parameter("limit", limit)
            }
        }
    }

    override suspend fun getThreatSummary(workspaceId: String): List<SecurityThreatSummary> {
        return executeRequest {
            client.get("api/v1/security/threat-summary") {
                parameter("activeWorkspaceId", workspaceId)
            }
        }
    }
}
