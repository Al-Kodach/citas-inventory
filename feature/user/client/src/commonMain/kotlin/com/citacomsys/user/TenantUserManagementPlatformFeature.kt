package com.citacomsys.user

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.OrgNavigationPlugin
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.StaffNav
import com.citacomsys.core.spi.UserDetailNav
import com.citacomsys.core.spi.UserFormNav
import com.citacomsys.core.spi.UserWorkspaceBindingNav
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.types.UserCapability
import com.citacomsys.user.di.userModule
import com.citacomsys.user.ui.ActiveUsersWidget
import com.citacomsys.user.ui.screen.TenantUserListScreen
import com.citacomsys.user.ui.screen.UserDetailScreen
import com.citacomsys.user.ui.screen.UserFormScreen
import com.citacomsys.user.ui.screen.UserWorkspaceBindingScreen
import io.github.aakira.napier.Napier
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin

class TenantUserManagementCoreFeature : CoreFeature {

    override val id: String = "core.user.management"

    // Packaged, completely self-contained Navigation 3 & Dashboard configuration
    override val navigationPlugin: FeatureNavigationPlugin = object : OrgNavigationPlugin {
        override val id = "UserModule"

        override val serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(StaffNav::class, StaffNav.serializer())
                subclass(UserFormNav::class, UserFormNav.serializer())
                subclass(UserDetailNav::class, UserDetailNav.serializer())
                subclass(UserWorkspaceBindingNav::class, UserWorkspaceBindingNav.serializer())
            }
        }

        override val dashboardWidgets: List<PluginDashboardWidget> = listOf(
            PluginDashboardWidget(
                id = "active.user.sessions",
                placement = DashboardPlacement.LIST,
                order = 10,
                content = {
                    ActiveUsersWidget()
                }
            )
        )

        // Surfaced natively to populate your sidebar/nav suite scaffold layout
        override val tabs = listOf(
            PluginTabItem(
                route = StaffNav,
                label = "Staff",
                selectedIcon = AppIcons.People,
                unselectedIcon = AppIcons.People,
                order = 4
            )
        )

        override fun EntryProviderScope<NavKey>.registerEntries(
            navigator: Navigator,
            isCompact: Boolean
        ) {
            entry<StaffNav>(
                metadata = ListDetailScene.listPane()
            ) {
                TenantUserListScreen(
                    onUserSelected = { userId ->
                        navigator.navigateToSingleTop(UserDetailNav(userId))
                    },
                    onAddUser = {
                        navigator.navigateToSingleTop(UserFormNav())
                    },
                    onBindUser = {
                        navigator.navigateToSingleTop(UserWorkspaceBindingNav)
                    }
                )
            }

            entry<UserDetailNav>(
                metadata = ListDetailScene.detailPane()
            ) { key ->
                UserDetailScreen(
                    userId = key.userId,
                    onBack = { navigator.goBack() },
                    onEdit = { navigator.navigateToSingleTop(UserFormNav(key.userId)) }
                )
            }

            entry<UserFormNav>(
                metadata = ListDetailScene.detailPane()
            ) { key ->
                UserFormScreen(
                    userId = key.userId,
                    onBackClick = { navigator.goBack() }
                )
            }

            entry<UserWorkspaceBindingNav>(
                metadata = ListDetailScene.detailPane()
            ) { _ ->
                UserWorkspaceBindingScreen(
                    onBackClick = { navigator.goBack() }
                )
            }
        }

        override fun getRequiredCapabilities(key: NavKey): Set<UserCapability> {
            return when (key) {
                is StaffNav -> setOf(UserCapability.VIEW_USERS)
                is UserDetailNav -> setOf(UserCapability.VIEW_USERS)
                is UserFormNav -> setOf(UserCapability.MANAGE_USERS)
                is UserWorkspaceBindingNav -> setOf(UserCapability.MANAGE_USERS)
                else -> emptySet()
            }
        }
    }

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(userModule))
    }

    override fun deactivateService(koin: Koin) {
        Napier.d { "Tenant User Management Feature Deactivated" }
        koin.unloadModules(listOf(userModule))
    }
}
