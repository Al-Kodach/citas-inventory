package com.navigation.scenes

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** An [OverlayScene] that renders an [entry] within a [Dialog]. */
internal class DialogScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val dialogProperties: DialogProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        Dialog(
            onDismissRequest = onBack,
            properties = dialogProperties,
        ) {
            entry.Content()
        }
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [dialog] to their [NavEntry.metadata]
 * within a [Dialog] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
class DialogSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null

        val dialogProperties =
            lastEntry.metadata[DIALOG_KEY] as? DialogProperties ?: return null

        @Suppress("UNCHECKED_CAST")
        return DialogScene(
            key = lastEntry.contentKey as T,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = lastEntry,
            dialogProperties = dialogProperties,
            onBack = onBack
        )
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [Dialog].
         *
         * @param dialogProperties properties that should be passed to the containing [Dialog].
         */
        fun dialog(
            dialogProperties: DialogProperties = DialogProperties()
        ): Map<String, Any> = mapOf(DIALOG_KEY to dialogProperties)

        internal const val DI_KEY = "dialog"
        private const val DIALOG_KEY = "dialog"
    }
}
