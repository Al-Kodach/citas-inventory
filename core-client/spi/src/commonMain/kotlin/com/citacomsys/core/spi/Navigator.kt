package com.citacomsys.core.spi

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 * Refined to support sophisticated multi-backstack management for SaaS modules.
 */
class Navigator(
    val state: NavigationState,
    private val coordinator: NavigationCoordinator
) {
     val currentRoute: NavKey = state.topLevelRoute

    // Thread-safe storage for catching background push notification targets
     var pendingRedirectRoute: NavKey? = null

     fun navigateTo(route: NavKey) {
        state.navigate(route)
    }

     fun setPendingRedirect(route: NavKey?) {
        pendingRedirectRoute = route
    }

     fun clearPendingRedirect() {
        pendingRedirectRoute = null
    }

    /**
     * Safely executes any pending notification or deep link redirect route
     * by building out its proper structural backstack.
     */
     fun processPendingRedirect() {
        val route = pendingRedirectRoute ?: return

        val currentStack = state.backStacks[state.topLevelRoute]
        currentStack?.clear()

        // Reset the redirect pointer token cleanly
        pendingRedirectRoute = null
    }

    /**
     * Navigates to a route with Single Top behavior.
     * If the route is already at the top of the stack, it does nothing.
     */
     fun navigateToSingleTop(route: NavKey) {
        if (route == state.currentTop) return
        state.navigate(route)
    }

    /**
     * Pops the current route.
     * If we are at the root of a secondary tab, it navigates back to the start tab.
     */
     fun goBack() {
        state.pop()
    }

    /**
     * Resets a specific stack to its root.
     */
     fun resetToRoot(tab: NavKey) {
        val stack = state.backStacks[tab] ?: return
        while (stack.size > 1) {
            stack.removeLastOrNull()
        }
    }
}

/**
 * CompositionLocal used to provide the [Navigator] down the compose tree.
 * This allows any screen to trigger navigation without having to pass the backstack manually.
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator provided. Make sure to wrap your content in PlatformNavigationHost.")
}
