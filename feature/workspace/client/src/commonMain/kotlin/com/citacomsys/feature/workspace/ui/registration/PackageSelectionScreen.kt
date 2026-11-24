package com.citacomsys.feature.workspace.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.layout.AdaptiveRegistrationLayout
import com.citacomsys.types.DeploymentMode

@Composable
fun PackageSelectionScreen(
    handler: SubscriptionPackageReviewHandler,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val state by handler.state.collectAsStateWithLifecycle()
    val packagesState by handler.packages.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        handler.loadPkgs()
    }

    AdaptiveRegistrationLayout(
        title = "Step 2: Scaling & Deployment",
        onBack = onBack,
        icon = AppIcons.Layers,
        instructionTitle = "Choose your growth plan & operating mode",
        instructionText = "Select whether you want Cloud Storage & Backup or Local Device Storage deployment with peer-to-peer mesh routing."
    ) {
        UiResultView(
            state = packagesState,
            onRetry = { handler.loadPkgs() }
        ) { packages ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                packages.forEach { pkg ->
                    SelectablePackageTierCard(
                        pkg = pkg,
                        isSelected = state.selectedPackage?.id == pkg.id,
                        selectedTermId = state.selectedBillingTerm?.id,
                        onPackageSelected = { handler.selectPackage(pkg) },
                        onTermSelected = { term ->
                            handler.selectPackage(pkg)
                            handler.selectTerm(term)
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    enabled = state.selectedBillingTerm != null,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "Review Selection",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectablePackageTierCard(
    pkg: ProductPackageUiState,
    isSelected: Boolean,
    selectedTermId: String?,
    onPackageSelected: () -> Unit,
    onTermSelected: (BillTermUiState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentTerm = pkg.billingTerms.find { it.id == selectedTermId }
        ?: pkg.billingTerms.firstOrNull { it.isDefault } ?: pkg.billingTerms.first()

    AppCard(
        isSelected = isSelected,
        onClick = {
            onPackageSelected()
            onTermSelected(currentTerm)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pkg.name,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                if (pkg.recommended) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "RECOMMENDED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            pkg.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Text(
                text = "${currentTerm.pricePerMonth} / month",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            // Resource Highlights
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MiniSpecItem(AppIcons.Person, "${pkg.maxUsers} Users")
                MiniSpecItem(AppIcons.Smartphone, "${pkg.maxDevices} Terminals")
            }

            if (isSelected) {
                val currentModeLabel =
                    if (currentTerm.deploymentMode == DeploymentMode.OFFLINE_LOCAL) "Offline Mode (Local Device Storage)" else "Cloud Storage & Backup (${currentTerm.billingCycleName})"
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentModeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Operating Mode & Billing Term") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        pkg.billingTerms.forEach { term ->
                            val modeLabel =
                                if (term.deploymentMode == DeploymentMode.OFFLINE_LOCAL) "Offline Mode (Local Device Storage)" else "Cloud Storage & Backup (${term.billingCycleName})"
                            DropdownMenuItem(
                                text = { Text(modeLabel) },
                                onClick = {
                                    onTermSelected(term)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSpecItem(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
