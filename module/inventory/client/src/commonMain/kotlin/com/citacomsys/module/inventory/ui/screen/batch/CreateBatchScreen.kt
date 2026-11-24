package com.citacomsys.module.inventory.ui.screen.batch

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.CurrencyVisualTransformation
import com.citacomsys.core.components.SaveButton
import com.citacomsys.core.data.session.WriteBranchBanner
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.fromEpochMillisecondsToLocalDate
import com.citacomsys.core.util.toInstant
import com.citacomsys.core.domain.security.QuotaStatus
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.result.isError
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.module.inventory.ui.screen.MarginCalculator
import com.citacomsys.types.Money
import com.citacomsys.types.ProductType
import com.design.components.AppScaffold
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateBatchScreen(
    productId: String,
    batchId: String? = null,
    onGoBack: () -> Unit,
    viewModel: CreateBatchViewModel = koinInject(),
    quotaGuard: WorkspaceResourceQuotaGuard = koinInject(),
) {
    val stockQuota = produceState<QuotaStatus?>(null) {
        value = quotaGuard.getBatchQuotaStatus()
    }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val isBatchNumberEditable by viewModel.isBatchNumberEditable.collectAsStateWithLifecycle()

    val batchState by viewModel.batchState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    val batchNumber by viewModel.batchNumber.collectAsStateWithLifecycle()
    val quantity by viewModel.quantity.collectAsStateWithLifecycle()
    val costPrice by viewModel.costPrice.collectAsStateWithLifecycle()
    val sellingPrice by viewModel.sellingPrice.collectAsStateWithLifecycle()
    val expiryDate by viewModel.expiryDate.collectAsStateWithLifecycle()

    val productState by viewModel.productState.collectAsStateWithLifecycle()
    val product = productState.getOrNull()
    val originApp = product?.type ?: ProductType.RETAIL

    var showDatePicker by remember { mutableStateOf(false) }

    val liveMargin = remember(costPrice, sellingPrice) {
        MarginCalculator.calculateMargin(costPrice, sellingPrice)
    }
    val isEditing = batchId != null

    val costAsMoney = remember(costPrice) { Money(costPrice.toLongOrNull() ?: 0L) }
    val sellingAsMoney = remember(sellingPrice) { Money(sellingPrice.toLongOrNull() ?: 0L) }
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()

    LaunchedEffect(batchId) {
        viewModel.loadBatch(batchId)
    }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    AppScaffold(
        title = if (isEditing) "Edit Batch Metadata" else "Restock Inventory",
        onBack = onGoBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Info, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isEditing) "Updating batch metadata. Pricing and quantity are locked to preserve ledger history."
                        else "Adding a new batch. Pricing and initial quantity will be recorded in the movement ledger.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (batchState.isLoading || batchState.isError) {
                UiResultView(batchState)
            }

            // CEO Mode Awareness and Targeting Banner
            WriteBranchBanner(
                label = "CEO Mode: All Branches"
            ) { selectedBranchId ->
                viewModel.setTargetBranch(selectedBranchId)
            }

            // Row 1: Batch Number + Quantity + Expiry Date
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { viewModel.batchNumber.value = it },
                    label = { Text("Batch Number*") },
                    modifier = Modifier.weight(1f),
                    enabled = isBatchNumberEditable,
                    isError = validationErrors.containsKey("batchNumber"),
                    singleLine = true,
                    supportingText = {
                        Column {
                            if (!isBatchNumberEditable) {
                                Text(
                                    text = "Locked",
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (isEditing) {
                                Text("Typo Correction Mode")
                            }

                            validationErrors["batchNumber"]?.let { errorText ->
                                Text(text = errorText, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.all { d -> d.isDigit() }) {
                            viewModel.quantity.value = it
                        }
                    },
                    label = { Text("Quantity") },
                    enabled = !isEditing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f),
                    supportingText = {
                        Column {
                            if (isEditing) Text("Use 'Adjust'")

                            validationErrors["quantity"]?.let { errorText ->
                                Text(text = errorText, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    singleLine = true,
                )

                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = expiryDate?.toString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expiry Date") },
                        placeholder = { Text("Perpetual / No Expiry") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(AppIcons.CalendarToday, null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        interactionSource = remember {
                            MutableInteractionSource()
                        }
                            .also { source ->
                                LaunchedEffect(source) {
                                    source.interactions.collect {
                                        if (it is PressInteraction.Release) {
                                            showDatePicker = true
                                        }
                                    }
                                }
                            }
                    )
                }
            }

            // Row 2: Cost Price + Sale Price + Gross Margin
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = {
                        val digits = it.filter { c -> c.isDigit() }
                        val cleaned = digits.trimStart('0').ifEmpty { "0" }
                        if (cleaned.length <= 12) {
                            viewModel.costPrice.value = cleaned
                        }
                    },
                    label = { Text("Unit Cost Price") },
                    prefix = { Text("GHS ") },
                    enabled = !isEditing,
                    visualTransformation = CurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f),
                    isError = validationErrors.containsKey("costPrice"),
                    supportingText = {
                        Column {
                            if (isEditing) Text("Locked")

                            validationErrors["costPrice"]?.let { errorText ->
                                Text(text = errorText, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = {
                        val digits = it.filter { c -> c.isDigit() }
                        val cleaned = digits.trimStart('0').ifEmpty { "0" }
                        if (cleaned.length <= 12) {
                            viewModel.sellingPrice.value = cleaned
                        }
                    },
                    label = { Text("Unit Sale Price") },
                    prefix = { Text("GHS ") },
                    enabled = !isEditing,
                    visualTransformation = CurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = validationErrors.containsKey("sellingPrice"),
                    supportingText = {
                        Column {
                            if (isEditing) Text("Locked")
                            validationErrors["sellingPrice"]?.let { errorText ->
                                Text(text = errorText, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }

            // Dynamic Gross Margin Indicator Badge
            if (sellingPrice.isNotBlank() && costPrice.isNotBlank()) {
                val isNegative = liveMargin.contains("Negative")
                Surface(
                    color = if (isNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Estimated Margin: $liveMargin",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (showDatePicker) {
                ExpiryDatePickerDialog(
                    onDateSelected = { viewModel.expiryDate.value = it },
                    initialDate = expiryDate,
                    onDismiss = { showDatePicker = false },
                    originApp = originApp
                )
            }

            stockQuota.value?.let { status ->
                Text(status.resourceLabel)
                LinearProgressIndicator(
                    progress = { status.percentage },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (status.percentage > 0.9f) Color.Red else MaterialTheme.colorScheme.primary
                )
                Text(
                    status.displayLabel,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) // "850 / 1000 Active Batch"
            }

            saveState.getErrorOrNull()?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cancel")
                }

                SaveButton(
                    onClick = { showConfirmationDialog = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    isLoading = saveState.isLoading,
                    enabled = batchNumber.isNotBlank()
                            && (isEditing || (quantity.isNotBlank() && quantity.toIntOrNull() != null))
                            && saveState.isLoading.not()
                )
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = if (isEditing) "Confirm Typo Correction" else "Confirm Stock Restock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Please verify that the details below match your physical container labels perfectly:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    //  Data Breakdown Matrix
                    ReviewRow(label = "Batch Code:", value = batchNumber, isHighlighted = true)
                    ReviewRow(label = "Quantity Added:", value = "$quantity Units")
                    //  Cross-Platform Money Class applied to UI fields
                    ReviewRow(label = "Cost per Unit:", value = costAsMoney.formatted)
                    ReviewRow(label = "Selling Price:", value = sellingAsMoney.formatted)

                    // Added Profit Margin Audit Line
                    ReviewRow(
                        label = "Profit Margin:",
                        value = liveMargin,
                        isHighlighted = !liveMargin.contains("Negative"),
                        isWarning = liveMargin.contains("Negative")
                    )

                    ReviewRow(
                        label = "Expiry Date:",
                        value = expiryDate?.toString() ?: "NO EXPIRY SET",
                        isWarning = expiryDate == null
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        // Execute the decoupled save function securely
                        viewModel.save(
                            productId = productId,
                            existingBatchId = batchId,
                            originApp = originApp,
                            onSuccess = onGoBack
                        )
                    },
                    enabled = !liveMargin.contains("Negative"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm & Commit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDatePickerDialog(
    originApp: ProductType,
    initialDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTime = Clock.System.now()

    // Dynamically shift the calendar selection window based on product type
    val minDaysRequired = when (originApp) {
        ProductType.PHARMACY -> 30
        else -> 0
    }

    val minAllowedDate = currentTime.plus(minDaysRequired.toDuration(DurationUnit.DAYS))

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toInstant()?.toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Ensure users cannot select locked dates in the calendar picker interface
                return utcTimeMillis >= minAllowedDate.toEpochMilliseconds()
            }
        }
    )

    val selectedDate = datePickerState.selectedDateMillis?.fromEpochMillisecondsToLocalDate()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onDateSelected(selectedDate)
                onDismiss()
            }) { Text("Confirm") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Only notify Perpetual option if the industry flavor allows it
                if (originApp != ProductType.PHARMACY) {
                    TextButton(onClick = {
                        onDateSelected(null)
                        onDismiss()
                    }) {
                        Text("Clear / Perpetual", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun ReviewRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted || isWarning) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isWarning -> MaterialTheme.colorScheme.error
                isHighlighted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
