package com.design.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.oikvpqya.compose.fastscroller.*

@Composable
 fun AppVerticalScrollbar(state: LazyListState, modifier: Modifier) {
    VerticalScrollbar(
        modifier = modifier,
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

@Composable
 fun AppVerticalScrollbarGrid(state: LazyGridState, modifier: Modifier) {
    VerticalScrollbar(
        modifier = modifier,
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
