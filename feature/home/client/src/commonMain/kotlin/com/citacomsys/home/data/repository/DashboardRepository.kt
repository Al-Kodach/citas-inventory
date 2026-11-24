package com.citacomsys.home.data.repository

import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeSummary(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<DashboardSummaryData>

    fun observeActivityFeed(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<List<DashboardActivityData>>

    fun observeBusinessSummary(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<DashboardSummaryData>

    fun observeBusinessActivityFeed(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<List<DashboardActivityData>>
}
