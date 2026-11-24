package com.citacomsys.client.customer.ui.screen.list

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.management.SaaSListScreenScaffold
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.types.EmailAddress
import com.citacomsys.types.PhoneNumber
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onBackClick: (() -> Unit)? = null,
    onNewCustomer: () -> Unit,
    onSelectedCustomer: (String, String) -> Unit,
    viewModel: CustomerListViewModel = koinViewModel()
) {
    val isExpanded = !LocalBackButtonVisibility.current

    SaaSListScreenScaffold(
        viewModel = viewModel,
        title = "Customers",
        onBack = onBackClick,
        searchPlaceholder = "Search by id or phone...",
        onClick = { customer ->
            onSelectedCustomer(customer.id, customer.workspaceId)
        },
        topBarActions = {
            IconButton(onClick = onNewCustomer) {
                Icon(AppIcons.Add, "Add Customer")
            }
        }
    ) { customer, isSelected ->
        CustomerListItem(customer, isSelected && isExpanded)
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerListItem(
    customer: CustomerItem,
    isSelected: Boolean,
) {
    val phone = customer.phone?.let { PhoneNumber(it).mask } ?: "----"
    val email = customer.email?.let { EmailAddress(it).getMaskedEmail() } ?: "----"

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = isSelected,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon and Name Section (Flows Icon on top if not enough space in row)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Contacts Row / Column
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}