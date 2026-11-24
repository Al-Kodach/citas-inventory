package com.citacomsys.feature.workspace.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.types.PackageTier
import com.citacomsys.core.spi.DashboardPlacement
import com.design.AppTheme
import org.koin.compose.koinInject

@Composable
fun WorkspaceResourceQuotaWidget(
    modifier: Modifier = Modifier,
    service: PlatformClientService = koinInject()
) {
    val navigator = LocalNavigator.current
    val wsConfig by service.activeWorkspaceConfig.collectAsStateWithLifecycle()

    val res = wsConfig?.resource
    val usage = wsConfig?.usage

    val userLimit = res?.maxUsers ?: 10
    val userCount = usage?.userCount ?: 0

    val branchLimit = res?.maxBranches ?: 5
    val branchCount = usage?.branchCount ?: 0

    val productLimit = res?.maxProductsLimit ?: 500
    val productCount = usage?.productCountUsed ?: 0

    val batchLimit = res?.maxActiveBatchesLimit ?: 100
    val batchCount = usage?.activeBatchesCount ?: 0

    val retention = res?.historyRetentionMonths ?: 3


    val canUpgrade = remember(wsConfig) {
        res?.tier != null && res.tier != PackageTier.STARTER
    }

    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.GRID
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resource Quotas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${res?.name ?: "Pro Plan"} active limits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canUpgrade) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { navigator.navigateToSingleTop(SubscriptionHubNav) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Upgrade", maxLines = 1)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuotaProgressBar(label = "Products Catalog", current = productCount, limit = productLimit)
                QuotaProgressBar(label = "Active Stock Batches", current = batchCount, limit = batchLimit)
                QuotaProgressBar(label = "User Seats", current = userCount, limit = userLimit)
                QuotaProgressBar(label = "Store Branches", current = branchCount, limit = branchLimit)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Retention",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$retention MONTHS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaProgressBar(
    label: String,
    current: Int,
    limit: Int
) {
    val progress = if (limit > 0) (current.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$current / $limit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = if (progress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}



@AllAdaptivePreviews
@Composable
private fun ResourceQuotaWidgetPreview() {
    AppTheme {
        WorkspaceResourceQuotaWidget()
    }
}