package com.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.oikvpqya.compose.fastscroller.*

@Composable
fun AppListView(
    state: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            content = content
        )

        AppVerticalScrollbar(state)
    }
}

@Composable
fun AppVerticalGridView(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    columns: GridCells = GridCells.Adaptive(350.dp),
    state: LazyGridState = rememberLazyGridState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    content: LazyGridScope.() -> Unit,
) {
    Box(modifier = modifier) {
        LazyVerticalGrid(
            state = state,
            columns = columns,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            modifier = Modifier.fillMaxSize(),
            content = content
        )

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight()
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            adapter = rememberScrollbarAdapter(scrollState = state),
            style = ScrollbarStyle(
                minimalHeight = 52.dp,
                thickness = 4.dp,
                hoverDurationMillis = 300,
                thumbStyle = ThumbStyle(
                    shape = RoundedCornerShape(2.dp),
                    unhoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
                trackStyle = TrackStyle(
                    shape = RoundedCornerShape(2.dp),
                    unhoverColor = Color.Transparent,
                    hoverColor = Color.Transparent,
                ),
            ),
            enablePressToScroll = true,
        )
    }
}
