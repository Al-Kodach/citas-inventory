package com.citacomsys.core.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.adminShortcuts(
    onNew: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
): Modifier = this.onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

    when {
        event.isCtrlPressed && event.key == Key.N -> {
            onNew()
            true
        }

        event.isCtrlPressed && event.key == Key.S -> {
            onSave()
            true
        }

        event.key == Key.Escape -> {
            onCancel()
            true
        }

        else -> false
    }
}

fun Modifier.handleTap(
    key: Any? = Unit,
    onDoubleTab: (Offset) -> Unit = {},
    onTab: (Offset) -> Unit = {}
): Modifier = this.pointerInput(key) {
    detectTapGestures(
        onDoubleTap = onDoubleTab,
        onTap = onTab
    )
}

