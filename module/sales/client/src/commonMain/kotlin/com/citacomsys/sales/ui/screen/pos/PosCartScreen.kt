package com.citacomsys.sales.ui.screen.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.core.components.PrimaryButton
import com.citacomsys.core.components.SecondaryButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.module.api.customer.nav.CustomerRegistrationNav
import com.citacomsys.sales.engine.CartItem
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PosCartScreen(
    viewModel: PosViewModel = koinViewModel(),
    onCheckout: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val customerResults by viewModel.customerSearchResults.collectAsStateWithLifecycle()

    val navigator = LocalNavigator.current
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.items) {
        if (uiState.items.isEmpty() && uiState.completedSaleId == null) {
            navigator.goBack()
        }
    }

    AppScaffold(
        title = "Cart",
        onBack = if (LocalBackButtonVisibility.current) onBack else null,
        actions = {},
        showInfo = showInfo,
        supportTitle = null,
        supportContent = {
            CustomerSelectionScreen(
                selectedCustomer = uiState.selectedCustomer,
                searchQuery = customerSearchQuery,
                onSearchChange = viewModel::onCustomerSearchQueryChange,
                results = customerResults,
                onSelect = {
                    viewModel.selectCustomer(it)
                    if (it != null) {
                        showInfo = false
                    }
                },
                onNewCustomer = {
                    navigator.navigateToSingleTop(CustomerRegistrationNav())
                }
            )
        },
        actionIcon = AppIcons.Person2
    ) { paddingValues ->
        CartContent(
            modifier = Modifier.padding(paddingValues),
            items = uiState.items,
            total = uiState.total.formatted,
            isLoading = uiState.isLoading,
            canWrite = uiState.canWriteSale,
            canSell = uiState.canProcessSales,
            error = uiState.error,
            errDetails = uiState.errorDetails,
            draftName = uiState.activeCartName,
            onNameChange = viewModel::updateDraftName,
            onIncrement = { viewModel.updateQuantity(it.productId, 1) },
            onDecrement = { viewModel.updateQuantity(it.productId, -1) },
            onRemove = { viewModel.removeItem(it.productId) },
            onCheckout = {
                viewModel.checkout()
                onCheckout()
            },
            onClearCart = viewModel::clearCart,
            isCustomerSelected = uiState.selectedCustomer != null,
            onUpgradePlan = {
                navigator.navigateToSingleTop(SubscriptionHubNav)
            },
        )
    }
}

@Composable
private fun CartContent(
    modifier: Modifier = Modifier,
    items: List<CartItem>,
    total: String,
    isLoading: Boolean,
    canWrite: Boolean,
    canSell: Boolean,
    error: String?,
    isCustomerSelected: Boolean,
    errDetails: Map<String, String>,
    draftName: String,
    onNameChange: (String) -> Unit,
    onIncrement: (CartItem) -> Unit,
    onDecrement: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onCheckout: () -> Unit,
    onClearCart: () -> Unit,
    onUpgradePlan: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draftName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Customer Name / Draft Note",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            singleLine = true,
            enabled = !isCustomerSelected,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.3f
                ),
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items.forEach {
                CartItemRow(
                    item = it,
                    onIncrement = { onIncrement(it) },
                    onDecrement = { onDecrement(it) },
                    onRemove = { onRemove(it) }
                )
            }
        }

        Column(Modifier.fillMaxSize()) {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // Pricing Summary Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SummaryRow(label = "Subtotal", value = total)
                SummaryRow(label = "Tax (Placeholder)", value = "GHS 0.00")
                SummaryRow(label = "Other Charges", value = "GHS 0.00")
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    total,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            error?.let {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            if (errDetails.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    errDetails.forEach { (t, m) ->
                        Text(
                            text = "$t $m",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Action Footer
        Column(Modifier.fillMaxSize().padding(bottom = 16.dp)) {
            Spacer(Modifier.weight(1f))
            if (!canWrite) {
                TextButton(
                    onClick = onUpgradePlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Please renew your plan to enable sales",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else if (!canSell) {
                Text(
                    text = "View-Only Mode: Account lacks PROCESS_SALES capability.",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            val checkoutEnabled = items.isNotEmpty() && !isLoading && canWrite && canSell
            val buttonText = when {
                !canSell -> "VIEW ONLY MODE"
                !canWrite -> "PLAN EXPIRED"
                else -> "CHECKOUT"
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                itemVerticalAlignment = Alignment.CenterVertically,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    onClick = onClearCart,
                    text = "Clear Cart",
                    modifier = Modifier.height(56.dp).weight(1f)
                )

                PrimaryButton(
                    onClick = onCheckout,
                    modifier = Modifier.height(56.dp).weight(1f),
//                    shape = RoundedCornerShape(12.dp),
                    isEnabled = checkoutEnabled,
                    text = buttonText
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${item.unitPrice.amount} x ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                item.totalPrice.amount.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRemove) {
                Icon(
                    AppIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDecrement) {
                Icon(AppIcons.Remove, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = onIncrement) {
                Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}