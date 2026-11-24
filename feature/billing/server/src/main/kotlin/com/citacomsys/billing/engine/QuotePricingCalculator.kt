package com.citacomsys.billing.engine

import com.citacomsys.api.platform.model.Promotion
import com.citacomsys.api.billing.BillingQuote
import com.citacomsys.types.Money
import com.citacomsys.types.PriceTax

internal class QuotePricingCalculator(
    private val promotionEngine: PromotionEngine
) {

    fun apply(
        promotions: List<Promotion>,
        quote: BillingQuote
    ): BillingQuote {

        val promoResult = promotionEngine.apply(promotions, quote.subtotal)

        val discountTotal =
            promoResult.discounts.fold(Money.zero()) { acc, item ->
                acc + item.amount
            }

        val taxable = quote.subtotal + discountTotal

        val vat = taxable.percentage(15.0)

        return quote.copy(
            discounts = promoResult.discounts,
            taxes = listOf(
                PriceTax(
                    code = "VAT",
                    name = "VAT",
                    ratePercent = 15.0,
                    amount = vat
                )
            ),
            total = taxable + vat,
            promotionExpiresAt = promoResult.expiresAt
        )
    }
}