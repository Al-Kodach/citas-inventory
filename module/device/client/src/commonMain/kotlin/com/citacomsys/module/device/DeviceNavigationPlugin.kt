package com.citacomsys.module.device

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.api.device.nav.DeviceListNav
import com.citacomsys.api.device.nav.WorkstationSummaryNav
import com.citacomsys.api.device.nav.deviceSerializerModule
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.OrgNavigationPlugin
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.module.device.ui.screen.DeviceSummaryScreen
import com.citacomsys.module.device.ui.widget.DeviceDashboardWidget
import com.citacomsys.module.device.ui.widget.DeviceRenameSettingsWidget
import com.citacomsys.types.UserCapability
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.SettingsGroup
import com.citacomsys.module.device.ui.screen.DeviceManagementScreen
import kotlinx.serialization.modules.SerializersModule

class DeviceNavigationPlugin : OrgNavigationPlugin {
    override val id: String = "DeviceModule"

    override val serializersModule: SerializersModule = deviceSerializerModule

    override val tabs: List<PluginTabItem> = listOf(
        PluginTabItem(
            route = DeviceListNav,
            label = "Devices",
            selectedIcon = AppIcons.DevicesFilled,
            unselectedIcon = AppIcons.DevicesOutlined,
            order = 5
        )
    )

    override val pluginSettingsContributions: List<PluginSettingsContribution> = listOf(
        PluginSettingsContribution.CustomWidget(
            id = "device.ID.setting",
            group = SettingsGroup.NODE,
            order = 5,
            content = {
                DeviceRenameSettingsWidget()
            }
        )
    )

    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<DeviceListNav>(
            metadata = ListDetailScene.listPane()
        ) {
            DeviceManagementScreen(
                onBack = if (isCompact) navigator::goBack else null,
                onDeviceClick = { hardwareId ->
                    navigator.navigateToSingleTop(WorkstationSummaryNav(hardwareId))
                }
            )
        }

        entry<WorkstationSummaryNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            DeviceSummaryScreen(
                hardwareId = key.hardwareId,
                onBack = { navigator.goBack() }
            )
        }
    }

    override fun getRequiredCapabilities(key: NavKey): Set<UserCapability> {
        return when (key) {
            is DeviceListNav -> setOf(UserCapability.MANAGE_ORGANIZATIONS)
            is WorkstationSummaryNav -> setOf(UserCapability.MANAGE_ORGANIZATIONS)
            else -> emptySet()
        }
    }
}
