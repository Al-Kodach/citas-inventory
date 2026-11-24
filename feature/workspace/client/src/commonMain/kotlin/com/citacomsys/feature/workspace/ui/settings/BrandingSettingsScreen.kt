package com.citacomsys.feature.workspace.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.branding.BrandingConfigScreen
import com.citacomsys.core.branding.BrandingStrategy
import com.citacomsys.core.components.ResponsiveScreenWrapper
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.spi.LocalNavigator
import com.design.components.AppScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandingSettingsScreen(
    viewModel: WorkspaceBrandingViewModel = koinViewModel(),
    sessionManager: AppSessionManager = koinInject()
) {
    val currentBranding by viewModel.branding.collectAsStateWithLifecycle()
    val useOrgBranding by viewModel.useOrgBranding.collectAsStateWithLifecycle()
    val usePlatformBranding by viewModel.usePlatformBranding.collectAsStateWithLifecycle()
    val workStationInfo by sessionManager.workStationInfo.collectAsStateWithLifecycle()
    val navState = LocalNavigator.current

    val initialStrategy = when {
        useOrgBranding -> BrandingStrategy.INHERIT_ORG
        usePlatformBranding -> BrandingStrategy.PLATFORM_DEFAULT
        currentBranding != null -> BrandingStrategy.CUSTOM
        else -> if (workStationInfo.isOrgMode.not()) BrandingStrategy.INHERIT_ORG else BrandingStrategy.PLATFORM_DEFAULT
    }

    AppScaffold(
        title = "Appearance",
        onBack = navState::goBack,
    ) { padding ->
        ResponsiveScreenWrapper(
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BrandingConfigScreen(
                    currentAppName = workStationInfo.appName,
                    currentBranding = currentBranding,
                    allowInheritOption = workStationInfo.isOrgMode.not(),
                    initialStrategy = initialStrategy,
                    onStrategySelected = { strategy ->
                        when (strategy) {
                            BrandingStrategy.INHERIT_ORG -> viewModel.setUseOrgBranding(true)
                            BrandingStrategy.PLATFORM_DEFAULT -> viewModel.setUsePlatformBranding(true)
                            BrandingStrategy.CUSTOM -> {
                                viewModel.setUseOrgBranding(false)
                                viewModel.setUsePlatformBranding(false)
                            }
                        }
                    },
                    onSaveStrategy = { strategy, _ ->
                        when (strategy) {
                            BrandingStrategy.INHERIT_ORG -> viewModel.setUseOrgBranding(true)
                            BrandingStrategy.PLATFORM_DEFAULT -> viewModel.setUsePlatformBranding(true)
                            BrandingStrategy.CUSTOM -> {
                                viewModel.setUseOrgBranding(false)
                                viewModel.setUsePlatformBranding(false)
                            }
                        }
                    },
                    onSave = { config ->
                        viewModel.updateBranding(config)
                    }
                )
            }
        }
    }
}
