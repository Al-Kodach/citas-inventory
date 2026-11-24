package com.citacomsys.feature.platform.ui.welcome

import androidx.lifecycle.viewModelScope
import com.citacomsys.api.platform.model.PlatformContent
import com.citacomsys.api.platform.remote.PlatformApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.core.ui.result.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlatformViewModel(
    private val api: PlatformApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _content = MutableStateFlow<UiResult<PlatformContent>>(UiResult.Idle)
    val content = _content.asStateFlow()

    fun loadData() {
        if (content.value.isLoading || content.value.isSuccess) return

        viewModelScope.launch {
            _content.value = UiResult.Loading
            _content.value = api.getWelcomeContent().asUiResult()
        }
    }
}
