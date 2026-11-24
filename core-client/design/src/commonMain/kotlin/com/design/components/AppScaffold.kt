package com.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String = "",
    topBar: @Composable (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    supportContent: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionIcon: ImageVector = Icons.Outlined.Info,
    showInfo: Boolean = false,
    onDismiss: () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    supportTitle: String? = "Information",
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var showInfoSheet by remember(showInfo) { mutableStateOf(showInfo) }

    LaunchedEffect(showInfoSheet) {
        if (!showInfoSheet) onDismiss()
    }

    Scaffold(
        topBar = topBar ?: {
            AppTopBar(
                title = title,
                onBack = onBack,
                onActionClick = supportContent?.let { { showInfoSheet = true } },
                actions = actions,
                actionIcon = actionIcon
            )
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        containerColor = containerColor,
        modifier = modifier.fillMaxSize().let {
            if (scrollBehavior != null) it.nestedScroll(scrollBehavior.nestedScrollConnection) else it
        }.consumeWindowInsets(WindowInsets.safeContent),
    ) { padding ->
        content(padding)

        if (showInfoSheet && supportContent != null) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(modifier = Modifier.fillMaxHeight(0.7f).padding(24.dp)) {
                    supportTitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    supportContent()
                }
            }
        }
    }
}
