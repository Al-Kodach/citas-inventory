package com.citacomsys.core.spi.scenes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_LARGE_LOWER_BOUND

/**
 * A [Scene] that displays a list and a detail [NavEntry] side-by-side in a 40/60 split.
 *
 */
data class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)
    override val content: @Composable (() -> Unit) = {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.4f)) {
                listEntry.Content()
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Let the detail entry know not to display a back button.
            CompositionLocalProvider(LocalBackButtonVisibility provides false) {
                Column(modifier = Modifier.weight(0.6f)) {
                    AnimatedContent(
                        targetState = detailEntry,
                        contentKey = { entry -> entry.contentKey },
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it }
                            ) togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it })
                        }
                    ) { entry ->
                        entry.Content()
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * in the list pane of a [ListDetailScene].
         */
        fun listPane() = metadata {
            put(ListKey, true)
        }

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * in the detail pane of a the [ListDetailScene].
         */
        fun detailPane() = metadata {
            put(DetailKey, true)
        }

        /**
         * Helper function to add metadata to a [NavEntry] indicating it is the
         * default placeholder for the detail pane.
         */
        fun detailPlaceholder() = metadata {
            put(DetailKey, true)
            put(PlaceholderKey, true)
        }
    }

    object ListKey : NavMetadataKey<Boolean>
    object DetailKey : NavMetadataKey<Boolean>
    object PlaceholderKey : NavMetadataKey<Boolean>
}

/**
 * This `CompositionLocal` can be used by a detail `NavEntry` to decide whether to display
 * a back button. Default is `true`. It is set to `false` for a detail `NavEntry` when being
 * displayed in a `ListDetailScene`.
 */
val LocalBackButtonVisibility = compositionLocalOf { true }

@Composable
fun <T : Any> rememberListDetailSceneStrategy2(): ListDetailSceneStrategy<T> {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

    return remember(windowSizeClass) {
        ListDetailSceneStrategy(windowSizeClass)
    }
}


/**
 * A [SceneStrategy] that returns a [ListDetailScene] if:
 *
 * - the window width is over 600dp
 * - A `Detail` entry is the last item in the back stack
 * - A `List` entry is in the back stack
 *
 * Notably, when the detail entry changes the scene's key does not change. This allows the scene,
 * rather than the NavDisplay, to handle animations when the detail entry changes.
 */
class ListDetailSceneStrategy<T : Any>(val windowSizeClass: WindowSizeClass) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {

        if (!windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND)) {
            return null
        }

        val listEntry =
            entries.findLast { it.metadata.contains(ListDetailScene.ListKey) } ?: return null

        val detailEntry =
            entries.findLast { it.metadata.contains(ListDetailScene.DetailKey) && !it.metadata.contains(ListDetailScene.PlaceholderKey) }
            ?: entries.find { it.metadata.contains(ListDetailScene.PlaceholderKey) }
            ?: return null

        // We use the list's contentKey to uniquely identify the scene.
        val sceneKey = listEntry.contentKey

        return ListDetailScene(
            key = sceneKey,
            previousEntries = entries.filter { it != listEntry && it != detailEntry },
            listEntry = listEntry,
            detailEntry = detailEntry
        )
    }
}
