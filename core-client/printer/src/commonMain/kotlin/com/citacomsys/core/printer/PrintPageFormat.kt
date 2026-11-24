package com.citacomsys.core.printer

import kotlinx.serialization.Serializable

@Serializable
sealed class PrintPageFormat {
    abstract val name: String
    abstract val widthMm: Float
    abstract val heightMm: Float
    abstract val isContinuousRoll: Boolean

    /** Standard A4 Sheet (210mm x 297mm) - Financial Reports, Inventory Audits, Business Invoices */
    @Serializable
    data object A4 : PrintPageFormat() {
        override val name: String = "A4"
        override val widthMm: Float = 210f
        override val heightMm: Float = 297f
        override val isContinuousRoll: Boolean = false
    }

    /** US Letter Sheet (215.9mm x 279.4mm) - US Standard Reports */
    @Serializable
    data object Letter : PrintPageFormat() {
        override val name: String = "Letter"
        override val widthMm: Float = 215.9f
        override val heightMm: Float = 279.4f
        override val isContinuousRoll: Boolean = false
    }

    /** 80mm Thermal Paper Roll (80mm width) - Standard POS Checkout Sales Receipts */
    @Serializable
    data class Receipt80mm(override val heightMm: Float = 0f) : PrintPageFormat() {
        override val name: String = "Receipt 80mm"
        override val widthMm: Float = 80f
        override val isContinuousRoll: Boolean = true
    }

    /** 58mm Thermal Paper Roll (58mm width) - Mobile POS Handheld Thermal Printers */
    @Serializable
    data class Receipt58mm(override val heightMm: Float = 0f) : PrintPageFormat() {
        override val name: String = "Receipt 58mm"
        override val widthMm: Float = 58f
        override val isContinuousRoll: Boolean = true
    }

    /** Custom Barcode Label Sticker (e.g., 50mm x 25mm, 40mm x 30mm) - Price Tags & Shelf Barcodes */
    @Serializable
    data class CustomLabel(
        override val widthMm: Float = 50f,
        override val heightMm: Float = 25f
    ) : PrintPageFormat() {
        override val name: String = "Custom Label ${widthMm.toInt()}x${heightMm.toInt()}mm"
        override val isContinuousRoll: Boolean = false
    }
}
