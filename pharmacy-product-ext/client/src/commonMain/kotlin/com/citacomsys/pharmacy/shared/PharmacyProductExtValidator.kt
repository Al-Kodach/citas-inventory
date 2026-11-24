package com.citacomsys.pharmacy.shared

import com.citacomsys.module.inventory.validation.BaseProductValidator
import com.citacomsys.module.inventory.validation.ProductValidationResult
import com.citacomsys.pharmacy.api.model.PharmacyProduct

class PharmacyProductExtValidator : BaseProductValidator() {

    /**
     * Polymorphic verification pass: Core Invariants + Clinical Invariants
     */
    fun validatePharmacy(product: PharmacyProduct): ProductValidationResult {
        // RUN SUPER PASS: Check global catalog rules (Name, UOM, SKU, Barcode)
        val baseResult = super.validate(product)

        val clinicalMessages = mutableListOf<String>()
        val clinicalFieldErrors = mutableMapOf<String, String>()

        // RUN DOMAIN PASS: Check explicit pharmacy fields
        if (product.genericName.isNullOrBlank()) {
            clinicalFieldErrors["genericName"] = "Active pharmaceutical generic formula required."
            clinicalMessages.add("Generic id is vital for clinical drug cross-matching.")
        }

        if (product.dosageForm.isNullOrBlank()) {
            clinicalFieldErrors["dosageForm"] = "Dosage form selection missing (e.g., Tablet, Syrup)."
            clinicalMessages.add("Dosage form is mandatory to maintain dispensing context.")
        }

        if (product.strength.isNullOrBlank()) {
            clinicalFieldErrors["strength"] =
                "Item formulation strength missing (e.g., 500mg, 10ml)."
            clinicalMessages.add("Active strength is required to prevent medication errors.")
        }

        product.fdaNumber?.let { fda ->
            if (fda.isNotBlank() && fda.length < 5) {
                clinicalFieldErrors["fdaNumber"] = "Invalid regulatory tracking layout."
                clinicalMessages.add("The provided FDA registration number is invalid.")
            }
        }

        val clinicalResult = if (clinicalFieldErrors.isEmpty()) {
            ProductValidationResult.Success
        } else {
            ProductValidationResult.Failure(clinicalMessages, clinicalFieldErrors)
        }

        // SYNTHESIZE COMBINED OUTCOME
        return when {
            baseResult is ProductValidationResult.Failure && clinicalResult is ProductValidationResult.Failure -> {
                baseResult.merge(clinicalResult) // Combine both error sets seamlessly
            }

            baseResult is ProductValidationResult.Failure -> baseResult
            clinicalResult is ProductValidationResult.Failure -> clinicalResult
            else -> ProductValidationResult.Success
        }
    }
}
