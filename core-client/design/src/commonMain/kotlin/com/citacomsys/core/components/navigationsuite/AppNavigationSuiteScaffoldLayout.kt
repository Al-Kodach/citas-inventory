package com.citacomsys.core.components.navigationsuite

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.domain.repository.PreferencesRepository
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.compose.mutableStateOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * A generic navigation item model to decouple the UI shell from specific app routes.
 */
data class NavigationItem(
    val id: NavKey,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val isFooter: Boolean = false
)

/**
 * A custom adaptive navigation scaffold that manages the UI shell with a "floating card" design.
 * Supports Hybrid Mode for compact screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationSuiteScaffoldLayout(
    navigationSuiteState: NavigationSuiteScaffoldState,
    layoutType: NavigationSuiteType,
    navItems: List<NavigationItem>,
    appPrefs: PreferencesRepository = koinInject(),
    prefs: KSafe = koinInject(),
    isSelected: (NavigationItem) -> Boolean,
    selectedNavItemId: NavKey? = null,
    onNavClick: (NavigationItem) -> Unit,
    systemTopBar: @Composable () -> Unit = {},
    sidebarHeaderContent: @Composable (isExpanded: Boolean) -> Unit = {},
    footerContent: @Composable (isExpanded: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val fontScale by appPrefs.userFontScale.collectAsStateWithLifecycle(1)

    // Automatic expansion logic hub on screen width
    val widthClass = currentWindowAdaptiveInfoV2().windowSizeClass
    var isNavExpanded by prefs.mutableStateOf(widthClass.minWidthDp > 600)
    val showBottomBar = layoutType == NavigationSuiteType.NavigationBar

    // Hybrid Mode logic for Bottom Bar crowding
    val maxItems = when (fontScale) {
        3 -> 2
        else -> 3 // + more icon == 4 items
    }

    val useHybridMode = showBottomBar && navItems.size > maxItems
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(useHybridMode) {
        if (useHybridMode) {
            drawerState.close()
        }
    }

    val contentBody = @Composable {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
        ) {
            //  Status Bar always on top
            systemTopBar()

            Row(modifier = Modifier.weight(1f)) {
                // Sidebar Navigation (Rail/Drawer substitute for larger screens)
                if (!showBottomBar) {
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxHeight()
                            .animateContentSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .width(if (isNavExpanded) 240.dp else 72.dp)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                if (isNavExpanded) Spacer(Modifier.weight(1f))
                                IconButton(onClick = { isNavExpanded = !isNavExpanded }) {
                                    Icon(
                                        if (isNavExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                                        contentDescription = "Toggle Sidebar"
                                    )
                                }
                            }

                            // Render custom sidebar header content (e.g., Context Switcher & Branch Switcher)
                            sidebarHeaderContent(isNavExpanded)

                            val primary = navItems.filter { it.label != "Settings" }

                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                            ) {
                                primary.forEach { item ->
                                    NavigationItemView(
                                        item = item,
                                        selected = isSelected(item),
                                        expanded = isNavExpanded,
                                        onClick = { onNavClick(item) }
                                    )
                                }
                            }

                            Box(Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                                footerContent(isNavExpanded)
                            }
                        }
                    }
                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                }

                // Content Area (Floating Page Effect)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (showBottomBar) 0.dp else 16.dp,
                            end = if (showBottomBar) 0.dp else 16.dp,
                            bottom = if (showBottomBar) 0.dp else 16.dp,
                            top = if (showBottomBar) 0.dp else 16.dp
                        )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = if (showBottomBar) RectangleShape else RoundedCornerShape(16.dp),
                        tonalElevation = if (showBottomBar) 0.dp else 2.dp,
                        shadowElevation = if (showBottomBar) 0.dp else 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentWindowInsets = WindowInsets(0.dp),
                            bottomBar = {
                                if (showBottomBar) {
                                    val displayItems =
                                        if (useHybridMode) navItems.take(maxItems)
                                            .associateBy { it.id } else navItems.associateBy { it.id }

                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {

                                        displayItems.forEach { (_, item) ->
                                            val selected = isSelected(item)
                                            NavigationBarItem(
                                                icon = {
                                                    Icon(
                                                        if (selected) item.selectedIcon else item.icon,
                                                        null
                                                    )
                                                },
                                                label = { Text(item.label) },
                                                selected = selected,
                                                onClick = { onNavClick(item) }
                                            )
                                        }

                                        if (useHybridMode) {
                                            val isMoreSelected = displayItems.values.none { isSelected(it) }
                                            NavigationBarItem(
                                                selected = isMoreSelected,
                                                icon = { Icon(Icons.Default.Menu, "More") },
                                                label = { Text("More") },
                                                onClick = { scope.launch { drawerState.open() } }
                                            )
                                        }
                                    }
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                                MainContentBox(
                                    navigationSuiteState = navigationSuiteState,
                                    layoutType = layoutType,
                                    content = content
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (useHybridMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    LazyListView(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(navItems) { item ->
                            val selected = isSelected(item)
                            NavigationDrawerItem(
                                modifier = Modifier.padding(vertical = 2.dp),
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.icon,
                                        null
                                    )
                                },
                                label = { Text(item.label) },
                                selected = selected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onNavClick(item)
                                }
                            )
                        }
                        item {
                            Spacer(Modifier.weight(1f))
                        }
                        item {
                            footerContent(true)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            },
            content = contentBody
        )
    } else {
        contentBody()
    }
}

@Composable
private fun NavigationItemView(
    item: NavigationItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        icon = {
            Icon(
                if (selected) item.selectedIcon else item.icon,
                null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            if (expanded) {
                Text(
                    item.label,
                    maxLines = 1,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            unselectedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
