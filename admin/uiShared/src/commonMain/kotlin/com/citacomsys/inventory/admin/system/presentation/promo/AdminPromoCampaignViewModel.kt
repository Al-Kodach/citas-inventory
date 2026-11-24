package com.citacomsys.inventory.admin.system.presentation.promo

import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.api.platform.model.ProductPackage
import com.citacomsys.api.platform.model.PromotionCampaign
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.inventory.admin.common.ui.AdminListItemsBaseViewModel
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.EntityType
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing platform-wide marketing campaigns and discount codes.
 * Cloud-native list that inherits from SaaS management architecture.
 */
class AdminPromoCampaignViewModel(
    private val api: SystemApiService,
    sessionManager: AppSessionManager,
    notificationService: InAppNotificationService
) : AdminListItemsBaseViewModel<PromotionCampaign>(notificationService, sessionManager) {
    override val entityType: EntityType = EntityType.PACKAGE

    override val itemsState: StateFlow<UiResult<List<PromotionCampaign>>> = pageQueryFlow
        .flatMapLatest { query ->
            flow {
                emit(UiResult.Loading)
                runCatching {
                    val all = api.fetchCampaigns()
                    val searchQuery = query.search?.lowercase()

                    val filtered = if (searchQuery.isNullOrBlank()) {
                        all
                    } else {
                        all.filter {
                            it.name.lowercase().contains(searchQuery) ||
                                    it.code.lowercase().contains(searchQuery)
                        }
                    }

                    PageResult(
                        items = filtered,
                        page = 0,
                        size = filtered.size.coerceAtLeast(1),
                        totalItems = filtered.size.toLong()
                    )
                }.onSuccess { pageResult ->
                    updateLoadState(canLoadMore = !pageResult.isLast, isLoadingMore = false)
                    emit(UiResult.Success(pageResult.items))
                }.onFailure { err ->
                    updateLoadState(isLoadingMore = false, error = err.message)
                    emit(UiResult.Error(err.message ?: "Failed to fetch packages"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Loading
        )

    /**
     * Saves or updates a campaign configuration.
     */
    fun saveCampaign(campaign: PromotionCampaign) {
        viewModelScope.launch {
            api.saveCampaign(campaign).onSuccess {
                showSuccess("Campaign '${campaign.name}' saved successfully")
                refresh()
            }.onFailure {
                showError(it.message ?: "Failed to save campaign")
            }
        }
    }

    /**
     * Quickly toggles the active status of a campaign.
     */
    fun toggleCampaign(campaign: PromotionCampaign) {
        viewModelScope.launch {
            val updated = campaign.copy(active = !campaign.active)
            api.saveCampaign(updated).onSuccess {
                refresh()
            }.onFailure {
                showError(it.message ?: "Failed to update campaign status")
            }
        }
    }
}
