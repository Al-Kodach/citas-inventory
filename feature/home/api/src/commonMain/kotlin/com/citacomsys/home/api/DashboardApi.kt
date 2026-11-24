package com.citacomsys.home.api

interface DashboardApi {
    /**
     * Retrieves the high-level operational summary.
     * @param branchId Optional branch filter. If null, returns business-wide (CEO Mode) data.
     * @param startDate Optional start date in ISO format.
     * @param endDate Optional end date in ISO format.
     */
    suspend fun getDashboardOverview(
        branchId: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<DashboardOverview>
}
