package com.citacomsys.core.ui.management.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult

@Composable
fun <T> UiResultItemsScreenPlaceholder(
    modifier: Modifier = Modifier,
    itemsState: UiResult<List<T>>,
    selected: T? = null,
    onClick: (T) -> Unit,
    onRetry: (() -> Unit)? = null,

    contentPadding: PaddingValues = PaddingValues(0.dp),
    gridCells: GridCells = GridCells.Adaptive(350.dp),
    state: LazyGridState = rememberLazyGridState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),

    idleContent: (@Composable () -> Unit)? = null,
    loadingContent: (@Composable () -> Unit)? = null,
    errorContent: (@Composable () -> Unit)? = null,
    content: @Composable (T, Boolean) -> Unit
) {

    UiResultView(
        state = itemsState,
        modifier = modifier,
        onRetry = onRetry,
        idleView = idleContent,
        loadingView = loadingContent,
        errorView = errorContent
    ) { items ->
        LazyVerticalGridView(
            state = state,
            columns = gridCells,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement
        ) {
            items(items, key = { it.hashCode() }) { item ->
                val isSelected = item == selected
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick(item) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Unspecified
                        )
                        .padding(12.dp)
                ) {
                    content(item, isSelected)
                }
            }
        }
    }
}