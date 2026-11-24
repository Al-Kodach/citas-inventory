package com.citacomsys.feature.workspace.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.core.components.ResponsiveScreenWrapper
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.WorkspaceEnforcementPolicyCard
import com.citacomsys.core.ui.WorkspaceRuntimeConfigCard
import com.citacomsys.core.ui.WorkspaceRuntimeStateCard
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.UiResultActionIconButton
import com.citacomsys.core.util.formatted
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceProfileScreen(
    viewModel: WorkspaceProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    AppScaffold(
        title = "Business Workspace",
        onBack = navigator::goBack,
        actions = {
            UiResultActionIconButton(
                result = refreshState,
                icon = AppIcons.Refresh,
                onClick = viewModel::refresh
            )
        }
    ) { padding ->
        ResponsiveScreenWrapper(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            UiResultView(state = state, onRetry = viewModel::refresh) { profile ->
                WorkspaceProfileContent(
                    profile = profile,
                    onUpdateIdentity = viewModel::updateIdentity,
                    onNavigateToBilling = { navigator.navigateToSingleTop(SubscriptionHubNav) },
                    onNavigateToBranches = {
//                        navigator.navigateToSingleTop(BranchManagementNav)
                    },
                    onNavigateToBranding = {
//                        navigator.navigateToSingleTop(BrandingNav)
                    },
                )
            }
        }
    }
}

@Composable
private fun WorkspaceProfileContent(
    profile: WorkspaceProfileUiState,
    onUpdateIdentity: (String, String?, String?) -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToBranches: () -> Unit,
    onNavigateToBranding: () -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 1000.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Column: Identity & Access
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IdentityCard(profile, onUpdateIdentity)
                    AccessKeysCard(profile)
                    LocationSummaryCard(profile, onNavigateToBranches)
                    BrandingLinkCard(onNavigateToBranding)
                }

                // Right Column: Licensing & Health
                Column(
                    modifier = Modifier.weight(1.2f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    SubscriptionSummaryCard(profile, onNavigateToBilling)
                    profile.config?.let { WorkspaceRuntimeConfigCard(it) }

                    profile.workspaceState?.let {
                        WorkspaceRuntimeStateCard(it)
                        WorkspaceEnforcementPolicyCard(it)
                    }
                }
            }
        } else {
            // Mobile Stacked
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IdentityCard(profile, onUpdateIdentity)
                SubscriptionSummaryCard(profile, onNavigateToBilling)
                LocationSummaryCard(profile, onNavigateToBranches)
                BrandingLinkCard(onNavigateToBranding)

                profile.config?.let { WorkspaceRuntimeConfigCard(it) }

                profile.workspaceState?.let {
                    WorkspaceRuntimeStateCard(it)
                    WorkspaceEnforcementPolicyCard(it)
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(
    profile: WorkspaceProfileUiState,
    onUpdateIdentity: (String, String?, String?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(profile.name) { mutableStateOf(profile.name) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Business Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (profile.canManageBilling) {
                    TextButton(onClick = {
                        if (isEditing) onUpdateIdentity(editedName, null, null)
                        isEditing = !isEditing
                    }) {
                        Text(if (isEditing) "Save" else "Edit")
                    }
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Trading Name") },
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
                "System ID: ${profile.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AccessKeysCard(profile: WorkspaceProfileUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Industry Platform Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            profile.productKey?.let { key ->
                Text("Product Key", style = MaterialTheme.typography.labelSmall)
                Text(
                    key,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LocationSummaryCard(profile: WorkspaceProfileUiState, onClick: () -> Unit) {
    val usage = profile.config?.usage
    val resource = profile.config?.resource

    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Physical Locations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(AppIcons.ChevronRight, null)
            }

            usage?.let { u ->
                resource?.let { r ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${u.branchCount} of ${r.maxBranches} locations configured",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandingLinkCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Visual Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(AppIcons.Palette, null)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Configure colors, logo and custom theme.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SubscriptionSummaryCard(profile: WorkspaceProfileUiState, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.1f
            )
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(AppIcons.ChevronRight, null)
            }

            val sub = (profile.subscription as? UiResult.Success)?.data
            if (sub != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Renewal: ${sub.endedAt.formatted("MEDIUM") ?: "Never"}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
