package com.citacomsys.inventory.admin.system.presentation.pkg

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citacomsys.api.platform.model.ProductPackage
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.inventory.admin.common.ui.AdminListScreenScaffold
import com.citacomsys.inventory.admin.system.AdminPackageDetailsNav
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackageListScreen(
    viewModel: AdminPackageListViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current

    AdminListScreenScaffold(
        viewModel = viewModel,
        title = "Product Packages",
        onBack = { navigator.goBack() },
        searchPlaceholder = "Search packages...",
        emptyTitle = "No Packages Configured",
        emptyDescription = "Use the 'Stage New Build' or 'Add Package' features to populate this registry.",
        emptyIcon = AppIcons.CardMembership,
        onClick = { pkg ->
            navigator.navigateToSingleTop(AdminPackageDetailsNav(pkg.id))
        }
    ) { pkg, _ ->
        PackageListItemContent(pkg)
    }
}

@Composable
private fun PackageListItemContent(pkg: ProductPackage) {
    ListItem(
        headlineContent = {
            Text(
                pkg.name,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
                          },
        supportingContent = {
            Column {
                Text(
                    text = pkg.tier.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                pkg.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = AppIcons.CardMembership,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        overlineContent = {
            if (pkg.recommended) {
                SuggestionChip(
                    onClick = { },
                    label = { Text("Recommended") },
                    icon = { Icon(AppIcons.AutoAwesome, null, modifier = Modifier.size(16.dp)) }
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
