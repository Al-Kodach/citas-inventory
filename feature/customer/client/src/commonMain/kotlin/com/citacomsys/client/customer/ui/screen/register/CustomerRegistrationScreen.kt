package com.citacomsys.client.customer.ui.screen.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.UiResultButton
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.module.api.customer.Customer
import com.design.components.AppScaffold
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerRegistrationScreen(
    customerId: String? = null,
    workspaceId: String? = null,
    goBack: () -> Unit,
    viewModel: CustomerRegistrationViewModel = koinViewModel(),
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val customerState by viewModel.customerState.collectAsStateWithLifecycle()

    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val creditLimitInput by viewModel.creditLimitInput.collectAsStateWithLifecycle()
    val isAlertsConsented by viewModel.isAlertsConsented.collectAsStateWithLifecycle()

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        viewModel.load(customerId, workspaceId)
    }

    LaunchedEffect(saveState) {
        if (saveState.isSuccess) {
            delay(200.milliseconds)
            goBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = if (customerId == null) "New Customer" else "Edit Customer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = goBack) {
                Icon(AppIcons.Close, null)
            }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.info != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.info!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                "Personal Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { viewModel.fullName.value = it },
                label = { Text("Full Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.phone.value = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = creditLimitInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                        viewModel.creditLimitInput.value = input
                    }
                },
                label = { Text("Credit Limit (GHS)") },
                placeholder = { Text("e.g. 500.00 (Leave empty for no credit limit)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAlertsConsented,
                    onCheckedChange = { viewModel.isAlertsConsented.value = it }
                )
                Text(
                    "Customer consents to receive automated alerts",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (customerId != null && customerState is UiResult.Success) {
                val customer = (customerState as UiResult.Success<Customer>).data

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "Financial Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current Balance:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = customer.currentBalance?.formatted ?: "GHS 0.00",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if ((customer.currentBalance?.amountMinor ?: 0) > 0
                        ) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Credit Limit:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = customer.creditLimit?.formatted ?: "No Limit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            saveState.getErrorOrNull()?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            UiResultButton(
                result = saveState,
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = fullName.isNotBlank() && state.enableSaveForm,
                text = if (customerId == null) "Register Customer" else "Save Changes"
            )
        }
    }
}
