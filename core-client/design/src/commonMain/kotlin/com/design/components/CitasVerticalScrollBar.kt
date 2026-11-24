package com.design.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.oikvpqya.compose.fastscroller.ScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.ThumbStyle
import io.github.oikvpqya.compose.fastscroller.TrackStyle
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter

@Composable
fun BoxScope.AppVerticalScrollbar(
    state: LazyListState = rememberLazyListState(),
) {

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