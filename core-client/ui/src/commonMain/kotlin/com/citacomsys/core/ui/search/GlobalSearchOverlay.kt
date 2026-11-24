package com.citacomsys.core.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.ModalScaffold
import com.citacomsys.core.ui.result.UiResult
import com.design.components.AppListView

@Composable
fun GlobalSearchOverlay(
    viewModel: GlobalSearchViewModel,
    onResultClick: (SearchResult) -> Unit
) {
    val isVisible by viewModel.isVisible.collectAsState()
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    if (!isVisible) return

    ModalScaffold(
        title = "Global Search",
        onBack = { viewModel.hide() }
    ) {
        GlobalSearchOverlayContent(
            query = query,
            results = results,
            onResultClick = { result ->
                onResultClick(result)
                viewModel.hide()
            },
            onQueryChange = viewModel::onQueryChange
        )
    }
}

@Composable
fun GlobalSearchOverlayContent(
    query: String,
    results: UiResult<List<SearchResult>>,
    onResultClick: (SearchResult) -> Unit,
    onQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products, sales, customers...") },
            leadingIcon = { Icon(AppIcons.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(AppIcons.Close, null)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Box(modifier = Modifier.weight(1f)) {
            UiResultView(state = results) { list ->
                if (query.trim().length < 2) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Type at least 2 characters to search...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No results found matching '$query'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AppListView(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(list, key = { "${it.type}:${it.id}" }) { result ->
                            ListItem(
                                headlineContent = { Text(result.title) },
                                supportingContent = { Text(result.subtitle) },
                                leadingContent = {
                                    Icon(
                                        imageVector = when (result.type.name) {
                                            "PRODUCT" -> AppIcons.Inventory
                                            "SALE" -> AppIcons.PointOfSale
                                            "CUSTOMER" -> AppIcons.People
                                            else -> AppIcons.Search
                                        },
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onResultClick(result) }
                            )
                        }
                    }
                }
            }
        }
    }
}
