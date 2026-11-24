package com.citacomsys.inventory.admin.system.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.inventory.admin.system.AdminAppReleases
import com.citacomsys.inventory.admin.system.AdminPackageNav
import com.citacomsys.inventory.admin.system.AdminSmsPackageNav
import com.citacomsys.inventory.admin.system.AdminSystemSettingsNav
import com.citacomsys.inventory.admin.system.CapabilityMatrixNav
import com.citacomsys.inventory.admin.system.PlatformContentNav
import com.citacomsys.inventory.admin.system.PromoCampaignNav

/**
 * The landing hub for Platform Infrastructure and Marketing Content.
 * This screen is hosted within the global platform shell.
 */
@Composable
fun AdminSystemHomeScreen() {
    val navigator = LocalNavigator.current

    LazyVerticalGridView(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(320.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION: CORE INFRASTRUCTURE ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Core Infrastructure")
        }
        
        item {
            SystemHubCard(
                title = "Product Packages",
                description = "Manage service tiers, resource limits, and baseline feature modules.",
                icon = AppIcons.CardMembership,
                onClick = { navigator.navigateToSingleTop(AdminPackageNav) }
            )
        }
        item {
            SystemHubCard(
                title = "SMS Inventory",
                description = "Configure SMS bundle quantities and current market pricing for credits.",
                icon = AppIcons.Sms,
                onClick = { navigator.navigateToSingleTop(AdminSmsPackageNav) }
            )
        }
        item {
            SystemHubCard(
                title = "Roles & Permissions",
                description = "Configure global defaults for tiered roles and capability inheritance.",
                icon = AppIcons.Security,
                onClick = { navigator.navigateToSingleTop(CapabilityMatrixNav) }
            )
        }
        item {
            SystemHubCard(
                title = "App Releases",
                description = "Manage & Upload Apps",
                icon = Icons.Default.Apps,
                onClick = { navigator.navigateToSingleTop(AdminAppReleases) }
            )
        }
        item {
            SystemHubCard(
                title = "Global Settings",
                description = "Configure MoMo collection details, support contacts, and system maintenance.",
                icon = AppIcons.AdminPanelSettings,
                onClick = { navigator.navigateToSingleTop(AdminSystemSettingsNav) }
            )
        }

        // --- SECTION: GROWTH & MARKETING ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader("Growth & Marketing")
        }

        item {
            SystemHubCard(
                title = "Promotion Engine",
                description = "Manage discount campaigns, seasonal offers, and bonus credit rules.",
                icon = AppIcons.AutoAwesome,
                onClick = { navigator.navigateToSingleTop(PromoCampaignNav) }
            )
        }
        
        item {
            SystemHubCard(
                title = "Marketing CMS",
                description = "Update platform features, walkthrough demos, and customer testimonials.",
                icon = AppIcons.Layers,
                onClick = { navigator.navigateToSingleTop(PlatformContentNav) }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SystemHubCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
