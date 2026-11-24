package com.citacomsys.inventory.admin.system.remote

import com.citacomsys.admin.api.system.*
import com.citacomsys.api.platform.model.*
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import io.ktor.client.*
import io.ktor.client.request.*

internal class KtorSystemApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/admin/system"
) : SystemApiService, BaseApi(client) {

    val pkgEndpoint = "$endpoint/packages"
    val campaignEndpoint = "$endpoint/campaigns"
    val smsEndpoint = "$endpoint/sms"
    val contentEndpoint = "$endpoint/content"

    override suspend fun fetchPackages(query: PageQuery): PageResult<ProductPackage> {
        return executeRequest {
            client.post(pkgEndpoint) {
                setBody(query)
            }
        }
    }

    override suspend fun getPackageDetails(id: String): ProductPackageDetails {
        return executeRequest { client.get("$pkgEndpoint/$id") }
    }

    override suspend fun savePackage(details: ProductPackageDetails): Result<Unit> = runCatching {
        executeRequest {
            client.post("$pkgEndpoint/${details.packageInfo.id}") {
                setBody(details)
            }
        }
    }

    override suspend fun fetchSmsPackages(): List<SmsPackage> {
        return executeRequest { client.get(smsEndpoint) }
    }

    override suspend fun saveSmsPackage(pkg: SmsPackage): Result<Unit> = runCatching {
        executeRequest {
            client.post(smsEndpoint) { setBody(pkg) }
        }
    }

    override suspend fun fetchCampaigns(): List<PromotionCampaign> {
        return executeRequest { client.get(campaignEndpoint) }
    }

    override suspend fun saveCampaign(campaign: PromotionCampaign): Result<Unit> = runCatching {
        executeRequest { client.post(campaignEndpoint) { setBody(campaign) } }
    }

    override suspend fun getPlatformContent(): PlatformContent {
        return executeRequest { client.get(contentEndpoint) }
    }

    override suspend fun saveFeature(feature: PlatformFeature): Result<Unit> = runCatching {
        executeRequest {
            client.post("$contentEndpoint/feature") {
                setBody(feature)
            }
        }
    }

    override suspend fun saveDemo(demo: PlatformDemo): Result<Unit> = runCatching {
        executeRequest {
            client.post("$contentEndpoint/demo") {
                setBody(demo)
            }
        }
    }

    override suspend fun saveTestimonial(testimonial: PlatformTestimonial): Result<Unit> = runCatching {
        executeRequest {
            client.post("$contentEndpoint/testimonial") {
                setBody(testimonial)
            }
        }
    }

    override suspend fun getSettings(): SystemSettings {
        return executeRequest { client.get("$endpoint/settings") }
    }

    override suspend fun updateSettings(settings: SystemSettings): Result<Unit> = runCatching {
        executeRequest { client.post("$endpoint/settings") { setBody(settings) } }
    }

    override suspend fun fetchGlobalRoleCapabilities(): Map<String, Set<com.citacomsys.types.UserCapability>> {
        return executeRequest { client.get("$endpoint/roles/capabilities") }
    }

    override suspend fun saveGlobalRoleCapabilities(
        role: String,
        capabilities: Set<com.citacomsys.types.UserCapability>
    ): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/roles/$role/capabilities") {
                setBody(capabilities)
            }
        }
    }

    override suspend fun fetchRoleInheritance(): List<com.citacomsys.inventory.api.user.RoleInheritanceDto> {
        return executeRequest { client.get("$endpoint/roles/inheritance") }
    }

    override suspend fun saveRoleInheritance(role: String, parentRole: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/roles/inheritance") {
                setBody(mapOf("role" to role, "parentRole" to parentRole))
            }
        }
    }

    override suspend fun deleteRoleInheritance(role: String, parentRole: String): Result<Unit> = runCatching {
        executeRequest {
            client.post("$endpoint/roles/inheritance/delete") {
                setBody(mapOf("role" to role, "parentRole" to parentRole))
            }
        }
    }
}
