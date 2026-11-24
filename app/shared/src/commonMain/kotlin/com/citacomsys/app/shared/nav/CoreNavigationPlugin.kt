package com.citacomsys.app.shared.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.citacomsys.api.workspace.WorkspaceBrandNav
import com.citacomsys.app.shared.sync.SyncStoreManagerScreen
import com.citacomsys.backup.api.nav.CreateBackupNav
import com.citacomsys.backup.api.nav.RestoreBackupNav
import com.citacomsys.backup.ui.BackupRestoreHomeScreen
import com.citacomsys.backup.ui.CreateBackupScreen
import com.citacomsys.backup.ui.RestoreBackupScreen
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.BackupNav
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.SyncManagerNav
import com.citacomsys.core.spi.TelemetryNav
import com.citacomsys.core.spi.TenantSecurityNav
import com.citacomsys.core.spi.UserProfileNav
import com.citacomsys.feature.telemetry.presentation.screen.AdminSecurityHubScreen
import com.citacomsys.feature.telemetry.presentation.screen.LocalTelemetryAuditScreen
import com.citacomsys.home.di.homeFeatureModule
import com.citacomsys.settings.ui.screen.profile.user.UserProfileScreen
import com.citacomsys.settings.ui.screen.settings.SettingsSyncInfoPanel
import com.citacomsys.settings.ui.screen.settings.SettingsUpdateSection
import com.citacomsys.settings.ui.screen.settings.SettingsViewModel
import com.design.components.AppSettingsItem
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.SettingsGroup
import com.citacomsys.types.UserCapability
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import org.koin.core.Koin

class AppCoreFeatures : CoreFeature {
    override val id: String = "core.platform"
    override val navigationPlugin: FeatureNavigationPlugin = CoreNavigationPlugin()

    override fun activate(
        koin: Koin
    ) {
        koin.loadModules(listOf(homeFeatureModule))
    }

    override fun deactivateService(koin: Koin) {
        koin.unloadModules(listOf(homeFeatureModule))
    }
}

class CoreNavigationPlugin : FeatureNavigationPlugin {
    override val id = "CoreModule"

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SyncManagerNav::class, SyncManagerNav.serializer())
            subclass(TelemetryNav::class, TelemetryNav.serializer())
            subclass(TenantSecurityNav::class, TenantSecurityNav.serializer())
            subclass(BackupNav::class, BackupNav.serializer())
            subclass(CreateBackupNav::class, CreateBackupNav.serializer())
            subclass(RestoreBackupNav::class, RestoreBackupNav.serializer())
        }
    }

    override val pluginSettingsContributions: List<PluginSettingsContribution> = listOf(
        // SECURITY
        PluginSettingsContribution.CustomWidget(
            id = "core.telemetry",
            group = SettingsGroup.SECURITY,
            order = 2,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Device Telemetry",
                    subtitle = "Inspect local hardware execution logs",
                    icon = AppIcons.Smartphone,
                    onClick = { navigator.navigateToSingleTop(TelemetryNav) }
                )
            }
        ),
        // PERSONALIZATION
        PluginSettingsContribution.CustomWidget(
            id = "branding",
            group = SettingsGroup.PERSONALIZATION,
            order = 0,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Visual Identity",
                    subtitle = "Branding, colors and custom styles",
                    icon = AppIcons.Palette,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navigator.navigateToSingleTop(WorkspaceBrandNav) },
                )
            },
            requiredCapabilities = setOf(UserCapability.MANAGE_SETTINGS)
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.personalization",
            group = SettingsGroup.PERSONALIZATION,
            order = 1,
            content = {
                val viewModel: SettingsViewModel = koinInject()
                PersonalizationSettingsSection(viewModel)
            }
        ),
        // CONTINUITY / SECURITY
        PluginSettingsContribution.CustomWidget(
            id = "core.backup",
            group = SettingsGroup.SECURITY,
            order = 3,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Data Portability",
                    subtitle = "Export encrypted data for recovery",
                    icon = AppIcons.Backup,
                    onClick = { navigator.navigateToSingleTop(BackupNav) }
                )
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.data_metrics",
            group = SettingsGroup.SYSTEM,
            order = 1,
            content = {
                SettingsSyncInfoPanel()
            }
        ),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {

        entry<UserProfileNav> {
            UserProfileScreen()
        }

        entry<SyncManagerNav> { SyncStoreManagerScreen(onBack = { navigator.goBack() }) }
        entry<TelemetryNav> { LocalTelemetryAuditScreen() }
        entry<TenantSecurityNav> { AdminSecurityHubScreen() }

        entry<BackupNav> {
            BackupRestoreHomeScreen(
                onBack = { navigator.goBack() },
                onCreateBackup = { navigator.navigateToSingleTop(CreateBackupNav) },
                onRestoreBackup = { navigator.navigateToSingleTop(RestoreBackupNav(null)) }
            )
        }

        entry<CreateBackupNav>(
            metadata = DialogSceneStrategy.dialog(
                dialogProperties = DialogProperties(
                    dismissOnClickOutside = false,
                    dismissOnBackPress = false,
                    usePlatformDefaultWidth = true
                )
            )
        ) {
            CreateBackupScreen(onBack = { navigator.goBack() })
        }

        entry<RestoreBackupNav>(
            metadata = DialogSceneStrategy.dialog(
                dialogProperties = DialogProperties(
                    dismissOnClickOutside = false,
                    dismissOnBackPress = false,
                    usePlatformDefaultWidth = true
                )
            )
        ) {
            RestoreBackupScreen()
        }
    }
}

@Composable
private fun PersonalizationSettingsSection(viewModel: SettingsViewModel) {
    val darkMode by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val brandingConfig by viewModel.branding.collectAsStateWithLifecycle()
    val isDarkTheme = darkMode ?: brandingConfig?.defaultDarkMode ?: isSystemInDarkTheme()
    val fontScale by viewModel.userFontScale.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Theme Mode Segmented Toggle
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Theme Mode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Switch between Light and Dark interface themes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                SegmentedButton(
                    selected = !isDarkTheme,
                    onClick = { if (isDarkTheme) viewModel.toggleTheme() },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = AppIcons.LightMode,
                            contentDescription = "Light Theme",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("Light")
                }

                SegmentedButton(
                    selected = isDarkTheme,
                    onClick = { if (!isDarkTheme) viewModel.toggleTheme() },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = AppIcons.DarkMode,
                            contentDescription = "Dark Theme",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("Dark")
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // Font Size Selector
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp)) {
            Text(
                "Interface Scale",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Adjust the readability of text and UI elements globally",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        MaterialTheme.shapes.medium
                    )
            ) {
                Text(
                    "A",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = fontScale.toFloat(),
                    onValueChange = { viewModel.setFontScale(it.toInt()) },
                    valueRange = 1f..4f,
                    steps = 4,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                Text(
                    "A",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
