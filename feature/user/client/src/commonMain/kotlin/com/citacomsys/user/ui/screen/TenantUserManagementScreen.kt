package com.citacomsys.user.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.management.SaaSListScreenScaffold
import com.citacomsys.shared.displayName
import com.citacomsys.shared.util.toRoleDisplayName
import com.citacomsys.types.ProductType
import com.citacomsys.types.UserItem
import com.citacomsys.types.UserStatus
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantUserListScreen(
    onUserSelected: (String) -> Unit,
    onAddUser: () -> Unit,
    onBindUser: () -> Unit,
    viewModel: TenantUserListViewModel = koinViewModel()
) {
    val isExpanded = LocalBackButtonVisibility.current.not()

    SaaSListScreenScaffold(
        viewModel = viewModel,
        title = "Staff Management",
        searchPlaceholder = "Search staff id or username...",
        onClick = {  },
        topBarActions = {
            IconButton(onClick = onBindUser) {
                Icon(AppIcons.Business, "Bind User to Workspace")
            }
            IconButton(onClick = onAddUser) {
                Icon(AppIcons.Add, "Add Staff")
            }
        }
    ) { user, isSelected ->
        UserListItemCard(
            user,
            isSelected && isExpanded,
            onClick = { onUserSelected(user.id) },
            viewModel.activeProductType,
        )
    }
}

@Composable
private fun UserListItemCard(
    user: UserItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    type: ProductType
) {
    AppCard(
        isSelected = isSelected,
        onClick = onClick,
    ) {
        FlowRow(
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(user.name, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "@" + user.username,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.role.toRoleDisplayName(type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Badge(
                containerColor = if (user.status == UserStatus.ACTIVE)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    user.status.displayName(),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = if (user.status == UserStatus.ACTIVE)
                        Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
