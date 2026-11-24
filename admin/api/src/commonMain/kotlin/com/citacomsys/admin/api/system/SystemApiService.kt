package com.citacomsys.admin.api.system

import com.citacomsys.api.platform.model.*
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.UserCapability

interface SystemApiService {
    // Package Management
    suspend fun fetchPackages(query: PageQuery): PageResult<ProductPackage>
    suspend fun getPackageDetails(id: String): ProductPackageDetails
    suspend fun savePackage(details: ProductPackageDetails): Result<Unit>
    
    // SMS Package Management
    suspend fun fetchSmsPackages(): List<SmsPackage>
    suspend fun saveSmsPackage(pkg: SmsPackage): Result<Unit>

    // Campaign Management
    suspend fun fetchCampaigns(): List<PromotionCampaign>
    suspend fun saveCampaign(campaign: PromotionCampaign): Result<Unit>

    // Platform Content Management
    suspend fun getPlatformContent(): PlatformContent
    suspend fun saveFeature(feature: PlatformFeature): Result<Unit>
    suspend fun saveDemo(demo: PlatformDemo): Result<Unit>
    suspend fun saveTestimonial(testimonial: PlatformTestimonial): Result<Unit>

    // System Configuration
    suspend fun getSettings(): SystemSettings
    suspend fun updateSettings(settings: SystemSettings): Result<Unit>

    // Role & Capability Management
    suspend fun fetchGlobalRoleCapabilities(): Map<String, Set<UserCapability>>
    suspend fun saveGlobalRoleCapabilities(role: String, capabilities: Set<UserCapability>): Result<Unit>
    
    suspend fun fetchRoleInheritance(): List<com.citacomsys.inventory.api.user.RoleInheritanceDto>
    suspend fun saveRoleInheritance(role: String, parentRole: String): Result<Unit>
    suspend fun deleteRoleInheritance(role: String, parentRole: String): Result<Unit>
}
