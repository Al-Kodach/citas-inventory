package com.citacomsys.settings.ui.screen.profile.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.PasswordConfirmationDialog
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.sudo.OtpVerificationScreen
import com.citacomsys.shared.AppConfig
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserStatus
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = koinViewModel(),
    capabilityManager: UserCapabilityManager = koinInject(),
    appConfig: AppConfig = koinInject(),
    changePasswordViewModel: UserChangePasswordViewModel = koinInject(),
) {
    val state by viewModel.appUser.collectAsStateWithLifecycle(UiResult.Loading)
    val showPasswordPrompt by viewModel.showPasswordPrompt.collectAsStateWithLifecycle()
    val showOtpPrompt by viewModel.showOtpPrompt.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val navigator = LocalNavigator.current

    var showChangePassword by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            BackButton(onClick = { navigator.goBack() })

            UiResultView(
                state = state,
                modifier = Modifier.fillMaxWidth()
            ) { user ->
                if (user == null) {
                    Text("User profile data unavailable.")
                    return@UiResultView
                }

                Column(
                    modifier = Modifier
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // --- PROFILE HERO SECTION ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(96.dp),
                            border = BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(Modifier.width(28.dp))
                        Column {
                            Text(
                                user.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                user.phone.value,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            user.email?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            StatusBadge(user.status)
                        }
                    }

                    // --- 2. SECURITY & PRIVACY ---
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileSectionHeader("Security & Privacy")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.2f
                                )
                            ),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                ProfileSettingItem(
                                    title = "Update Password",
                                    subtitle = "Keep your account secure",
                                    icon = AppIcons.Lock,
                                    onClick = {
                                        showChangePassword = true
                                    }
                                )
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                AppIcons.VerifiedUser,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Multi-Factor Auth",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "SMS or Email verification for logins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                    }
                                    Switch(
                                        checked = user.mfaEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) viewModel.initiateMfaSetup() else viewModel.initiateMfaDisable()
                                        },
                                        enabled = actionState !is UiResult.Loading,
                                        modifier = Modifier.scale(0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // --- 3. WORK ACCOUNT DETAILS ---
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileSectionHeader("Work Account Details")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.2f
                                )
                            ),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val role = user.role.split("_").last()
                                val rd = try {
                                    TenantUserRole.valueOf(role).displayName(viewModel.productType)
                                } catch (_: Exception) {
                                    user.role
                                }

                                DetailRow("Workspace Role", rd.uppercase())
                                DetailRow("Access Group", user.userType.name.uppercase())
                                DetailRow(
                                    "Member Since",
                                    user.createdAt.toString().substringBefore('T')
                                )
                            }
                        }
                    }

                    // --- 4. ROLE CAPABILITIES ---
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileSectionHeader("Role Capabilities")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.1f
                                )
                            ),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            FlowRow(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val activeCaps =
                                    UserCapability.entries.filter { cap -> capabilityManager.has(cap) }
                                if (activeCaps.isEmpty()) {
                                    Text(
                                        "No specific capabilities assigned.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    activeCaps.forEach { cap ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = cap.name.replace("_", " ").lowercase()
                                                        .replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            icon = {
                                                Icon(
                                                    AppIcons.Check,
                                                    null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 4.dp),
            adapter = rememberScrollbarAdapter(scrollState = scrollState),
            style = defaultMaterialScrollbarStyle(),
            enablePressToScroll = true,
        )
    }

    // --- Overlays ---
    if (showPasswordPrompt) {
        PasswordConfirmationDialog(
            onConfirm = { viewModel.handleActionConfirmation(it) },
            onDismiss = { viewModel.dismissPrompts() },
            actionState = actionState
        )
    }

    if (showOtpPrompt) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            OtpVerificationScreen(
                handler = viewModel.authManager,
                onVerify = { viewModel.verifyMfaAction(it) },
                onResend = { viewModel.resendMfaCode() },
                onBack = { viewModel.dismissPrompts() },
                verifyButtonText = if (viewModel.isDisablingMfa()) "Verify & Disable" else "Verify & Enable"
            )
        }
    }

    if (showChangePassword) {
        Dialog(
            onDismissRequest = { showChangePassword = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            UserChangePasswordScreen(
                onBack = { showChangePassword = false },
                viewModel = changePasswordViewModel
            )
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ProfileSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(
                AppIcons.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(status: UserStatus) {
    val color =
        if (status == UserStatus.ACTIVE) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}
