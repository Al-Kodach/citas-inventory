package com.citacomsys.api.billing

import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.types.ResourcePriceBreakdown
import com.citacomsys.types.ProductModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("billingPayload")
sealed class BillingPayload

@Serializable
enum class PayloadType {
    RENEWAL, UPGRADE
}

@Serializable
@SerialName("subscription")
data class SubscriptionBillingPayload(
    val provisionedPackage: ProvisionedPackage,
    @SerialName("sub_type")
    val type: PayloadType
) : BillingPayload()


@Serializable
@SerialName("sms")
data class SmsTopupBillingPayload(
    val packageId: String,
    val purchasedUnits: Int,
    val resourcePriceBreakdown: ResourcePriceBreakdown,
    val effects: List<BillingEffect>
) : BillingPayload() {
    val totalSms: Int
        get() = effects.fold(0) { acc, effect ->
            acc + when (effect) {
                is BillingEffect.AddSms -> effect.units
                else -> 0
            }
        }
}


@Serializable
@SerialName("BillingEffect")
sealed interface BillingEffect {
    @Serializable
    @SerialName("AddSms")
    data class AddSms(
        val units: Int
    ) : BillingEffect

    @Serializable
    @SerialName("EnableModule")
    data class EnableModule(
        val module: ProductModule
    ) : BillingEffect
}