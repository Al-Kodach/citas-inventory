package com.citacomsys.admin.api.dashboard

interface AdminDashboardApiService {
    suspend fun getPlatformSummary(): AdminDashboardOverview
}
