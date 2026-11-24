package com.citacomsys.feature.organization.ui.registration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.branding.BrandingConfigScreen
import com.design.components.AppScaffold
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.sudo.OtpVerificationScreen
import com.citacomsys.feature.platform.ui.welcome.WelcomeScreen
import com.design.components.AppListView
import com.design.components.AppSectionCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingHost(
    onCompleted: () -> Unit = {},
    onLoginHub: () -> Unit = {},
    onConnectWorkspace: () -> Unit = {},
    viewModel: OrganizationRegistrationViewModel = koinViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentStep by viewModel.onboardingState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "registration_flow"
        ) { step ->
            when (step) {
                RegistrationStep.WELCOME -> WelcomeScreen(
                    onNewRegistration = { viewModel.navigateTo(RegistrationStep.ORG_INFO) },
                    onLoginHub = onLoginHub
                )

                RegistrationStep.ORG_INFO -> BusinessInfoScreen(
                    draft = draft,
                    onUpdate = { name ->
                        viewModel.updateDraft { it.copy(organizationName = name) }
                    },
                    onNext = { viewModel.navigateTo(RegistrationStep.BRANDING) },
                    onBack = { viewModel.navigateTo(RegistrationStep.WELCOME) }
                )

                RegistrationStep.BRANDING -> {
                    AppScaffold(
                        title = "Branding",
                        onBack = { viewModel.navigateTo(RegistrationStep.ORG_INFO) },
                        actions = {
                            TextButton(
                                onClick = { viewModel.navigateTo(RegistrationStep.OWNER_INFO) }
                            ) {
                                Text("Skip")
                            }
                        }
                    ) { paddingValues ->
                        AppListView(modifier = Modifier.padding(paddingValues)) {
                            item {
                                BrandingConfigScreen(
                                    currentAppName = draft.organizationName,
                                    currentBranding = draft.branding,
                                    onSave = {
                                        viewModel.updateDraft { d -> d.copy(branding = it) }
                                        viewModel.navigateTo(RegistrationStep.OWNER_INFO)
                                    }
                                )
                            }
                        }
                    }
                }

                RegistrationStep.OWNER_INFO -> OwnerInfoScreen(
                    draft = draft,
                    uiState = uiState,
                    onUpdate = { name, phone, email, username, pass ->
                        viewModel.updateDraft {
                            it.copy(
                                ownerName = name,
                                ownerPhone = phone,
                                ownerEmail = email,
                                username = username,
                                ownerPassword = pass
                            )
                        }
                    },
                    onNext = { viewModel.submit() },
                    onBack = { viewModel.navigateTo(RegistrationStep.BRANDING) }
                )

                RegistrationStep.VERIFICATION -> {
                    AppSectionCard {
                        OtpVerificationScreen(
                            handler = viewModel.otpHandler,
                            onVerify = { viewModel.verify(it) },
                            onResend = { /* Resend logic already triggered in VM/Handler */ },
                            onBack = { viewModel.navigateTo(RegistrationStep.OWNER_INFO) }
                        )
                    }

                }

                RegistrationStep.COMPLETED -> {
                    OnboardingCompleted(
                        draft = draft,
                        onCompleted = {
                            onCompleted()
                            viewModel.reset()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCompleted(
    draft: OrgRegistrationDraft,
    onCompleted: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val availableWidth = maxWidth
        val isWide = availableWidth >= 840.dp
        val isMedium = availableWidth in 600.dp..839.dp
        val isCompact = availableWidth < 600.dp

        val horizontalPadding = when {
            isWide -> 48.dp
            isMedium -> 32.dp
            else -> 16.dp
        }

        val maxContainerWidth = when {
            isWide -> 960.dp
            isMedium -> 720.dp
            else -> 540.dp
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxContainerWidth),
            contentPadding = PaddingValues(
                horizontal = horizontalPadding,
                vertical = if (isCompact) 20.dp else 36.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 16.dp else 24.dp)
        ) {
            // 1. Success Hero Badge & Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(if (isCompact) 72.dp else 88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = "Success",
                                modifier = Modifier.size(if (isCompact) 40.dp else 48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Organization Established!",
                        style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Your legal parent organization has been successfully registered on the Citas Platform. You can now access your Mother Hub to provision and manage your industry workspaces.",
                        style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
            }

            // 2. Middle Content Section (2 Columns on Wide, Single Column on Medium/Compact)
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EntityDetailsCard(draft)
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            NextStepsGuidanceCard()
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EntityDetailsCard(draft)
                        NextStepsGuidanceCard()
                    }
                }
            }

            // 3. Primary CTA Button
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 50.dp else 56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Enter Organization Hub",
                            style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityDetailsCard(draft: OrgRegistrationDraft) {
    AppSectionCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = AppIcons.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Established Legal Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailItemRow(
                    label = "Organization Name",
                    value = draft.organizationName,
                    icon = AppIcons.Business
                )
                DetailItemRow(
                    label = "Primary Owner",
                    value = draft.ownerName,
                    icon = AppIcons.Person
                )
                if (draft.username.isNotBlank()) {
                    DetailItemRow(
                        label = "Owner Username",
                        value = "@${draft.username}",
                        icon = AppIcons.AccountCircle
                    )
                }
                if (draft.ownerPhone.isNotBlank()) {
                    DetailItemRow(
                        label = "Contact Phone",
                        value = draft.ownerPhone,
                        icon = AppIcons.Call
                    )
                }
                if (draft.ownerEmail.isNotBlank()) {
                    DetailItemRow(
                        label = "Contact Email",
                        value = draft.ownerEmail,
                        icon = AppIcons.Email
                    )
                }
            }
        }
    }
}

@Composable
private fun NextStepsGuidanceCard() {
    AppSectionCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Next Steps on Your Mother Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            NextStepGuideItem(
                stepNumber = "1",
                title = "Provision Workspaces",
                description = "Create dedicated operational platforms (Pharmacy, Retail POS, Grocery, etc.) or multi-branch stores."
            )
            NextStepGuideItem(
                stepNumber = "2",
                title = "Custom Branding & Themes",
                description = "Assign distinct trading names, logos, and color palettes per location to easily identify units."
            )
            NextStepGuideItem(
                stepNumber = "3",
                title = "Hardware & Staff Permissions",
                description = "Cryptographically bind physical POS workstations and configureWorkspace granular staff access capabilities directly from the Hub."
            )
        }
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NextStepGuideItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
