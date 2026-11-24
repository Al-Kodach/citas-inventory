package com.citacomsys.inventory.admin.security.siem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.feature.api.telemetry.ComponentFailureSummary
import com.citacomsys.feature.api.telemetry.SecurityThreatSummary
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed interface SiemDashboardState {
    object Loading : SiemDashboardState
    data class Success(
        val threats: List<SecurityThreatSummary>,
        val failures: List<ComponentFailureSummary>
    ) : SiemDashboardState

    data class Error(val message: String) : SiemDashboardState
}

class SiemDashboardViewModel(
    private val httpClient: HttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<SiemDashboardState>(SiemDashboardState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadAnalyticsData()
    }

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = SiemDashboardState.Loading
            runCatching {
                // Fetch threat matrices and operational failure matrices concurrently
                val threats: List<SecurityThreatSummary> =
                    httpClient.get("/api/v1/admin/siem/analytics/threats").body()
                val failures: List<ComponentFailureSummary> =
                    httpClient.get("/api/v1/admin/siem/analytics/failures").body()

                SiemDashboardState.Success(threats, failures)
            }.onSuccess { successState ->
                _uiState.value = successState
            }.onFailure { exception ->
                _uiState.value = SiemDashboardState.Error(
                    exception.message ?: "Failed to pull telemetry indices."
                )
            }
        }
    }
}
