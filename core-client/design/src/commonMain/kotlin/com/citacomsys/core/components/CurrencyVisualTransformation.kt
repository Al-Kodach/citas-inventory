package com.citacomsys.core.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.citacomsys.types.Money

/**
 * Visual transformation for ATM-style currency input.
 * Expects a string of digits representing minor units.
 */
class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val amountMinor = originalText.toLongOrNull() ?: 0L
        val formatted = Money(amountMinor).decimal()

        return TransformedText(
            AnnotatedString(formatted),
            CurrencyOffsetMapping(originalText, formatted)
        )
    }
}

private class CurrencyOffsetMapping(
    private val original: String,
    private val transformed: String
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        // For ATM style, we usually just keep the cursor at the end as content shifts
        return transformed.length
    }

    override fun transformedToOriginal(offset: Int): Int {
        return original.length
    }
}
