package com.citacomsys.api.platform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

enum class PromotionType {
    PERCENTAGE_DISCOUNT,
    FREE_SMS,

    FREE_USERS,
    FREE_DEVICES
}


@Serializable
@SerialName("Promotion")
data class Promotion(
    val code: String,
    val name: String,
    val type: PromotionType,
    /**
     * Meaning depends on type:
     *
     * PERCENTAGE_DISCOUNT -> %
     * FIXED_DISCOUNT      -> money minor units
     * FREE_SMS            -> sms units
     * FREE_USERS          -> user count
     * FREE_DEVICES        -> device count
     */
    val value: Double,

    val expiresAt: Instant? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
)

@Serializable
@SerialName("PromotionCampaign")
data class PromotionCampaign(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val active: Boolean,
    val startsAt: Instant,
    val expiresAt: Instant?,
    val promotions: List<Promotion>
)

@Serializable
data class AppliedPromotion(
    val campaignId: String,
    val promotionCode: String,
    val expiresAt: Instant?
)