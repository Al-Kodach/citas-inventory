package com.citacomsys.app.shared.nav

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.scenes.BottomSheetSceneStrategy
import com.citacomsys.core.spi.scenes.rememberListDetailSceneStrategy2
import com.citacomsys.core.spi.toEntries
import com.citacomsys.core.splash.SplashScreen

/**
 * A pure navigation host that manages the navigation graph and backstack display.
 * This allows app flavors (Mobile, Desktop) to wrap it in their own custom layouts.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SaaSNavHost(
    modifier: Modifier = Modifier,
    navigator: Navigator,
    coordinator: NavigationCoordinator,
    isCompact: Boolean,
    legacyDefinitions: @Composable EntryProviderScope<NavKey>.(Navigator) -> Unit = {}
) {
    val provider = entryProvider {
        with(coordinator) { assembleAllEntries(navigator, isCompact) }
        legacyDefinitions(navigator)
    }

    SharedTransitionLayout {
        NavDisplay(
            entries = navigator.state.toEntries(provider),
            onBack = { navigator.goBack() },
            sceneStrategies = listOf(
                DialogSceneStrategy(),
                BottomSheetSceneStrategy(),
                rememberListDetailSceneStrategy2(),
                rememberListDetailSceneStrategy(),
                SinglePaneSceneStrategy(),
            ),
            modifier = modifier.fillMaxSize()
        )
    }
}


@Composable
fun UnauthorizedAccessScreen(onGoBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Access Locked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Access Restricted",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Your operator account lacks the capabilities required to view this screen.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onGoBack) {
                Text("Go Back")
            }
        }
    }
}
