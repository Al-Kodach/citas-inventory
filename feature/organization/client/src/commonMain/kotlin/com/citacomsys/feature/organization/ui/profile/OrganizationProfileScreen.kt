package com.citacomsys.feature.organization.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.ResponsiveScreenWrapper
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.kernel.model.OrganizationMetrics
import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.types.ProductType
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationProfileScreen(
    viewModel: OrganizationProfileViewModel = koinViewModel()
) {
    val configResult by viewModel.config.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshState) {
        if (refreshState is UiResult.Error) {
            snackbarHostState.showSnackbar((refreshState as UiResult.Error).message)
        }
    }

    AppScaffold(
        title = "Company Profile",
        onBack = navigator::goBack,
        actions = {
            IconButton(
                onClick = viewModel::loadData
            ) {
               Icon(AppIcons.Refresh, "Refresh")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ResponsiveScreenWrapper(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            UiResultView(state = configResult, onRetry = viewModel::loadData) { config ->
                if (config == null) {
                    DetailsPlaceholder(
                        icon = Icons.Default.BusinessCenter,
                        title = "Company Profile",
                        description = "Company profile is not available"
                    )
                    return@UiResultView
                }
                OrganizationProfileContent(
                    config = config,
                    onUpdateIdentity = viewModel::updateIdentity
                )
            }
        }
    }
}

@Composable
private fun OrganizationProfileContent(
    config: OrganizationRuntimeConfig,
    onUpdateIdentity: (String, String?, String?) -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 1000.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Column: Identity & Global Metrics
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    LegalIdentityCard(config, onUpdateIdentity)
                    GlobalMetricsCard(config.metrics)
                }

                // Right Column: Workspaces List
                Column(
                    modifier = Modifier.weight(1.2f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    WorkspacesListCard(config.workspaces)
                }
            }
        } else {
            // Mobile Stacked
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegalIdentityCard(config, onUpdateIdentity)
                GlobalMetricsCard(config.metrics)
                WorkspacesListCard(config.workspaces)
            }
        }
    }
}

@Composable
private fun LegalIdentityCard(
    config: OrganizationRuntimeConfig,
    onUpdateIdentity: (String, String?, String?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(config.name) { mutableStateOf(config.name) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Legal Entity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    if (isEditing) onUpdateIdentity(editedName, null, null)
                    isEditing = !isEditing
                }) {
                    Text(if (isEditing) "Save" else "Edit")
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Organization Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    editedName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                "Global Business Code: ${config.businessCode}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GlobalMetricsCard(metrics: OrganizationMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Organization Rollup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Total Nodes",
                    value = metrics.totalWorkstations.toString(),
                    icon = AppIcons.Smartphone
                )
                SmallMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Branches",
                    value = metrics.totalActiveBranches.toString(),
                    icon = AppIcons.Storefront
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Monthly Cost", style = MaterialTheme.typography.bodyMedium)
                Text(
                    metrics.totalMonthlyRecurring,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun WorkspacesListCard(workspaces: List<WorkspaceSummary>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Industry Platforms",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            workspaces.forEach { ws ->
                WorkspaceRow(ws)
                if (ws != workspaces.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun WorkspaceRow(ws: WorkspaceSummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProjectLogos.ProductLogo(ws.type)

        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(ws.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                ws.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(ws.status)
        }
    }
}

@Composable
private fun SmallMetricItem(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Column(modifier) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}
