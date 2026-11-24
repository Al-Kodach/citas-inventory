package com.citacomsys.inventory.admin.tenant.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.management.FilterBar
import com.citacomsys.core.ui.management.SaaSListScreenScaffold
import com.citacomsys.inventory.admin.common.ui.AdminListScreenScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Administrative screen for managing the list of organizations & workspaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTenantListScreen(
    onTenantClick: (String) -> Unit,
    onPreRegisterClick: () -> Unit = {},
    viewModel: OrganizationsListViewModel = koinInject()
) {
    val queryState by viewModel.queryState.collectAsStateWithLifecycle()

    SaaSListScreenScaffold(
        viewModel = viewModel,
        title = "Organizations",
        searchPlaceholder = "Search organizations by id or code...",
        emptyTitle = "No Organizations Found",
        emptyDescription = "There are no active or pending organizations matching your current search criteria.",
        emptyIcon = AppIcons.Business,
        topBarActions = {
            IconButton(onClick = onPreRegisterClick) {
                Icon(AppIcons.Add, contentDescription = "Pre-Register Organization")
            }
        },
        filterBar = {
            FilterBar(
                OrgStatus.entries.toList(),
                toString = { it?.name?.replace("_", " ")?.lowercase() },
                selectedStatus = queryState.filters["status"],
                onStatusSelected = { viewModel.filtersChanged("status", it) }
            )
        },
        onClick = { org ->
            onTenantClick(org.id)
        },
        modifier = Modifier.fillMaxSize()
    ) { org, selected ->
        TenantListItemContent(org, selected)
    }
}

@Composable
private fun TenantListItemContent(org: Organization, selected: Boolean) {
    ListItem(
        overlineContent = {
            TenantStatusBadge(org.status)
        },
        headlineContent = {
            Text(
                org.name,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = "Code: ${org.businessCode}",
                    overflow = TextOverflow.MiddleEllipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Phone: ${org.phone ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        leadingContent = {
            Icon(AppIcons.Business, contentDescription = null)
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            overlineColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        )
    )
}

@Composable
fun TenantStatusBadge(status: OrgStatus) {
    val color = when (status) {
        OrgStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        OrgStatus.SUSPENDED -> MaterialTheme.colorScheme.error
        OrgStatus.PENDING_VERIFICATION -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
