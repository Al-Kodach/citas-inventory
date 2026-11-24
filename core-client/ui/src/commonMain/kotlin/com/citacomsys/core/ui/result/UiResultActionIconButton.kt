package com.citacomsys.core.ui.result

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons

/**
 * An orchestrated IconButton that adapts its icon and colors based on a [UiResult].
 * Provides immediate feedback for background operations like syncing or refreshing.
 */
@Composable
fun UiResultActionIconButton(
    result: UiResult<*>,
    icon: ImageVector = AppIcons.Refresh,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = when (result) {
        is UiResult.Error -> IconButtonDefaults.iconButtonColors(
            contentColor = if (result.canRetry) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        UiResult.Loading -> IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )

        else -> IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && result !is UiResult.Loading,
        colors = colors
    ) {
        when (result) {
            UiResult.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is UiResult.Error -> {
                if (result.canRetry) {
                    Icon(AppIcons.Refresh, contentDescription = "Retry Action")
                } else {
                    Icon(
                        AppIcons.Error,
                        contentDescription = "Operation Failed",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Icon(imageVector = icon, contentDescription = null)
            }
        }
    }
}
