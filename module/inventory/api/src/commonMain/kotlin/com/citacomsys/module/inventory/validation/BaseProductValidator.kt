package com.citacomsys.module.inventory.validation

import com.citacomsys.types.BaseProduct

open class BaseProductValidator {

    /**
     * Checks core inventory rules. Marked as open so domain subclasses can extend it.
     */
    open fun validate(product: BaseProduct): ProductValidationResult {
        val messages = mutableListOf<String>()
        val fieldErrors = mutableMapOf<String, String>()

        if (product.name.isBlank()) {
            fieldErrors["name"] = "Product ID cannot be blank."
            messages.add("Name is required for catalog registration.")
        }

        if (product.unitOfMeasure.isBlank()) {
            fieldErrors["unitOfMeasure"] = "Unit of measure context is required (e.g., Piece, Box)."
            messages.add("Unit of measure is mandatory to compute stock balances accurately.")
        }

        if (product.reorderLevel < 0) {
            fieldErrors["reorderLevel"] = "Reorder warning threshold cannot be negative."
            messages.add("Reorder safety count must be a non-negative threshold value.")
        }

        product.sku?.let { sku ->
            if (sku.isNotBlank() && sku.length < 3) {
                fieldErrors["sku"] = "SKU code must be at least 3 characters long."
                messages.add("The provided SKU format is structurally invalid.")
            }
        }

        product.barcode?.let { barcode ->
            if (barcode.isNotBlank() && barcode.length < 4) {
                fieldErrors["barcode"] = "Barcode must be at least 4 characters long."
                messages.add("The provided barcode format is structurally invalid.")
            }
        }

        return if (fieldErrors.isEmpty()) {
            ProductValidationResult.Success
        } else {
            ProductValidationResult.Failure(messages, fieldErrors)
        }
    }
}
