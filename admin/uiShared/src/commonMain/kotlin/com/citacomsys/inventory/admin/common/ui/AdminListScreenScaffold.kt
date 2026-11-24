package com.citacomsys.inventory.admin.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.citacomsys.core.ui.management.SaaSListScreenScaffold
import com.citacomsys.core.ui.management.SaaSListViewModel

@Composable
fun <V> AdminListScreenScaffold(
    viewModel: SaaSListViewModel<V>,
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topBarActions: @Composable RowScope.() -> Unit = {},
    searchPlaceholder: String = "Search...",
    filterBar: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: (V) -> Unit,
    onRetry: (() -> Unit)? = { viewModel.refresh() },
    emptyTitle: String = "No Items Found",
    emptyDescription: String = "There are no records to display at this time.",
    emptyIcon: ImageVector = com.citacomsys.core.resources.AppIcons.Inventory2,

    // Grid Configuration
    gridCells: GridCells = GridCells.Adaptive(350.dp),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),

    scrollBehavior: TopAppBarScrollBehavior? = null,

    loadingContent: (@Composable () -> Unit)? = null,
    content: @Composable (V, Boolean) -> Unit
) {
    SaaSListScreenScaffold(
        viewModel = viewModel,
        title = title,
        onBack = onBack,
        modifier = modifier,
        topBarActions = topBarActions,
        searchPlaceholder = searchPlaceholder,
        filterBar = filterBar,
        onClick = onClick,
        onRetry = onRetry,
        gridCells = gridCells,
        lazyGridState = lazyGridState,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        scrollBehavior = scrollBehavior,
        emptyTitle = emptyTitle,
        emptyDescription = emptyDescription,
        emptyIcon = emptyIcon,
        content = content
    )
}
