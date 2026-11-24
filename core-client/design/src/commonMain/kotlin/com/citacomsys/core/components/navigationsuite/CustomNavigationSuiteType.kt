package com.citacomsys.core.components.navigationsuite

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass

/**
 * Calculates the [NavigationSuiteType] based on the [WindowSizeClass] breakpoints.
 * Uses the new WindowSizeClass API with direct breakpoint checks.
 */
fun customNavigationSuiteType(
    windowSizeClass: WindowSizeClass
): NavigationSuiteType {
    return when {
        // Expanded Width (>= 840dp)
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            // Instead of the heavy NavigationDrawer on desktop, use WideNavigationRail so we don't take up too much horizontal space
            when {
                // Short window (Landscape tablet)
                !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) ->
                    NavigationSuiteType.NavigationRail
                
                // For desktop & large tablets, use WideNavigationRail so users have more content width and can expand/collapse as needed
                else ->
                    NavigationSuiteType.WideNavigationRailCollapsed
            }
        }
        
        // Medium Width (>= 600dp)
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            if (!windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)) {
                // Short window (Landscape phone / split screen)
                NavigationSuiteType.NavigationBar
            } else {
                // Small tablet / Portrait large phone
                NavigationSuiteType.ShortNavigationBarMedium
            }
        }
        
        // Compact Width (< 600dp)
        else -> NavigationSuiteType.ShortNavigationBarCompact
    }
}
