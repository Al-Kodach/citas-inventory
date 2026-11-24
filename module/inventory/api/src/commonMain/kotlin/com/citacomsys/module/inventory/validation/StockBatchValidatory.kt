package com.citacomsys.module.inventory.validation

import com.citacomsys.types.ProductType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

object StockBatchValidator {

    fun validateRestock(
        originApp: ProductType,
        batchNumber: String,
        quantity: Int,
        costPriceMinor: Long,
        sellingPriceMinor: Long,
        expiryDate: LocalDate?,
        currentTime: Instant = Clock.System.now()
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // 1. DYNAMIC EXPIRY REGULATORY SAFETY GATES
        if (expiryDate != null) {
            val timeZone = TimeZone.currentSystemDefault()
            val expiryInstant = expiryDate.atStartOfDayIn(timeZone)

            // Determine the minimum required shelf life based on the industry module
            val minDaysRequired = when (originApp) {
                ProductType.PHARMACY -> 30  // Medical regulation floor
                else -> 0        // Retail/Hardware/Auto can accept close-dated items
            }

            val minAllowedDate = currentTime.plus(minDaysRequired.toDuration(DurationUnit.DAYS))

            if (expiryInstant < currentTime) {
                errors["expiryDate"] = "Product is already expired."
            } else if (expiryInstant < minAllowedDate) {
                errors["expiryDate"] =
                    "Expiry date must be at least $minDaysRequired days in the future for $originApp stock."
            }
        } else {
            // Strict enforcement: Medical stocks MUST have an expiration tracking record
            if (originApp == ProductType.PHARMACY) {
                errors["expiryDate"] =
                    "Pharma products require an explicit expiry date tracking index."
            }
        }

        // 2. PRICING INTEGRITY CHANNELS
        if (costPriceMinor <= 0L) errors["costPrice"] = "Cost price must be greater than zero."
        if (sellingPriceMinor <= 0L) errors["sellingPrice"] =
            "Selling price must be greater than zero."
        if (sellingPriceMinor < costPriceMinor) {
            errors["sellingPrice"] =
                "Selling price cannot be lower than cost price (Negative Margin)."
        }

        // 3. QUANTITY INTEGRITY CHANNELS
        if (quantity <= 0) errors["quantity"] = "Restock quantity must be greater than zero."
        if (batchNumber.isBlank()) errors["batchNumber"] = "Batch reference code is required."

        return errors
    }
}
