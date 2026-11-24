package com.citacomsys.core.ui.management.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult

@Composable
fun <V, T> UiResultManagementListPlaceholder(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector = AppIcons.Inventory2,
    description: String = "No items found",
    itemsState: UiResult<List<V>>,
    selected: T?,
    onClick: (V) -> Unit,
    onRetry: (() -> Unit)? = null,
    itemContent: @Composable (V, Boolean) -> Unit
) {
    UiResultView(
        state = itemsState,
        onRetry = onRetry,
        modifier = modifier
    ) { items ->
        LazyListView {
            if (items.isEmpty()) {
                item {
                    DetailsPlaceholder(
                        icon = icon,
                        title = title,
                        description = description
                    )
                }
                return@LazyListView
            }

            items(items) { item ->
                val isSelected = item == selected
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick(item) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .padding(12.dp)
                ) {
                    itemContent(item, isSelected)
                }
            }
        }
    }

}