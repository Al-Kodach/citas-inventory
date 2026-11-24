package com.citacomsys.pharmacy.shared.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.ui.InfoTag
import com.citacomsys.core.ui.ProductExtensionViewProvider
import com.citacomsys.pharmacy.api.model.PharmacyProduct
import com.citacomsys.pharmacy.api.model.PharmacyProductExt
import com.citacomsys.pharmacy.shared.presentation.screen.product.form.PharmacyProductFormExt
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

private data class ClinicalInfo(
    val genericName: String?,
    val dosageForm: String?,
    val strength: String?,
    val therapeuticClass: String?,
    val fdaNumber: String?,
    val requiresPrescription: Boolean
)

class PharmacyProductRenderer : ProductExtensionViewProvider {

    override val type: ProductType = ProductType.PHARMACY

    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Composable
    override fun RenderExtensionDetails(product: BaseProduct) {
        val info = remember(product) {
            if (product is PharmacyProduct) {
                ClinicalInfo(
                    genericName = product.genericName,
                    dosageForm = product.dosageForm,
                    strength = product.strength,
                    therapeuticClass = product.therapeuticClass,
                    fdaNumber = product.fdaNumber,
                    requiresPrescription = product.requiresPrescription
                )
            } else {
                val ext = runCatching {
                    json.decodeFromString(
                        PharmacyProductExt.serializer(),
                        product.description ?: "{}"
                    )
                }.getOrNull()

                ClinicalInfo(
                    genericName = ext?.genericName,
                    dosageForm = ext?.dosageForm,
                    strength = ext?.strength,
                    therapeuticClass = ext?.therapeuticClass,
                    fdaNumber = ext?.fdaNumber,
                    requiresPrescription = ext?.requiresPrescription ?: false
                )
            }
        }

        val hasAnyInfo = !info.genericName.isNullOrBlank() ||
                !info.dosageForm.isNullOrBlank() ||
                !info.strength.isNullOrBlank() ||
                !info.therapeuticClass.isNullOrBlank() ||
                !info.fdaNumber.isNullOrBlank() ||
                info.requiresPrescription

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Clinical & Regulatory Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

//        if (!hasAnyInfo) return


            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoTag(label = "Generic Name", value = info.genericName)
                InfoTag(label = "Dosage Form", value = info.dosageForm)
                InfoTag(label = "Strength", value = info.strength)

                InfoTag(
                    label = "Therapeutic Class",
                    value = info.therapeuticClass,
                    color = MaterialTheme.colorScheme.tertiary
                )

                InfoTag(
                    label = "FDA Number",
                    value = info.fdaNumber,
                    color = MaterialTheme.colorScheme.outline
                )

                if (info.requiresPrescription) {
                    InfoTag(
                        label = "Dispensing",
                        value = "PRESCRIPTION REQUIRED",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    InfoTag(
                        label = "Dispensing",
                        value = "OTC / GENERAL",
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }

    @Composable
    override fun RenderExtensionForm(viewModel: Any) {
        Napier.d { "Rendering extension form: viewmodel = $viewModel" }

        val vm = viewModel as? PharmacyProductFormExt ?: return

        val genericName by vm.genericName.collectAsStateWithLifecycle()
        val dosageForm by vm.dosageForm.collectAsStateWithLifecycle()
        val strength by vm.strength.collectAsStateWithLifecycle()
        val requiresPrescription by vm.requiresPrescription.collectAsStateWithLifecycle()
        val therapeuticClass by vm.therapeuticClass.collectAsStateWithLifecycle()
        val fdaNumber by vm.fdaNumber.collectAsStateWithLifecycle()

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Pharmacy Specific Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = genericName,
                onValueChange = { vm.genericName.value = it },
                label = { Text("Generic Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = dosageForm,
                    onValueChange = { vm.dosageForm.value = it },
                    label = { Text("Dosage Form") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = strength,
                    onValueChange = { vm.strength.value = it },
                    label = { Text("Strength") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = therapeuticClass,
                    onValueChange = { vm.therapeuticClass.value = it },
                    label = { Text("Therapeutic Class") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fdaNumber,
                    onValueChange = { vm.fdaNumber.value = it },
                    label = { Text("FDA Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = requiresPrescription,
                    onCheckedChange = { vm.requiresPrescription.value = it }
                )
                Spacer(Modifier.width(8.dp))
                Text("Requires Prescription", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
