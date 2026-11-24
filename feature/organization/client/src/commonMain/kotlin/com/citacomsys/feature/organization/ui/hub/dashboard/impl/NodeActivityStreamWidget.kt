package com.citacomsys.feature.organization.ui.hub.dashboard.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.database.dao.DashboardDao
import com.citacomsys.core.mesh.model.ActivityType
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.core.resources.AppIcons
import com.design.components.AppListView
import org.koin.compose.koinInject

data class UnifiedActivityDisplay(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestampMs: Long,
    val badge: String,
    val color: Color
)

@Composable
fun NodeActivityStreamWidget(
    meshManager: MeshManager = koinInject(),
    dashboardDao: DashboardDao = koinInject()
) {
    val businessFeed by dashboardDao.observeBusinessActivityFeed()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val meshSignals = remember { mutableStateListOf<NodeActivity>() }

    LaunchedEffect(Unit) {
        meshManager.meshSignals.collect { activity ->
            if (meshSignals.none { it.timestamp == activity.timestamp && it.bindingId == activity.bindingId }) {
                meshSignals.add(0, activity)
                if (meshSignals.size > 25) meshSignals.removeLast()
            }
        }
    }

    val combinedActivities = remember(businessFeed, meshSignals) {
        val bizItems = businessFeed.map { feed ->
            UnifiedActivityDisplay(
                id = "biz-${feed.id}",
                title = "${feed.type} - ${feed.productName} (Qty: ${feed.quantity})",
                subtitle = "Actor: ${feed.actorName} | Ref: ${feed.activityType ?: "System"}",
                timestampMs = feed.created_at.toEpochMilliseconds(),
                badge = "BIZ",
                color = Color(0xFF4CAF50)
            )
        }

        val meshItems = meshSignals.map { signal ->
            UnifiedActivityDisplay(
                id = "mesh-${signal.bindingId}-${signal.timestamp}",
                title = signal.action,
                subtitle = signal.summary,
                timestampMs = signal.timestamp.toEpochMilliseconds(),
                badge = "MESH",
                color = when (signal.type) {
                    ActivityType.STATE_CHANGE -> Color(0xFF2196F3)
                    ActivityType.SYNC_SIGNAL -> Color(0xFFFF9800)
                    ActivityType.TELEMETRY -> Color(0xFF9C27B0)
                }
            )
        }

        (bizItems + meshItems).sortedByDescending { it.timestampMs }
    }

    DashboardWidgetCard(
        modifier = Modifier.fillMaxWidth().height(400.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Unified Activity Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Business operations & node telemetry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (combinedActivities.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No recent business or mesh activity recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                AppListView(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(combinedActivities, key = { it.id }) { activity ->
                        ActivityRow(activity)
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
private fun ActivityRow(activity: UnifiedActivityDisplay) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = activity.color.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (activity.badge == "BIZ") AppIcons.Inventory else AppIcons.Sync,
                    contentDescription = null,
                    tint = activity.color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = activity.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = activity.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = activity.color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    activity.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                activity.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
