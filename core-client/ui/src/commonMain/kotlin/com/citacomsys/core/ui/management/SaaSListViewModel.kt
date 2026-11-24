package com.citacomsys.core.ui.management

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.types.EntityType
import com.citacomsys.types.Order
import com.citacomsys.types.PageQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class ListQueryState(
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: Order = Order.DESC,
    val filters: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val refreshTrigger: Long = 0L
)

data class ListLoadState(
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

/**
 * Standardized base ViewModel that manages UI query state, search debouncing,
 * filters, selection, and pagination.
 *
 * Subclasses determine how items are produced by overriding [itemsState] or observing [pageQueryFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
abstract class SaaSListViewModel<V>(
    notificationService: InAppNotificationService,
    protected val sessionManager: AppSessionManager
) : BaseViewModel(notificationService) {

    abstract val entityType: EntityType

    protected val _queryState = MutableStateFlow(ListQueryState())
    val queryState: StateFlow<ListQueryState> = _queryState.asStateFlow()

    protected val _loadState = MutableStateFlow(ListLoadState())
    val loadState: StateFlow<ListLoadState> = _loadState.asStateFlow()

    protected val _selectedItem = MutableStateFlow<V?>(null)
    val selectedItem: StateFlow<V?> = _selectedItem.asStateFlow()

    /**
     * Debounced PageQuery flow emitted whenever search, pagination, filters, or refreshTrigger change.
     */
    val pageQueryFlow: Flow<PageQuery> = _queryState
        .debounce { query ->
            if (query.searchQuery.isNotEmpty() && query.page == 1) 300.milliseconds else 0.milliseconds
        }
        .map { toPageQuery(it) }

    /**
     * Abstract itemsState. Extended ViewModels decide how to produce this flow
     * (e.g. from local DB stream, paged remote API, or a hybrid flow).
     */
    abstract val itemsState: StateFlow<UiResult<List<V>>>

    init {
        viewModelScope.launch {
            sessionManager.activeViewBranchId
                .drop(1)
                .collect { refresh() }
        }
    }

    open fun loadMore() {
        if (_loadState.value.isLoadingMore || !_loadState.value.canLoadMore) return
        _queryState.update { it.copy(page = it.page + 1) }
    }

    open fun refresh() {
        _queryState.update { it.copy(page = 1, refreshTrigger = it.refreshTrigger + 1) }
    }

    open fun onSearchQueryChange(query: String) {
        _queryState.update { it.copy(searchQuery = query, page = 1) }
    }

    open fun onSelect(item: V?) {
        _selectedItem.value = item
    }

    open fun filtersChanged(key: String, value: String?) {
        val currentFilters = _queryState.value.filters.toMutableMap()
        if (value == null) {
            currentFilters.remove(key)
        } else {
            currentFilters[key] = value
        }
        _queryState.update { it.copy(filters = currentFilters, page = 1) }
    }

    protected fun updateLoadState(
        canLoadMore: Boolean = _loadState.value.canLoadMore,
        isLoadingMore: Boolean = _loadState.value.isLoadingMore,
        error: String? = null
    ) {
        _loadState.update {
            it.copy(
                canLoadMore = canLoadMore,
                isLoadingMore = isLoadingMore,
                error = error
            )
        }
    }

    protected fun toPageQuery(query: ListQueryState): PageQuery {
        return PageQuery(
            page = query.page,
            size = query.pageSize,
            sortBy = query.sortBy,
            workspaceId = sessionManager.activeWorkspaceId,
            filters = query.filters,
            search = query.searchQuery.takeIf { it.isNotBlank() },
            sortDirection = query.sortDirection
        )
    }
}
