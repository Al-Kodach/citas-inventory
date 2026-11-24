package com.citacomsys.core.ui.management

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> FilterBar(
    items: List<T> = emptyList(),
    toString: (T?) -> String?,
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("All Status") }
        )

        items.forEach { item ->
            FilterChip(
                selected = selectedStatus == toString(item),
                onClick = { onStatusSelected(toString(item)) },
                label = { Text(toString(item) ?: "") }
            )
        }
    }
}