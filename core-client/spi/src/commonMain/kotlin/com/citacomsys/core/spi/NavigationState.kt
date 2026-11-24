package com.citacomsys.core.spi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer

/**
 * Create a navigation state that persists config changes and process death.
 * Optimized for SaaS multi-module backstacks.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    savedStateConfiguration: SavedStateConfiguration
): NavigationState {

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        configuration = savedStateConfiguration,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(startRoute)
    }

    // Initialize backstacks for each top-level module (Dashboard, Tenants, etc.) and the startRoute
    val backStacks = (topLevelRoutes + startRoute).associateWith { key ->
        rememberNavBackStack(savedStateConfiguration, key)
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}


/**
 * State holder for navigation state.
 *
 * @param startRoute - the entry point of the app (usually Dashboard)
 * @param topLevelRoute - the current active module stack
 * @param backStacks - mapping of top-level keys to their respective history
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    var backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute

    /**
     * Returns the currently visible route at the top of the active stack.
     */
    val currentTop: NavKey
        get() = backStacks[topLevelRoute]?.lastOrNull() ?: topLevelRoute

    /**
     * Navigates to a new top-level route.
     */
    fun navigate(route: NavKey) {
        if (backStacks.containsKey(route)) {
            topLevelRoute = route
        } else {
            // Push to current stack
            backStacks[topLevelRoute]?.add(route)
        }
    }

    /**
     * Replaces the current route with a new one.
     */
    fun replace(route: NavKey) {
        val currentStack = backStacks[topLevelRoute]
        if (!currentStack.isNullOrEmpty()) {
            currentStack.removeAt(currentStack.size - 1)
        }
        navigate(route)
    }

    /**
     * Pops the current route from the stack.
     */
    fun pop() {
        val currentStack = backStacks[topLevelRoute]
        if (currentStack != null && currentStack.size > 1) {
            currentStack.removeAt(currentStack.size - 1)
        } else if (topLevelRoute != startRoute) {
            topLevelRoute = startRoute
        }
    }

    /**
     * Determines which stacks should remain in memory.
     * We always preserve the 'startRoute' stack (usually Dashboard)
     * plus the currently active module stack.
     */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    fun restoreState(state: NavigationState) {
//        startRoute = state.startRoute
//        topLevelRoute = state.topLevelRoute
        backStacks = state.backStacks
    }

    fun isEqual(state: NavigationState?) = this.backStacks == state?.backStacks
}


/**
 * Convert NavigationState into NavEntries for display.
 * Applies standard SaaS decorators (State Persistence).
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
