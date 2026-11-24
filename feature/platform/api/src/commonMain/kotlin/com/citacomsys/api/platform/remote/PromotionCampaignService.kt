package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.PromotionCampaign

interface PromotionCampaignService {
    suspend fun get(id: String): PromotionCampaign

    suspend fun getByCode(code: String): PromotionCampaign

    suspend fun findActive(): List<PromotionCampaign>
}