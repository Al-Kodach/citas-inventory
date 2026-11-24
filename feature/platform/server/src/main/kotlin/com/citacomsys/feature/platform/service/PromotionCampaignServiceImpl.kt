package com.citacomsys.feature.platform.service

import com.citacomsys.api.platform.model.PromotionCampaign
import com.citacomsys.api.platform.repository.PromotionCampaignRepository
import com.citacomsys.api.platform.remote.PromotionCampaignService

internal class PromotionCampaignServiceImpl(
    private val promotionCampaignRepository: PromotionCampaignRepository
): PromotionCampaignService {
    override suspend fun get(id: String): PromotionCampaign {
        return promotionCampaignRepository.get(id).getOrThrow()
    }

    override suspend fun getByCode(code: String): PromotionCampaign {
        return promotionCampaignRepository.getByCode(code).getOrThrow()
    }

    override suspend fun findActive(): List<PromotionCampaign> {
       return promotionCampaignRepository.findActive()
    }
}