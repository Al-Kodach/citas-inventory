package com.citacomsys.api.platform.repository

import com.citacomsys.api.platform.model.Promotion
import com.citacomsys.api.platform.model.PromotionCampaign

interface PromotionCampaignRepository {

    suspend fun get(id: String): Result<PromotionCampaign>
    suspend fun getPromos(campaignId: String): List<Promotion>
    suspend fun getByCode(code: String): Result<PromotionCampaign>

    suspend fun findActive(): List<PromotionCampaign>
    suspend fun findAll(): List<PromotionCampaign>
    suspend fun save(campaign: PromotionCampaign): Result<Unit>
}