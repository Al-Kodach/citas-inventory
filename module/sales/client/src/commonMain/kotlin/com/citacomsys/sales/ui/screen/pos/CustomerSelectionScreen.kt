package com.citacomsys.sales.ui.screen.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.types.EmailAddress
import com.citacomsys.types.PhoneNumber
import com.design.components.ContactsView
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow

@Composable
fun CustomerSelectionScreen(
    selectedCustomer: CustomerItem?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    results: UiResult<List<CustomerItem>>,
    onSelect: (CustomerItem?) -> Unit,
    onNewCustomer: () -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Customer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onNewCustomer,
            ) {
                Icon(
                    AppIcons.Add,
                    contentDescription = "New Customer",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (selectedCustomer == null) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchChange(it)
                    isSearching = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Assign Customer") },
                leadingIcon = {
                    Icon(
                        AppIcons.Search,
                        null
                    )
                },
                singleLine = true,
                supportingText = {}
            )
        }

        if (selectedCustomer == null) {
            Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
                if (isSearching && searchQuery.length >= 2) {
                    UiResultView(results) { list ->
                        Column(Modifier.fillMaxSize()) {
                            if (list.isEmpty()) {
                                Text("No customer found", modifier = Modifier.padding(8.dp))
                            } else {
                                Text("Search Results")
                                list.forEach { customer ->
                                    AppCard(
                                        onClick = { onSelect(customer) }
                                    ) {
                                        FlowRow(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            itemVerticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = customer.fullName,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                            ContactsView(
                                                phone = customer.phone ?: "No phone",
                                                email = customer.email ?: "No email"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            AppCard {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedCustomer.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        ContactsView(
                            phone = selectedCustomer.phone?.let { PhoneNumber.tryCreate(it)?.mask }
                                ?: "00* *** **00",
                            email = selectedCustomer.email?.let { EmailAddress(it).getMaskedEmail() }
                        )
                    }

                    IconButton(onClick = { onSelect(null) }) {
                        Icon(AppIcons.Close, null)
                    }
                }
            }
        }
    }

}
