package com.citacomsys.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citacomsys.core.UIConstants
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.LocalDefaultPadding
import com.citacomsys.core.resources.LocalLargerPadding
import com.citacomsys.core.resources.LocalMediumPadding

@Composable
fun SubmitButtons(
    enabled: Boolean = true,
    isLoading: Boolean = false,
    label: String = "Submit",
    onDismiss: () -> Unit,
    onConfirmationClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(LocalDefaultPadding.current.size),
        horizontalArrangement =
            Arrangement.spacedBy(LocalDefaultPadding.current.size),
    ) {
        SecondaryButton(
            text = "Cancel",
            modifier = Modifier.weight(1f),
            onClick = onDismiss,
        )
        PrimaryButton(
            text = label,
            isLoading = isLoading,
            isEnabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onConfirmationClick,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    isLoading: Boolean = false,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = modifier.heightIn(min = LocalLargerPadding.current.size),
        shape = RoundedCornerShape(LocalMediumPadding.current.size),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        contentPadding =
            PaddingValues(
                horizontal = LocalDefaultPadding.current.size,
                vertical = LocalDefaultPadding.current.size,
            ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tint =
        if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = LocalLargerPadding.current.size),
        shape = RoundedCornerShape(LocalMediumPadding.current.size),
        border = BorderStroke(1.dp, tint),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = tint,
                disabledContentColor = tint.copy(alpha = UIConstants.FLOAT_3),
            ),
        contentPadding =
            PaddingValues(
                horizontal = LocalDefaultPadding.current.size,
                vertical = LocalDefaultPadding.current.size,
            ),
    ) {
        Text(
            text = text,
            color = if (enabled) tint else tint.copy(alpha = UIConstants.FLOAT_3)
        )
    }
}

@Composable
fun LanguageSwitcherButton(
    onToggle: () -> Unit,
    language: String,
) {
    IconButton(onClick = onToggle) {
        Text(language, color = Color.White)
    }
}

@Composable
fun ThemeToggleButton(
    isDark: Boolean,
    onToggle: () -> Unit
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (isDark) AppIcons.DarkMode else AppIcons.LightMode,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

@Composable
fun <T> SegmentedButtonRow(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    getDisplayName: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val buttonModifier = Modifier.weight(1f)
            val textModifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())

            if (isSelected) {
                Button(
                    onClick = { onOptionSelected(option) },
                    modifier = buttonModifier,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        getDisplayName(option),
                        textAlign = TextAlign.Center,
                        modifier = textModifier,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onOptionSelected(option) },
                    modifier = buttonModifier,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        getDisplayName(option),
                        textAlign = TextAlign.Center,
                        modifier = textModifier,
                    )
                }
            }
        }
    }
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Back")
    }
}

@Composable
fun RefreshButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isRefreshing: Boolean,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isRefreshing,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = contentColorFor(MaterialTheme.colorScheme.primary)
            )
        }
        Icon(AppIcons.Refresh, null)
    }
}

@Composable
fun SaveButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean = true,
    text: String = "Save"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = contentColorFor(MaterialTheme.colorScheme.primary)
            )
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * An action orchestrator that wraps a button and manages loading/error states based on a result.
 */
@Composable
fun UiActionButton(
    text: String,
    isLoading: Boolean,
    error: String? = null,
    icon: ImageVector? = null,
    isSuccess: Boolean = false,
    successIcon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
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
                    color = contentColorFor(MaterialTheme.colorScheme.primary)
                )
            } else {
                if (error != null) {
                    Icon(AppIcons.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Submission")
                } else {
                    if (isSuccess) {
                        Icon(successIcon ?: AppIcons.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    } else {
                        icon?.let {
                            Icon(icon, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    Text(text, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}