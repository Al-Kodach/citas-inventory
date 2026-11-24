package com.citacomsys.feature.platform.data.remote.api.ktor

import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.remote.ProductPackageService
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.get

internal class KtorProductPackageService(
    client: HttpClient,
    private val endpoint: String = "api/v1/packages"
) : ProductPackageService, BaseApi(client) {
    override suspend fun getPublicPackages(): List<ProductPackageDetails> {
        return executeRequest { client.get(endpoint) }
    }

    override suspend fun getAvailableUpgrades(workspaceId: String): List<ProductPackageDetails> {
        return executeRequest { client.get("$endpoint/upgrade/$workspaceId") }
    }

    override suspend fun getBillingTerms(workspaceId: String): List<PackageBillingTerm> {
        return executeRequest { client.get("$endpoint/billing/$workspaceId") }
    }

    override suspend fun getPromotionalPackages(workspaceId: String): List<ProductPackageDetails> {
        return executeRequest { client.get("$endpoint/promo/$workspaceId") }
    }
}