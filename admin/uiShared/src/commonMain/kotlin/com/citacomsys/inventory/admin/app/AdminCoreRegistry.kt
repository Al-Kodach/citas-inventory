package com.citacomsys.inventory.admin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.BackupNav
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.TelemetryNav
import com.citacomsys.core.spi.TenantSecurityNav
import com.citacomsys.feature.telemetry.presentation.screen.LocalTelemetryAuditScreen
import com.citacomsys.feature.telemetry.presentation.screen.AdminSecurityHubScreen
import com.citacomsys.inventory.admin.app.nav.AdminDashboardNavKey
import com.citacomsys.inventory.admin.app.nav.AdminSettingsNavKey
import com.citacomsys.inventory.admin.app.nav.AdminUserProfileNav
import com.citacomsys.inventory.admin.app.dashboard.presentation.AdminDashboardScreen
import com.citacomsys.settings.ui.screen.profile.user.UserProfileScreen
import com.citacomsys.settings.ui.screen.settings.SettingsScreen
import com.design.components.AppSettingsItem
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.SettingsGroup
import com.citacomsys.core.spi.UserProfileNav
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import org.koin.compose.koinInject
import org.koin.core.Koin

class AdminCoreFeatures : CoreFeature {
    override val id: String = "core.admin"
    override val navigationPlugin: FeatureNavigationPlugin = AdminCoreNavigationPlugin()

    override fun activate(koin: Koin) {}
}

class AdminCoreNavigationPlugin : FeatureNavigationPlugin {
    override val id = "CoreModule"

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule = SerializersModule {}

    override val tabs = listOf(
        PluginTabItem(
            route = AdminDashboardNavKey,
            label = "Dashboard",
            order = 1,
            selectedIcon = AppIcons.DashboardFilled,
            unselectedIcon = AppIcons.DashboardOutlined
        ),
        PluginTabItem(
            route = AdminSettingsNavKey,
            label = "Settings",
            order = 999,
            selectedIcon = AppIcons.SettingsFilled,
            unselectedIcon = AppIcons.SettingsOutlined
        )
    )

    override val pluginSettingsContributions: List<PluginSettingsContribution> = listOf(
        PluginSettingsContribution.CustomWidget(
            id = "core.updates",
            group = SettingsGroup.SYSTEM,
            order = 0,
            content = {}
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.security_hub",
            group = SettingsGroup.SECURITY,
            order = 0,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Security Hub",
                    subtitle = "Platform-wide audit feed and policies",
                    icon = AppIcons.Security,
                    onClick = { navigator.navigateToSingleTop(TenantSecurityNav) }
                )
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.telemetry",
            group = SettingsGroup.SECURITY,
            order = 1,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Technical Telemetry",
                    subtitle = "Inspect local hardware execution logs",
                    icon = AppIcons.Smartphone,
                    onClick = { navigator.navigateToSingleTop(TelemetryNav) }
                )
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.personalization",
            group = SettingsGroup.PERSONALIZATION,
            order = 0,
            content = {
                PersonalizationSettingsSection()
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.backup",
            group = SettingsGroup.SECURITY,
            order = 2,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Data Portability",
                    subtitle = "Export encrypted workspace data for recovery",
                    icon = AppIcons.Backup,
                    onClick = { navigator.navigateToSingleTop(BackupNav) }
                )
            }
        )
    )

    // Declarative Core Route Definitions
    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<AdminDashboardNavKey> {
            AdminDashboardScreen()
        }

        entry<AdminSettingsNavKey> {
            SettingsScreen(
                onHeaderClick = {
                    navigator.navigateToSingleTop(UserProfileNav)
                },
                onBack = if (isCompact) navigator::goBack else null
            )
        }

        entry<AdminUserProfileNav>() {
            UserProfileScreen()
        }

        entry<TelemetryNav> {
            LocalTelemetryAuditScreen()
        }

        entry<TenantSecurityNav> {
            AdminSecurityHubScreen()
        }
    }
}

@Composable
private fun PersonalizationSettingsSection(
    prefs: PreferencesRepository = koinInject()
) {
    val darkMode by prefs.isDarkTheme.collectAsStateWithLifecycle()
    val fontScale by prefs.userFontScale.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Dark Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Switch between light and dark themes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Switch(checked = darkMode ?: false, onCheckedChange = { prefs.toggleTheme() })
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
                    onValueChange = { prefs.setFontScale(it.toInt()) },
                    valueRange = 1F..4F,
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
