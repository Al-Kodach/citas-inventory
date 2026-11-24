@file:OptIn(ExperimentalMaterial3Api::class)

package com.citacomsys.core.ui.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.result.isLoading
import com.design.components.AppScaffold

/**
 * A highly modular scaffold for SaaS management lists with an adaptive collapsing header.
 */
@Composable
fun <V> SaaSListScreenScaffold(
    viewModel: SaaSListViewModel<V>,
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topBarActions: @Composable RowScope.() -> Unit = {},
    searchPlaceholder: String = "Search...",
    showSearchBar: Boolean = true,
    filterBar: @Composable (ColumnScope.() -> Unit)? = null,
    emptyTitle: String = "No Items Found",
    emptyDescription: String = "There are no records to display at this time.",
    emptyIcon: ImageVector = AppIcons.Inventory2,
    onClick: (V) -> Unit,
    onRetry: (() -> Unit)? = { viewModel.refresh() },
    // Grid Configuration
    gridCells: GridCells = GridCells.Adaptive(350.dp),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.enterAlwaysScrollBehavior(),
    content: @Composable (V, Boolean) -> Unit
) {
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val selected by viewModel.selectedItem.collectAsStateWithLifecycle()
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val queryState by viewModel.queryState.collectAsStateWithLifecycle()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = lazyGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false

            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore, loadState) {
        if (shouldLoadMore && loadState.canLoadMore && !loadState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    AppScaffold(
        modifier = modifier.fillMaxSize().let {
            if (scrollBehavior != null) it.nestedScroll(scrollBehavior.nestedScrollConnection) else it
        },
        title = title,
        onBack = onBack,
        actions = {
            topBarActions()
            IconButton(
                onClick = { viewModel.refresh() },
                enabled = !itemsState.isLoading
            ) {
                if (itemsState.isLoading || loadState.isLoadingMore) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(AppIcons.Refresh, contentDescription = "Refresh")
                }
            }
        }
    ) { paddingValues ->
        UiResultGridView(
            state = itemsState,
            onRetry = onRetry,
            columns = gridCells,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            lazyGridState = lazyGridState,
            emptyPlaceholder = {
                DetailsPlaceholder(
                    icon = emptyIcon,
                    title = emptyTitle,
                    description = emptyDescription,
                    modifier = Modifier.fillMaxSize()
                )
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) { items ->

            if (showSearchBar) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ManagementSearchBar(
                        query = queryState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        placeholder = searchPlaceholder
                    )
                }

                filterBar?.let { bar ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.padding(bottom = 8.dp)) {
                            Column { bar() }
                        }
                    }
                }
            }

            items(items, key = { it.hashCode() }) { item ->
                val isSelected = item == selected
                Surface(
                    onClick = { onClick(item) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.3f
                    )
                    else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        content(item, isSelected)
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (loadState.isLoadingMore) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(AppIcons.Search, null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQueryChange("") }) { Icon(AppIcons.Close, null) } }
            } else null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
