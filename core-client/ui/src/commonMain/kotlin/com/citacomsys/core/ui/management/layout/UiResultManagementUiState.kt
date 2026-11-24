package com.citacomsys.core.ui.management.layout

import com.citacomsys.core.ui.result.UiResult

data class UiResultManagementUiState<V, T>(
    val itemsState: UiResult<List<V>> = UiResult.Idle,
    val selected: T? = null,
    val extra: Any? = null
)
