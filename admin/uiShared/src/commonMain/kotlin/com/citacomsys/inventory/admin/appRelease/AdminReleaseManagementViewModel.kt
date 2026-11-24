package com.citacomsys.inventory.admin.appRelease

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.feature.api.telemetry.PrivateUpdateResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AdminReleaseManagementViewModel(
    notification: InAppNotificationService,
    private val httpClient: HttpClient
) : BaseViewModel(notification) {

    private val _releasesHistory = MutableStateFlow<List<PrivateUpdateResponse>>(emptyList())
    val releasesHistory = _releasesHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    var showUploadModal by mutableStateOf(false)

    init {
        refreshReleaseHistory()
    }

    fun refreshReleaseHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val results = httpClient.get("/api/v1/admin/platform/app-releases/history")
                    .body<List<PrivateUpdateResponse>>()
                _releasesHistory.value = results
            }.onFailure { error ->
                showError("Failed to synchronize distribution tracks: ${error.message}")
            }
            _isLoading.value = false
        }
    }
}
