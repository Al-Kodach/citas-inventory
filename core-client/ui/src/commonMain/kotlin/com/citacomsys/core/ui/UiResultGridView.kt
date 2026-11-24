package com.citacomsys.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.modifiers.ShimmerItem
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.result.UiResult
import kotlinx.coroutines.launch

/**
 * A specialized version of [UiResultView] optimized for [LazyVerticalGridView].
 * It automatically handles shimmer loading states and empty placeholders within the grid context.
 */
@Composable
fun <T> UiResultGridView(
    state: UiResult<List<T>>,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Adaptive(320.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    emptyPlaceholder: @Composable (() -> Unit)? = null,
    itemLabel: String = "Data",
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onRetry: (suspend () -> Unit)? = null,
    itemContent: LazyGridScope.(List<T>) -> Unit
) {
    val scope = rememberCoroutineScope()

    when (state) {
        UiResult.Idle -> {
            Box(modifier = modifier.fillMaxSize())
        }

        UiResult.Loading -> {
            LazyVerticalGridView(
                modifier = modifier.fillMaxSize(),
                state = lazyGridState,
                columns = columns,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement
            ) {
                repeat(12) {
                    item {
                        ShimmerItem()
                    }
                }
            }
        }

        is UiResult.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DetailsPlaceholder(
                    icon = Icons.Default.Error,
                    title = "Failed to load data",
                    isError = true,
                    description = state.message,
                    actionContent = {
                        if (state.canRetry && onRetry != null) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { scope.launch { onRetry.invoke() } }) {
                                Text("Try Again")
                            }
                        }
                    }
                )
            }
        }

        is UiResult.Success -> {
            if (state.data.isEmpty() && emptyPlaceholder != null) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    emptyPlaceholder()
                }
            } else {
                LazyVerticalGridView(
                    modifier = modifier.fillMaxSize(),
                    columns = columns,
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                    horizontalArrangement = horizontalArrangement
                ) {
                    itemContent(state.data)
                }
            }
        }
    }
}
