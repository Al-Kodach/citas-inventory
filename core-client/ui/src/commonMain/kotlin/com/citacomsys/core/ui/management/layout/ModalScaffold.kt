package com.citacomsys.core.ui.management.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter

/**
 * A standardized high-fidelity container for modal content.
 * Provides consistent glass styling, centering, and styled scrollbars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalScaffold(
    title: String,
    topBar: @Composable (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AppCard(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                topBar ?: TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },

                    actions = {
                        actions()
                        IconButton(onClick = onBack) {
                            Icon(AppIcons.Close, contentDescription = "Close")
                        }
                    }
                )

                Box(
                    modifier = Modifier.weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Top,
                        content = content
                    )

                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 4.dp),
                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                        style = defaultMaterialScrollbarStyle(),
                        enablePressToScroll = true,
                    )
                }
            }
        }
    }
}
