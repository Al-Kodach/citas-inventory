package com.citacomsys.feature.workspace.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.core.branding.BrandingPreviewCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LegalNav
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.layout.AdaptiveRegistrationLayout
import com.citacomsys.core.ui.productPackage.InvoicePreviewView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.feature.workspace.ui.registration.orchestrator.WorkspaceSetupDraft
import com.citacomsys.shared.displayName

@Composable
fun ReviewRegistrationScreen(
    draft: WorkspaceSetupDraft,
    invoicePreviewState: UiResult<InvoicePreviewUiState>,
    actionResult: UiResult<Any>,
    onConfirm: () -> Unit,
    onRefreshSubmitState: () -> Unit,
    onBack: () -> Unit
) {
    var agreedToTerms by rememberSaveable { mutableStateOf(false) }
    val navigator = LocalNavigator.current

    AdaptiveRegistrationLayout(
        title = "Step 4: Finalize",
        onBack = onBack,
        icon = AppIcons.CheckCircle,
        instructionTitle = "Ready to launch?",
        instructionText = "Review your business configuration and settlement summary. Once confirmed, we will provision your dedicated cloud environment immediately."
    ) {
        // Plan Review
        ReviewSection(title = "Financial Summary") {
            InvoicePreviewView(
                modifier = Modifier.fillMaxWidth(),
                invoicePreviewState = invoicePreviewState,
                onRefresh = onRefreshSubmitState
            )
        }

        // Identity & Operating Mode Review
        ReviewSection(title = "Business Identity & Mode") {
            InfoRow(label = "Trading Name", value = draft.name)
            InfoRow(label = "Primary Branch", value = draft.mainBranchName)
            InfoRow(label = "Business Type", value = draft.businessType.displayName())
            InfoRow(label = "Physical Location", value = "${draft.street}, ${draft.city}")
            draft.phone?.let { InfoRow(label = "Contact Phone", value = it) }
            draft.email?.let { InfoRow(label = "Email Address", value = it) }
        }

        // Branding Review
        draft.branding?.let { branding ->
            ReviewSection(title = "Visual Identity") {
                BrandingPreviewCard(
                    appName = draft.name,
                    primary = Color(branding.primaryColor.toInt()),
                    accent = branding.accentColor?.let { Color(it.toInt()) }
                        ?: MaterialTheme.colorScheme.secondary,
                    tertiary = branding.tertiaryColor?.let { Color(it.toInt()) }
                        ?: MaterialTheme.colorScheme.tertiary,
                    error = branding.errorColor?.let { Color(it.toInt()) }
                        ?: MaterialTheme.colorScheme.error,
                    darkState = remember { mutableStateOf(branding.defaultDarkMode) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = { navigator.navigateTo(LegalNav) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "View Terms & Conditions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Agreements
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agreedToTerms, onCheckedChange = { agreedToTerms = it })
            Text(
                "I accept the Citacom Systems Enterprise Terms and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        actionResult.getErrorOrNull()?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            enabled = agreedToTerms && actionResult !is UiResult.Loading && invoicePreviewState.isSuccess,
            shape = MaterialTheme.shapes.large,
        ) {
            if (actionResult is UiResult.Loading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    "Confirm & Provision Business",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ReviewSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
