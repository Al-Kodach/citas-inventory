package com.citacomsys.inventory.admin.app.dashboard.remote.api

import com.citacomsys.admin.api.dashboard.AdminDashboardApiService
import com.citacomsys.admin.api.dashboard.AdminDashboardOverview
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class KtorAdminDashboardApiService(
    httpClient: HttpClient
) : BaseApi(httpClient), AdminDashboardApiService {

    override suspend fun getPlatformSummary(): AdminDashboardOverview =
        executeRequest {
            client.get("/api/v1/admin/dashboard/summary")
        }
}
