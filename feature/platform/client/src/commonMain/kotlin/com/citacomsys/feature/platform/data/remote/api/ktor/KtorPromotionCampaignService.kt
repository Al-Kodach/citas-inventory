package com.citacomsys.feature.platform.data.remote.api.ktor

import com.citacomsys.api.platform.model.PromotionCampaign
import com.citacomsys.api.platform.remote.PromotionCampaignService
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get

internal class KtorPromotionCampaignService(
    client: HttpClient,
    private val endpoint: String = "api/v1/campaigns"
): PromotionCampaignService, BaseApi(client) {

    override suspend fun get(id: String): PromotionCampaign {
       return executeRequest { client.get("$endpoint/$id") }
    }

    override suspend fun getByCode(code: String): PromotionCampaign {
       return executeRequest { client.get("$endpoint/code/$code") }
    }

    override suspend fun findActive(): List<PromotionCampaign> {
        return executeRequest { client.get(endpoint) }
    }
}