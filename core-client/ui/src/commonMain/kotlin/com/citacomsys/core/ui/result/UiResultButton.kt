package com.citacomsys.core.ui.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons

/**
 * An action orchestrator that wraps a button and manages isLoading/error states based on a [UiResult].
 * Automatically handles displaying errors inline and provides a retry state.
 */
@Composable
fun UiResultButton(
    result: UiResult<*>,
    text: String,
    icon: ImageVector? = null,
    showErrorInline: Boolean = false,
    successIcon: ImageVector? = AppIcons.Check,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    val isLoading = result is UiResult.Loading
    val error = result.getErrorOrNull()
    val isSuccess = result.isSuccess

    Column(modifier = modifier.animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility (error != null && showErrorInline) {
            error?.let {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = colors,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            } else {
                if (isSuccess && successIcon != null) {
                    Icon(successIcon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                } else if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
