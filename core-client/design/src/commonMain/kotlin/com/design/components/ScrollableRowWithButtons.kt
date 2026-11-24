package com.design.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.theme.isDesktopPlatform
import kotlinx.coroutines.launch

/**
 * Reusable horizontal scrollable container that renders items in a [LazyRow].
 * On Desktop (non-touch) environments, provides ultra-compact left and right scroll buttons.
 * On Mobile (Android/Touch), hides scroll buttons and relies on touch swipe gestures.
 */
@Composable
fun ScrollableRowWithButtons(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 2.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    showButtonsOnDesktop: Boolean = isDesktopPlatform,
    content: LazyListScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val canScrollBackward by remember { derivedStateOf { state.canScrollBackward } }
    val canScrollForward by remember { derivedStateOf { state.canScrollForward } }

    if (!showButtonsOnDesktop) {
        // Mobile / Touch Layout: Pure gesture-based LazyRow with zero button overhead
        LazyRow(
            state = state,
            modifier = modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    } else {
        // Desktop Layout: Ultra-compact adjacent scroll buttons
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canScrollBackward) {
                IconButton(
                    onClick = {
                        scope.launch {
                            state.animateScrollBy(-250f)
                        }
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ChevronLeft,
                        contentDescription = "Scroll Left",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyRow(
                state = state,
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )

            if (canScrollForward) {
                IconButton(
                    onClick = {
                        scope.launch {
                            state.animateScrollBy(250f)
                        }
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = "Scroll Right",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
