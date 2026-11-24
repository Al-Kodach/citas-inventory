package com.citacomsys.core.ui.search

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.repository.GlobalSearchRepository
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GlobalSearchViewModel(
    private val repository: GlobalSearchRepository,
    notificationService: InAppNotificationService
) : BaseViewModel(notificationService) {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    val searchResults: StateFlow<UiResult<List<SearchResult>>> = _query
        .debounce(300.milliseconds)
        .flatMapLatest { q ->
            val clean = q.trim()
            if (clean.length < 2) {
                flowOf(UiResult.Success(emptyList()))
            } else {
                repository.search(clean)
                    .map<List<SearchResult>, UiResult<List<SearchResult>>> { UiResult.Success(it) }
                    .catch { emit(UiResult.Error(it.message ?: "Search error")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiResult.Success(emptyList())
        )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun show() {
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
        _query.value = ""
    }
}
