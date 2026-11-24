package com.citacomsys.module.inventory.ui.screen.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.barcode.BarcodePrinterService
import com.citacomsys.core.barcode.BarcodeScannerService
import com.citacomsys.core.components.SaveButton
import com.citacomsys.core.domain.security.QuotaStatus
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.ProductExtensionViewRegistry
import com.citacomsys.core.ui.result.UiResultButton
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.types.BaseProduct
import com.design.components.AppScaffold
import com.design.components.ScrollableRowWithButtons
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    goBack: () -> Unit,
    viewModel: BaseProductFormViewModel<out BaseProduct> = koinInject(),
    quotaGuard: WorkspaceResourceQuotaGuard = koinInject(),
    scannerService: BarcodeScannerService = koinInject(),
    printerService: BarcodePrinterService = koinInject()
) {
    var showScanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val productQuota = produceState<QuotaStatus?>(null) {
        value = quotaGuard.getProductQuotaStatus()
    }

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    // Form fields driven by ViewModel
    val name by viewModel.name.collectAsStateWithLifecycle()
    val sku by viewModel.sku.collectAsStateWithLifecycle()
    val barcode by viewModel.barcode.collectAsStateWithLifecycle()
    val brandName by viewModel.brandName.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val unitOfMeasure by viewModel.unitOfMeasure.collectAsStateWithLifecycle()
    val reorderLevel by viewModel.reorderLevel.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    if (showScanner) {
        scannerService.Scan(
            onResult = { scanned ->
                Napier.d(
                    tag = "ProductFormScreen",
                    message = "Scanned barcode received: '${scanned.rawValue}' (Format: ${scanned.format})"
                )
                viewModel.barcode.value = scanned.rawValue.trim()
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    }

    AppScaffold(
        title = if (productId == null) "New Product" else "Edit Product",
        onBack = goBack,
        actions = {
            // Print Barcode Label Button
            if (barcode.isNotBlank()) {
                IconButton(
                    onClick = {
                        scope.launch {
                            printerService.printBarcodeLabel(
                                title = name,
                                code = barcode,
                                price = null,
                                expiryDate = null
                            )
                        }
                    },
                ) {
                    Icon(AppIcons.Print, "Print Label")
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Hybrid Quick Templates Selection (Single row with 2 primary chips + More dropdown menu)
            if (productId == null && templates.isNotEmpty()) {
                var isMoreTemplatesExpanded by remember { mutableStateOf(false) }
                val primaryTemplates = templates.take(5)
                val remainingTemplates = templates.drop(5)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Quick Templates",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollableRowWithButtons {
                            items(primaryTemplates) { template ->
                                SuggestionChip(
                                    onClick = { viewModel.applyTemplate(template) },
                                    label = { Text(template.name) }
                                )
                            }

                            item {
                                // More Templates Dropdown
                                if (remainingTemplates.isNotEmpty()) {
                                    Box {
                                        AssistChip(
                                            onClick = { isMoreTemplatesExpanded = true },
                                            label = { Text("More (${remainingTemplates.size})") },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Expand templates",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )

                                        DropdownMenu(
                                            expanded = isMoreTemplatesExpanded,
                                            onDismissRequest = { isMoreTemplatesExpanded = false }
                                        ) {
                                            remainingTemplates.forEach { template ->
                                                DropdownMenuItem(
                                                    text = { Text(template.name) },
                                                    onClick = {
                                                        viewModel.applyTemplate(template)
                                                        isMoreTemplatesExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Standard Base Fields
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.name.value = it },
                    label = { Text("Product Name*") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = brandName,
                    onValueChange = { viewModel.brandName.value = it },
                    label = {
                        Text(
                            "Brand / Manufacturer",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { viewModel.category.value = it },
                    label = { Text("Category*") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { viewModel.barcode.value = it },
                    label = { Text("Barcode") },
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, "Scan Barcode")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
            }

            var isUomExpanded by remember { mutableStateOf(false) }
            val standardUoms = remember {
                listOf(
                    "Piece",
                    "Box",
                    "Pack",
                    "Bottle",
                    "Carton",
                    "Tablet",
                    "Strip",
                    "Kilogram (kg)",
                    "Gram (g)",
                    "Litre (L)",
                    "Millilitre (ml)",
                    "Set"
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isUomExpanded,
                    onExpandedChange = { isUomExpanded = !isUomExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = unitOfMeasure,
                        onValueChange = {
                            viewModel.unitOfMeasure.value = it
                            isUomExpanded = true
                        },
                        label = { Text("Unit of Measure*") },
                        placeholder = { Text("e.g. Piece, Box, Bottle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUomExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isUomExpanded,
                        onDismissRequest = { isUomExpanded = false }
                    ) {
                        val filteredUoms = standardUoms.filter {
                            unitOfMeasure.isBlank() || it.contains(unitOfMeasure, ignoreCase = true)
                        }
                        val displayList =
                            if (filteredUoms.isNotEmpty()) filteredUoms else standardUoms

                        displayList.forEach { uom ->
                            DropdownMenuItem(
                                text = { Text(uom) },
                                onClick = {
                                    viewModel.unitOfMeasure.value = uom
                                    isUomExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reorderLevel,
                    onValueChange = { viewModel.reorderLevel.value = it },
                    label = { Text("Reorder Level") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }

            // DYNAMIC INJECTION POINT
            ProductExtensionViewRegistry.ExtensionForm(viewModel.activeProductType, viewModel)

            OutlinedTextField(
                value = sku,
                onValueChange = { viewModel.onSkuInputChanged(it) },
                label = { Text("SKU / Code") },
                placeholder = { Text("Auto-generated as you type") },
                supportingText = {
                    if (viewModel.isUserInput) {
                        Text("Manual SKU entered", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Auto-generated based on product details")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.padding(8.dp))
            productQuota.value?.let { status ->
                LinearProgressIndicator(
                    progress = { status.percentage },
                    color = if (status.percentage > 0.9f) Color.Red else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()
                )
                Text(status.displayLabel) // "850 / 1000 Products"
            }


            Spacer(Modifier.padding(8.dp))

            saveState.getErrorOrNull()?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            SaveButton(
                text = if (productId == null) "Create Product" else "Save Changes",
                onClick = {
                    viewModel.saveProduct {
                        goBack()
                    }
                },
                enabled = saveState.isLoading.not(),
                isLoading = saveState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
