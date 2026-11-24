package com.citacomsys.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A generic dropdown input field.
 *
 * @param T The type of items in the dropdown.
 * @param label The label for the dropdown field.
 * @param items The list of items to display in the dropdown.
 * @param itemToString Function to convert an item of type T to a String for display.
 * @param modifier Modifier for this composable.
 * @param enabled Controls the enabled state of the field. When false, it will be visually
 *                disabled and not interactable.
 * @param errorMessage Optional error userFriendlyMessage to display below the field.
 * @param isError Whether the field is currently in an error state. This will highlight the field
 *                and display the error userFriendlyMessage.
 * @param leadingIcon Optional leading icon for the TextField.
 * @param placeholderText Text to display when no item is selected and the field is empty (optional).
 *                        Note: `itemToString` for the "empty" or "null" representation in `items`
 *                        is usually preferred for consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownInput(
    label: String,
    items: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    itemToString: (T?) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorMessage: String? = null,
    isError: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val textFieldValue by remember(selected, items) {
        derivedStateOf {
            itemToString(selected)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = {
                if (enabled) {
                    expanded = !expanded
                    if (!expanded) {
                        focusManager.clearFocus()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { /* Read-only, selection happens via dropdown */ },
                readOnly = true,
                label = { Text(label) },
                placeholder = if (placeholderText != null && selected == null) {
                    { Text(placeholderText) }
                } else null,
                leadingIcon = leadingIcon,
                trailingIcon = {
                    Row {
                        if (isError && errorMessage != null) {
                            Icon(
                                Icons.Filled.Error,
                                "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = if (enabled) 0.dp else 8.dp)
                            )
                        }
                        if (enabled) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }
                },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (enabled && focusState.isFocused) {
                            expanded = true
                        }
                    }
                    .alpha(if (enabled) 1f else 0.6f),
                enabled = enabled,
                isError = isError,
            )

            if (enabled) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        focusManager.clearFocus()
                    },
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(itemToString(item)) },
                            onClick = {
                                onSelect(item)
                                expanded = false
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Display error userFriendlyMessage below the field
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterDropdown(
    label: String,
    selectedValue: String,
    items: List<T>,
    itemTextProvider: (T) -> String,
    onItemSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    textFileModifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.widthIn(min = 200.dp)
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = textFileModifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { onItemSelected(null); expanded = false }
            )
            items.forEach { selection ->
                DropdownMenuItem(
                    text = { Text(itemTextProvider(selection)) },
                    onClick = { onItemSelected(selection); expanded = false }
                )
            }
        }
    }
}
