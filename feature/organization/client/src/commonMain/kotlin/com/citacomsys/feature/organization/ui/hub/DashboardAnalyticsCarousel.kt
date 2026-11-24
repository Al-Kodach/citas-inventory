package com.citacomsys.feature.organization.ui.hub

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.citacomsys.core.components.DashboardWidgetCard
import com.design.components.DASHBOARD_LARGE_SCREEN_WIDTH
import com.design.components.LocalApplyDashboardPadding
import com.citacomsys.core.spi.PluginDashboardWidget
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@Composable
fun DashboardAnalyticsCarousel(
    widgets: List<PluginDashboardWidget>,
    modifier: Modifier = Modifier
) {
    if (widgets.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { widgets.size })
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val minWidth = windowSizeClass.minWidthDp

    val horizontalDp = if (minWidth < DASHBOARD_LARGE_SCREEN_WIDTH) 0.dp else 16.dp

    LaunchedEffect(widgets.size) {
        while (widgets.size > 1) {
            delay(8.seconds)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage, animationSpec = tween(1000))
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalApplyDashboardPadding provides false) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentPadding = PaddingValues(horizontal = horizontalDp),
                pageSpacing = 16.dp
            ) { page ->
                val pageOffset =
                    ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = lerp(0.95f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        }
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    DashboardWidgetCard(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        widgets[page].content()
                    }
                }
            }
        }

        // Pager Indicators
        if (widgets.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}
