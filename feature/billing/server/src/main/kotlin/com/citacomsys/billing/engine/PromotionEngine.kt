package com.citacomsys.billing.engine

import com.citacomsys.api.platform.model.Promotion
import com.citacomsys.api.platform.model.PromotionType
import com.citacomsys.api.billing.BillingEffect
import com.citacomsys.api.billing.BillingEffect.*
import com.citacomsys.types.Money
import com.citacomsys.types.PriceDiscount
import kotlin.time.Instant

data class PromotionResult(
    val discounts: List<PriceDiscount>,
    val effects: List<BillingEffect>,
    val expiresAt: Instant?
)

interface PromotionEngine {

    fun apply(
        promotions: List<Promotion>,
        subtotal: Money = Money.zero()
    ): PromotionResult
}

internal class DefaultPromotionEngine : PromotionEngine {

    override fun apply(
        promotions: List<Promotion>,
        subtotal: Money,
    ): PromotionResult {

        val discounts = mutableListOf<PriceDiscount>()
        val effects = mutableListOf<BillingEffect>()
        var earliestExpiry: Instant? = null

        promotions.forEach { promo ->
            promo.expiresAt?.let { exp ->
                earliestExpiry =
                    when {
                        earliestExpiry == null -> exp
                        exp < earliestExpiry!! -> exp
                        else -> earliestExpiry
                    }
            }

            when (promo.type) {

                PromotionType.PERCENTAGE_DISCOUNT -> {
                    discounts += PriceDiscount(
                        code = promo.code,
                        description = promo.name,
                        amount = subtotal.percentage(promo.value)
                    )
                }

//                PromotionType.FIXED_DISCOUNT -> {
//                    discounts += PriceDiscount(
//                        code = promo.code,
//                        description = promo.ID,
//                        amount = Money(
//                            amountMinor = promo.value.toLong(),
//                            currencyCode = subtotal.currencyCode
//                        )
//                    )
//                }

                PromotionType.FREE_SMS -> {
                    effects += AddSms(
                        promo.value.toInt()
                    )
                }

                PromotionType.FREE_USERS -> {
//                    effects += BillingEffect.AddUsers(
//                        promo.value.toInt()
//                    )
                }

                PromotionType.FREE_DEVICES -> {
//                    effects += BillingEffect.AddDevices(
//                        promo.value.toInt()
//                    )
                }
//
            }
        }

        return PromotionResult(
            discounts = discounts,
            effects = effects,
            expiresAt = earliestExpiry
        )
    }
}