package com.citacomsys.feature.workspace.ui.registration.orchestrator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.branding.BrandingConfigScreen
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.layout.SectionWrapper
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.feature.workspace.ui.registration.*
import com.citacomsys.types.BrandingConfig
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkspaceSetupHost(
    onComplete: () -> Unit,
    onBackToHub: () -> Unit,
    viewModel: WorkspaceSetupViewModel = koinViewModel()
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val orgBranding by viewModel.orgBranding.collectAsStateWithLifecycle()
    val organizationConfig by viewModel.organizationConfig.collectAsStateWithLifecycle()
    val activationResult by viewModel.activationResult.collectAsStateWithLifecycle()
    val currentStep by viewModel.setUpState.collectAsStateWithLifecycle()
    val invoicePreviewState by viewModel.invoicePreviewState.collectAsStateWithLifecycle()
    val previewImages by viewModel.previewImages.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "workspace_setup_flow"
        ) { step ->
            when (step) {
                WorkspaceSetupStep.SPECS -> WorkspaceSpecsScreen(
                    companyName = organizationConfig?.name,
                    draft = draft,
                    onUpdate = viewModel::updateSpecs,
                    onNext = { viewModel.navigateTo(WorkspaceSetupStep.PLAN_SELECTION) },
                    onBack = onBackToHub
                )

                WorkspaceSetupStep.PLAN_SELECTION -> PackageSelectionScreen(
                    handler = viewModel.packageHandler,
                    onNext = { viewModel.navigateTo(WorkspaceSetupStep.PACKAGE_REVIEW) },
                    onBack = { viewModel.navigateTo(WorkspaceSetupStep.SPECS) }
                )

                WorkspaceSetupStep.PACKAGE_REVIEW -> PackageReviewScreen(
                    handler = viewModel.packageHandler,
                    flavor = draft.businessType.name,
                    onNext = { viewModel.navigateTo(WorkspaceSetupStep.BRANDING) },
                    onBack = { viewModel.navigateTo(WorkspaceSetupStep.PLAN_SELECTION) }
                )

                WorkspaceSetupStep.BRANDING -> {
                    SectionWrapper(
                        title = "Visual Identity",
                        onBack = { viewModel.navigateTo(WorkspaceSetupStep.PACKAGE_REVIEW) }
                    ) {
                        BrandingStepContent(
                            draft = draft,
                            orgBranding = orgBranding,
                            onUpdateBranding = viewModel::updateBranding,
                            onContinue = { viewModel.navigateTo(WorkspaceSetupStep.REVIEW) }
                        )
                    }
                }

                WorkspaceSetupStep.REVIEW -> {
                    ReviewRegistrationScreen(
                        draft = draft,
                        invoicePreviewState = invoicePreviewState,
                        onRefreshSubmitState = viewModel::previewInvoice,
                        actionResult = activationResult,
                        onConfirm = { viewModel.submitProvisioning() },
                        onBack = { viewModel.navigateTo(WorkspaceSetupStep.BRANDING) }
                    )
                }

                WorkspaceSetupStep.ACTIVATING -> {
                    UiResultView(activationResult) { result ->
                        OnboardingSuccessScreen(
                            businessName = draft.name,
                            productKey = result.productKey,
                            businessCode = result.businessCode,
                            previewImages = previewImages,
                            onComplete = {
                                viewModel.completed()
                                onComplete()
                            },
                            onSavePdf = {
                                viewModel.savePdf(
                                    businessName = draft.name,
                                    productKey = result.productKey,
                                    businessCode = result.businessCode,
                                    workspaceId = result.workspaceId
                                )
                            },
                            onPrint = {
                                viewModel.printCertificate(
                                    businessName = draft.name,
                                    productKey = result.productKey,
                                    businessCode = result.businessCode,
                                    workspaceId = result.workspaceId
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(activationResult) {
        if (activationResult is UiResult.Success) {
            viewModel.navigateTo(WorkspaceSetupStep.ACTIVATING)
        }
    }
}

@Composable
private fun BrandingStepContent(
    draft: WorkspaceSetupDraft,
    orgBranding: BrandingConfig?,
    onUpdateBranding: (BrandingConfig?) -> Unit,
    onContinue: () -> Unit
) {
    BrandingConfigScreen(
        currentAppName = draft.name,
        currentBranding = draft.branding ?: orgBranding,
        allowInheritOption = true,
        onSave = {
            onUpdateBranding(it)
            onContinue()
        },
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
    )
}

@Composable
private fun BrandingModeItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                alpha = 0.2f
            )
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = isSelected, onClick = null)
        }
    }
}
