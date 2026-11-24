package com.citacomsys.inventory.admin.security.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.inventory.admin.security.hub.LiveSecurityActivityViewModel
import com.citacomsys.shared.event.EntityChangeEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveActivityStreamWidget(
    viewModel: LiveSecurityActivityViewModel = koinViewModel()
) {
    val events by viewModel.events.collectAsState()

    AppCard(
        modifier = Modifier.fillMaxWidth().height(400.dp)
    ) {
        Column(Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Real-time System Mutations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = Color.Green.copy(alpha = 0.1f),
                    contentColor = Color.Green,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(6.dp).background(Color.Green, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Listening for system events...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(events, key = { it.seq }) { event ->
                        LiveEventRow(event)
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveEventRow(event: EntityChangeEvent) {
    val actionColor = when(event.action.uppercase()) {
        "INSERT" -> Color(0xFF4CAF50)
        "UPDATE" -> Color(0xFF2196F3)
        "DELETE" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = actionColor.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when(event.entityType) {
                    "USER" -> AppIcons.Person
                    "TENANT" -> AppIcons.Business
                    "STOCK_BATCH" -> AppIcons.Layers
                    "PRODUCT" -> AppIcons.Inventory
                    "BILLING" -> AppIcons.Payment
                    else -> AppIcons.Settings
                }
                Icon(icon, null, tint = actionColor, modifier = Modifier.size(18.dp))
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            Text(
                "${event.entityType} ${event.action}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "ID: ${event.entityId.take(8).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                event.timestamp.formatRelativeTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "T: ${event.workspaceId?.take(8)?.uppercase() ?: "--------"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
