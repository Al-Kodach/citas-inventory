package com.citacomsys.core.components.navigationsuite

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import kotlinx.coroutines.launch



/**
 * A custom adaptive navigation scaffold that manages the UI shell.
 *
 * Supports a "Hybrid Mode" for small screens with many navItems:
 * - If navItems <= 5: standard adaptive behavior (Bottom Bar on mobile).
 * - If navItems > 5: Bottom Bar shows the first 4 navItems + a "More" button to open a Drawer.
 *
 * @param navigationSuiteState Controls the visibility/animation of the drawer.
 * @param layoutType The determined layout (BottomBar, Rail, or Drawer).
 * @param navItems The list of top-level navigation navItems.
 * @param isSelected Predicate to determine if an item is currently active.
 * @param onNavigationItemClick Callback for module switching.
 * @param topBar A global app bar that wraps all module content.
 */
@Composable
fun CustomNavigationSuiteScaffoldLayout(
    navigationSuiteState: NavigationSuiteScaffoldState,
    layoutType: NavigationSuiteType,
    navItems: List<NavigationItem>,
    isSelected: (NavigationItem) -> Boolean,
    onNavigationItemClick: (NavigationItem) -> Unit,
    topBar: @Composable (onToggleMenu: () -> Unit) -> Unit = { it() },
    footerContent: @Composable () -> Unit = {},
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val wideNavigationRailState = rememberWideNavigationRailState()

    // Determine if we should use Hybrid Mode (Bottom Bar + "More" Drawer)
    val isCompact = layoutType == NavigationSuiteType.NavigationBar ||
            layoutType == NavigationSuiteType.ShortNavigationBarCompact ||
            layoutType == NavigationSuiteType.ShortNavigationBarMedium

    val useHybridMode = isCompact && navItems.size > 5

    if (useHybridMode) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val primaryItems = navItems.take(4)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            val (primaryItems, footerItems) = navItems.partition { it.label != "Settings" }

                            primaryItems.forEach { item ->
                                val selected = isSelected(item)
                                NavigationDrawerItem(
                                    modifier = Modifier.padding(
                                        vertical = 4.dp,
                                        horizontal = 12.dp
                                    ),
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
                                        onNavigationItemClick(item)
                                    }
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            footerItems.forEach { item ->
                                val selected = isSelected(item)
                                NavigationDrawerItem(
                                    modifier = Modifier.padding(
                                        vertical = 4.dp,
                                        horizontal = 12.dp
                                    ),
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
                                        onNavigationItemClick(item)
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.padding(16.dp)) {
                            footerContent()
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    topBar {
                        scope.launch { drawerState.open() }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = colors.shortNavigationBarContainerColor,
                        contentColor = colors.shortNavigationBarContentColor,
                    ) {
                        primaryItems.forEach { item ->
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
                                onClick = { onNavigationItemClick(item) }
                            )
                        }
                        // "More" button opens the drawer
                        NavigationBarItem(
                            selected = false,
                            onClick = { scope.launch { drawerState.open() } },
                            icon = { Icon(AppIcons.Menu, contentDescription = "More") },
                            label = { Text("More") }
                        )
                    }
                },
            ) { padding ->
                Box(
                    Modifier.padding(padding)
                        .fillMaxSize(),
                    content = content
                )
            }
        }
        return
    }

    // Standard Adaptive Logic (Rail, Wide Rail, Drawer, or simple Bottom Bar)
    LaunchedEffect(layoutType) {
        when (layoutType) {
            NavigationSuiteType.WideNavigationRailExpanded -> wideNavigationRailState.expand()
            NavigationSuiteType.WideNavigationRailCollapsed -> wideNavigationRailState.collapse()
            else -> Unit
        }
    }

    NavigationSuiteScaffoldLayout(
        state = navigationSuiteState,
        navigationSuite = {
            when (layoutType) {
                NavigationSuiteType.ShortNavigationBarCompact,
                NavigationSuiteType.ShortNavigationBarMedium,
                NavigationSuiteType.NavigationBar -> {
                    ShortNavigationBar(
                        containerColor = colors.shortNavigationBarContainerColor,
                        contentColor = colors.shortNavigationBarContentColor,
                    ) {
                        navItems.forEach { item ->
                            val selected = isSelected(item)
                            ShortNavigationBarItem(
                                iconPosition = if (layoutType == NavigationSuiteType.ShortNavigationBarCompact)
                                    NavigationItemIconPosition.Top else NavigationItemIconPosition.Start,
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.icon,
                                        null
                                    )
                                },
                                label = { Text(item.label) },
                                selected = selected,
                                onClick = { onNavigationItemClick(item) },
                            )
                        }
                    }
                }

                NavigationSuiteType.WideNavigationRailCollapsed,
                NavigationSuiteType.WideNavigationRailExpanded -> {
                    val isExpanded =
                        wideNavigationRailState.targetValue == WideNavigationRailValue.Expanded
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        WideNavigationRail(
                            state = wideNavigationRailState,
                            colors = colors.wideNavigationRailColors,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    navItems.forEach { item ->
                                        val selected = isSelected(item)
                                        WideNavigationRailItem(
                                            modifier = Modifier.padding(
                                                vertical = if (isExpanded) 2.dp else 4.dp,
                                                horizontal = if (isExpanded) 8.dp else 4.dp
                                            ),
                                            iconPosition = if (isExpanded)
                                                NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                                            icon = {
                                                Icon(
                                                    if (selected) item.selectedIcon else item.icon,
                                                    null
                                                )
                                            },
                                            label = { Text(item.label) },
                                            selected = selected,
                                            onClick = { onNavigationItemClick(item) },
                                            railExpanded = isExpanded,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(
                                            bottom = 16.dp,
                                            start = if (isExpanded) 16.dp else 8.dp,
                                            end = if (isExpanded) 16.dp else 8.dp
                                        )
                                ) {
                                    footerContent()
                                }
                            }
                        }
                    }
                }

                NavigationSuiteType.NavigationRail -> {
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRail(
                            containerColor = colors.navigationRailContainerColor,
                            contentColor = colors.navigationRailContentColor,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                navItems.forEach { item ->
                                    val selected = isSelected(item)
                                    NavigationRailItem(
                                        modifier = Modifier.padding(
                                            vertical = 4.dp,
                                            horizontal = 4.dp
                                        ),
                                        icon = {
                                            Icon(
                                                if (selected) item.selectedIcon else item.icon,
                                                null
                                            )
                                        },
                                        label = { Text(item.label) },
                                        selected = selected,
                                        onClick = { onNavigationItemClick(item) },
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                                ) {
                                    footerContent()
                                }
                            }
                        }
                    }
                }

                NavigationSuiteType.NavigationDrawer -> {
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        PermanentDrawerSheet(
                            drawerContainerColor = colors.navigationDrawerContainerColor,
                            drawerContentColor = colors.navigationDrawerContentColor,
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val (primaryItems, footerItems) = navItems.partition { it.label != "Settings" }

                                    primaryItems.forEach { item ->
                                        val selected = isSelected(item)
                                        NavigationDrawerItem(
                                            modifier = Modifier.padding(
                                                vertical = 4.dp,
                                                horizontal = 12.dp
                                            ),
                                            icon = {
                                                Icon(
                                                    if (selected) item.selectedIcon else item.icon,
                                                    null
                                                )
                                            },
                                            label = { Text(item.label) },
                                            selected = selected,
                                            onClick = { onNavigationItemClick(item) },
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    footerItems.forEach { item ->
                                        val selected = isSelected(item)
                                        NavigationDrawerItem(
                                            modifier = Modifier.padding(
                                                vertical = 4.dp,
                                                horizontal = 12.dp
                                            ),
                                            icon = {
                                                Icon(
                                                    if (selected) item.selectedIcon else item.icon,
                                                    null
                                                )
                                            },
                                            label = { Text(item.label) },
                                            selected = selected,
                                            onClick = { onNavigationItemClick(item) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    footerContent()
                                }
                            }
                        }
                    }
                }
            }
        },
        layoutType = layoutType,
        content = {
            val isSidebar = layoutType == NavigationSuiteType.WideNavigationRailCollapsed ||
                    layoutType == NavigationSuiteType.WideNavigationRailExpanded ||
                    layoutType == NavigationSuiteType.NavigationRail ||
                    layoutType == NavigationSuiteType.NavigationDrawer

            Row(modifier = Modifier.fillMaxSize()) {
                if (isSidebar) {
                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                }
                Scaffold(
                    topBar = {
                        topBar {
                            scope.launch {
                                when (layoutType) {
                                    NavigationSuiteType.NavigationDrawer -> navigationSuiteState.show()
                                    NavigationSuiteType.WideNavigationRailCollapsed,
                                    NavigationSuiteType.WideNavigationRailExpanded -> {
                                        if (wideNavigationRailState.targetValue == WideNavigationRailValue.Expanded)
                                            wideNavigationRailState.collapse()
                                        else wideNavigationRailState.expand()
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { padding ->
                    MainContentBox(
                        navigationSuiteState = navigationSuiteState,
                        layoutType = layoutType,
                        content = {
                            Box(
                                Modifier.padding(padding)
                                    .fillMaxSize()
                            ) {
                                content()
                            }
                        }
                    )
                }
            }
        }
    )
}
