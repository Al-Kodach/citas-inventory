package com.citacomsys.inventory.admin.system

import com.citacomsys.api.platform.model.*
import com.citacomsys.api.platform.repository.ProductPackageRepository
import com.citacomsys.api.platform.repository.PromotionCampaignRepository
import com.citacomsys.api.platform.repository.SmsPackageRepository
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.inventory.api.user.RoleRepository
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.table.MarketingContentTable
import com.citacomsys.core.persistence.table.packages.ProductPackages
import com.citacomsys.core.persistence.table.system.SystemSettingsTable
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.vault.SecretVault
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.inventory.api.user.RoleInheritanceDto
import com.citacomsys.types.EntityType
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.UserCapability
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID
import kotlin.time.Clock

internal class AdminSystemService(
    private val packageRepo: ProductPackageRepository,
    private val smsPackageRepo: SmsPackageRepository,
    private val campaignRepo: PromotionCampaignRepository,
    private val roleRepository: RoleRepository,
    private val vault: SecretVault,
    private val auditor: TelemetryAuditor
) {

    suspend fun fetchPackages(query: PageQuery): PageResult<ProductPackage> {
        return ProductPackages.queryPage(
            pageQuery = query,
            searchColumns = listOf(ProductPackages.name, ProductPackages.id),
            defaultSort = ProductPackages.createdAt,
            mapper = { row ->
                ProductPackage(
                    id = row[ProductPackages.id],
                    name = row[ProductPackages.name],
                    tier = row[ProductPackages.packageTier],
                    smsQuotaPerCycle = row[ProductPackages.smsQuotaPerCycle],
                    description = row[ProductPackages.description],
                    recommended = row[ProductPackages.recommended],
                    isPublic = row[ProductPackages.isPublic],
                    createdAt = row[ProductPackages.createdAt],
                    updatedAt = row[ProductPackages.updatedAt]
                )
            }
        )
    }

    suspend fun getPackageDetails(id: String): ProductPackageDetails {
        return packageRepo.getPackageDetails(id).getOrThrow()
    }

    suspend fun savePackage(details: ProductPackageDetails): Result<Unit> = runCatching {
        dbQuery {
            packageRepo.save(details)

            auditor.logApplicationEvent(
                action = "PACKAGE_UPDATED",
                entityType = EntityType.PACKAGE.name,
                entityId = details.packageInfo.id,
                summary = "Admin updated product package: ${details.packageInfo.name}",
                level = TelemetryLevel.INFO
            )
        }
    }

    suspend fun getSettings(): SystemSettings = dbQuery {
        SystemSettingsTable.selectAll()
            .where { SystemSettingsTable.id eq "GLOBAL_SYSTEM" }
            .singleOrNull()?.let { row ->
                SystemSettings(
                    momoCollectionNumber = vault.decrypt(row[SystemSettingsTable.momoCollectionNumberEncrypted]),
                    momoCollectionName = row[SystemSettingsTable.momoCollectionName],
                    supportPhoneNumber = vault.decrypt(row[SystemSettingsTable.supportPhoneNumberEncrypted]),
                    maintenanceWindowEnabled = row[SystemSettingsTable.maintenanceWindowEnabled]
                )
            } ?: SystemSettings(
                momoCollectionNumber = "+233000000000",
                momoCollectionName = "Citacom Systems LTD",
                supportPhoneNumber = "+233000000000",
                maintenanceWindowEnabled = false
            )
    }

    suspend fun updateSettings(settings: SystemSettings): Result<Unit> = runCatching {
        dbQuery {
            SystemSettingsTable.upsert {
                it[id] = "GLOBAL_SYSTEM"
                it[momoCollectionNumberEncrypted] = vault.encrypt(settings.momoCollectionNumber)
                it[momoCollectionName] = settings.momoCollectionName
                it[supportPhoneNumberEncrypted] = vault.encrypt(settings.supportPhoneNumber)
                it[maintenanceWindowEnabled] = settings.maintenanceWindowEnabled
                it[updatedAt] = Clock.System.now()
            }

            auditor.logApplicationEvent(
                action = "SYSTEM_SETTINGS_UPDATED",
                entityType = EntityType.AUDIT.name,
                entityId = "SYSTEM",
                summary = "Admin updated global system settings",
                level = TelemetryLevel.WARNING
            )
        }
    }

    suspend fun fetchSmsPackages(): List<SmsPackage> {
        return smsPackageRepo.getAll()
    }

    suspend fun saveSmsPackage(pkg: SmsPackage): Result<Unit> = runCatching {
        dbQuery {
            smsPackageRepo.save(pkg)

            auditor.logApplicationEvent(
                action = "SMS_PACKAGE_SAVED",
                entityType = EntityType.PACKAGE.name,
                entityId = pkg.id,
                summary = "Admin saved SMS package: ${pkg.units} units",
                level = TelemetryLevel.INFO
            )
        }
    }

    suspend fun fetchCampaigns(): List<PromotionCampaign> {
        return campaignRepo.findAll()
    }

    suspend fun saveCampaign(campaign: PromotionCampaign): Result<Unit> = runCatching {
        dbQuery {
            campaignRepo.save(campaign)

            auditor.logApplicationEvent(
                action = "CAMPAIGN_SAVED",
                entityType = EntityType.PACKAGE.name,
                entityId = campaign.id,
                summary = "Admin saved promotion campaign: ${campaign.name}",
                level = TelemetryLevel.INFO
            )
        }
    }

    suspend fun getPlatformContent(): PlatformContent = dbQuery {
        val features = MarketingContentTable
            .selectAll()
            .where { MarketingContentTable.contentType eq "FEATURE" }
            .orderBy(MarketingContentTable.orderRank, SortOrder.ASC)
            .map { row ->
                PlatformFeature(
                    id = row[MarketingContentTable.id],
                    title = row[MarketingContentTable.title],
                    featureIcon = "INVENTORY",
                    module = null,
                    description = row[MarketingContentTable.description] ?: "",
                    orderRank = row[MarketingContentTable.orderRank],
                    isActive = row[MarketingContentTable.isActive]
                )
            }

        val demos = MarketingContentTable
            .selectAll()
            .where { MarketingContentTable.contentType eq "DEMO" }
            .orderBy(MarketingContentTable.orderRank, SortOrder.ASC)
            .map { row ->
                PlatformDemo(
                    id = row[MarketingContentTable.id],
                    title = row[MarketingContentTable.title],
                    videoUrl = row[MarketingContentTable.mediaUrl],
                    imageUrl = row[MarketingContentTable.mediaUrl],
                    orderRank = row[MarketingContentTable.orderRank],
                    isActive = row[MarketingContentTable.isActive]
                )
            }

        val testimonials = MarketingContentTable
            .selectAll()
            .where { MarketingContentTable.contentType eq "TESTIMONIAL" }
            .orderBy(MarketingContentTable.orderRank, SortOrder.ASC)
            .map { row ->
                PlatformTestimonial(
                    id = row[MarketingContentTable.id],
                    name = row[MarketingContentTable.authorName] ?: row[MarketingContentTable.title],
                    designation = row[MarketingContentTable.authorTitle] ?: "",
                    message = row[MarketingContentTable.description] ?: "",
                    imageUrl = row[MarketingContentTable.mediaUrl],
                    orderRank = row[MarketingContentTable.orderRank],
                    isActive = row[MarketingContentTable.isActive]
                )
            }

        PlatformContent(features, demos, testimonials)
    }

    suspend fun saveFeature(feature: PlatformFeature): Result<Unit> = runCatching {
        dbQuery {
            MarketingContentTable.upsert {
                it[id] = feature.id.takeIf { id -> id.isNotBlank() } ?: UUID.randomUUID().toString()
                it[contentType] = "FEATURE"
                it[title] = feature.title
                it[description] = feature.description
                it[orderRank] = feature.orderRank
                it[isActive] = feature.isActive
            }
            auditor.logApplicationEvent(
                "FEATURE_SAVED",
                EntityType.AUDIT.name,
                feature.id,
                "Saved feature highlight: ${feature.title}"
            )
        }
    }

    suspend fun saveDemo(demo: PlatformDemo): Result<Unit> = runCatching {
        dbQuery {
            MarketingContentTable.upsert {
                it[id] = demo.id.takeIf { id -> id.isNotBlank() } ?: UUID.randomUUID().toString()
                it[contentType] = "DEMO"
                it[title] = demo.title
                it[mediaUrl] = demo.videoUrl ?: demo.imageUrl
                it[orderRank] = demo.orderRank
                it[isActive] = demo.isActive
            }
            auditor.logApplicationEvent(
                "DEMO_SAVED",
                EntityType.AUDIT.name,
                demo.id,
                "Saved platform demo: ${demo.title}"
            )
        }
    }

    suspend fun saveTestimonial(testimonial: PlatformTestimonial): Result<Unit> = runCatching {
        dbQuery {
            MarketingContentTable.upsert {
                it[id] = testimonial.id.takeIf { id -> id.isNotBlank() } ?: UUID.randomUUID().toString()
                it[contentType] = "TESTIMONIAL"
                it[title] = testimonial.name
                it[authorName] = testimonial.name
                it[authorTitle] = testimonial.designation
                it[description] = testimonial.message
                it[mediaUrl] = testimonial.imageUrl
                it[orderRank] = testimonial.orderRank
                it[isActive] = testimonial.isActive
            }

            auditor.logApplicationEvent(
                "TESTIMONIAL_SAVED",
                EntityType.AUDIT.name,
                testimonial.id,
                "Saved testimonial from: ${testimonial.name}"
            )
        }
    }

    suspend fun fetchGlobalRoleCapabilities(): Map<String, Set<UserCapability>> {
        return roleRepository.findAllCapabilities(SystemActors.SYSTEM_ID)
    }

    suspend fun saveGlobalRoleCapabilities(role: String, capabilities: Set<UserCapability>): Result<Unit> = runCatching {
        roleRepository.save(SystemActors.SYSTEM_ID, role, capabilities)
        Unit
    }

    suspend fun fetchRoleInheritance(): List<RoleInheritanceDto> {
        return roleRepository.findInheritance(SystemActors.SYSTEM_ID)
    }

    suspend fun saveRoleInheritance(role: String, parentRole: String): Result<Unit> {
        return roleRepository.saveInheritance(SystemActors.SYSTEM_ID, role, parentRole)
    }

    suspend fun deleteRoleInheritance(role: String, parentRole: String): Result<Unit> {
        return roleRepository.deleteInheritance(SystemActors.SYSTEM_ID, role, parentRole)
    }
}
