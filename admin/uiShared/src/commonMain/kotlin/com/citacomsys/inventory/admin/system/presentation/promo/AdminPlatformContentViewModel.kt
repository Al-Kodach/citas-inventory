package com.citacomsys.inventory.admin.system.presentation.promo

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.api.platform.model.PlatformContent
import com.citacomsys.api.platform.model.PlatformDemo
import com.citacomsys.api.platform.model.PlatformFeature
import com.citacomsys.api.platform.model.PlatformTestimonial
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing public-facing platform content (Marketing CMS).
 */
class AdminPlatformContentViewModel(
    private val api: SystemApiService,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _contentState = MutableStateFlow<UiResult<PlatformContent>>(UiResult.Idle)
    val contentState = _contentState.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _contentState.value = UiResult.Loading
            _contentState.value = runCatching { api.getPlatformContent() }.asUiResult()
        }
    }

    fun saveFeature(feature: PlatformFeature) {
        viewModelScope.launch {
            api.saveFeature(feature).onSuccess {
                showSuccess("Feature '${feature.title}' saved")
                loadContent()
            }.onFailure {
                showError(it.message ?: "Failed to save feature")
            }
        }
    }

    fun saveDemo(demo: PlatformDemo) {
        viewModelScope.launch {
            api.saveDemo(demo).onSuccess {
                showSuccess("Demo '${demo.title}' saved")
                loadContent()
            }.onFailure {
                showError(it.message ?: "Failed to save demo")
            }
        }
    }

    fun saveTestimonial(testimonial: PlatformTestimonial) {
        viewModelScope.launch {
            api.saveTestimonial(testimonial).onSuccess {
                showSuccess("Testimonial from '${testimonial.name}' saved")
                loadContent()
            }.onFailure {
                showError(it.message ?: "Failed to save testimonial")
            }
        }
    }
}
