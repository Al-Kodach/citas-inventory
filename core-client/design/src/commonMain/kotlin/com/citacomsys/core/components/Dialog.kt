package com.citacomsys.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.citacomsys.core.UIConstants
import com.citacomsys.core.resources.LocalDefaultPadding
import com.citacomsys.core.resources.LocalMediumPadding

@Composable
fun ConfirmationDialog(
    title: String,
    showDialog: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirmationClick: () -> Unit,
) {
    if (!showDialog.value) return

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(LocalMediumPadding.current.size)
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp,
                MaterialTheme.colorScheme.primary
            ),
            tonalElevation = LocalMediumPadding.current.size,
            modifier =
                Modifier
                    .widthIn(min = UIConstants.DIALOG_MIN_WIDTH.dp, max = UIConstants.DIALOG_MAX_WIDTH.dp)
                    .heightIn(min = UIConstants.DIALOG_MAX_HEIGHT.dp)
                    .clip(shape),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalDefaultPadding.current.size, vertical = LocalDefaultPadding.current.size),
                verticalArrangement = Arrangement.spacedBy(LocalDefaultPadding.current.size),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                SubmitButtons(
                    enabled = true,
                    onDismiss = onDismiss,
                    onConfirmationClick = onConfirmationClick,
                )
            }
        }
    }
}

