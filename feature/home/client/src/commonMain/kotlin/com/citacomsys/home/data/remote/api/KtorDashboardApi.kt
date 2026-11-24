package com.citacomsys.home.data.remote.api

import com.citacomsys.home.api.DashboardApi
import com.citacomsys.home.api.DashboardOverview
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get

internal class KtorDashboardApi(client: HttpClient) : BaseApi(client), DashboardApi {
    override suspend fun getDashboardOverview(
        branchId: String?,
        startDate: String?,
        endDate: String?
    ): Result<DashboardOverview> = runCatching {
        executeRequest { 
            client.get("/home/dashboard") {
                url {
                    branchId?.let { parameters.append("branchId", it) }
                    startDate?.let { parameters.append("startDate", it) }
                    endDate?.let { parameters.append("endDate", it) }
                }
            }
        }
    }
}
