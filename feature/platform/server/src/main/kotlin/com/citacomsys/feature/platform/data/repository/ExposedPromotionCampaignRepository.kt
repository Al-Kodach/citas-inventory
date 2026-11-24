package com.citacomsys.feature.platform.data.repository

import com.citacomsys.api.platform.model.Promotion
import com.citacomsys.api.platform.model.PromotionCampaign
import com.citacomsys.api.platform.model.PromotionType
import com.citacomsys.api.platform.repository.PromotionCampaignRepository
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.packages.PromotionCampaigns
import com.citacomsys.core.persistence.table.packages.Promotions
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

internal class ExposedPromotionCampaignRepository : PromotionCampaignRepository {

    private fun ResultRow.toPromotionCampaign(
        promotions: List<Promotion>
    ) = PromotionCampaign(
        id = this[PromotionCampaigns.id].value.toString(),
        code = this[PromotionCampaigns.code],
        name = this[PromotionCampaigns.name],
        description = this[PromotionCampaigns.description],
        active = this[PromotionCampaigns.active],
        startsAt = this[PromotionCampaigns.startsAt],
        expiresAt = this[PromotionCampaigns.expiresAt],
        promotions = promotions
    )

    private fun getPromotionsForCampaign(campaignUuid: UUID): List<Promotion> {
        return Promotions.selectAll().where {
            Promotions.campaignId eq campaignUuid
        }.map {
            Promotion(
                code = it[Promotions.code],
                name = it[Promotions.name],
                type = PromotionType.valueOf(it[Promotions.type]),
                value = it[Promotions.value],
                expiresAt = it[Promotions.expiresAt],
                createdAt = it[Promotions.createdAt],
                updatedAt = it[Promotions.updatedAt]
            )
        }
    }

    override suspend fun get(id: String): Result<PromotionCampaign> = runCatching {
        dbQuery {
            val uuid = id.toUuid()
            val promotions = getPromotionsForCampaign(uuid)

            PromotionCampaigns.selectAll().where {
                PromotionCampaigns.id eq uuid
            }.singleOrNull()
                ?.toPromotionCampaign(promotions) ?: error("Campaign not found")
        }
    }

    override suspend fun getPromos(campaignId: String): List<Promotion> = dbQuery {
         getPromotionsForCampaign(campaignId.toUuid())
    }

    override suspend fun getByCode(code: String): Result<PromotionCampaign> = runCatching {
        dbQuery {
            val row = PromotionCampaigns.selectAll().where {
                PromotionCampaigns.code eq code
            }.singleOrNull() ?: error("Campaign not found")

            val promotions = getPromotionsForCampaign(row[PromotionCampaigns.id].value)
            row.toPromotionCampaign(promotions)
        }
    }

    override suspend fun findActive(): List<PromotionCampaign> {
        return dbQuery {
            PromotionCampaigns.selectAll().where {
                PromotionCampaigns.active eq true
            }.map {
                val promotions = getPromotionsForCampaign(it[PromotionCampaigns.id].value)
                it.toPromotionCampaign(promotions)
            }
        }
    }

    override suspend fun findAll(): List<PromotionCampaign> {
        return dbQuery {
            PromotionCampaigns.selectAll()
                .orderBy(PromotionCampaigns.createdAt, SortOrder.DESC)
                .map {
                    val promotions = getPromotionsForCampaign(it[PromotionCampaigns.id].value)
                    it.toPromotionCampaign(promotions)
                }
        }
    }

    override suspend fun save(campaign: PromotionCampaign): Result<Unit> = runCatching {
        dbQuery {
            val isNew = campaign.id.isBlank() || campaign.id == "00000000-0000-0000-0000-000000000000"
            val campaignUuid = if (isNew) UUID.randomUUID() else campaign.id.toUuid()

            PromotionCampaigns.upsert(PromotionCampaigns.id) {
                it[id] = campaignUuid
                it[code] = campaign.code
                it[name] = campaign.name
                it[description] = campaign.description
                it[active] = campaign.active
                it[startsAt] = campaign.startsAt
                it[expiresAt] = campaign.expiresAt
                it[updatedAt] = Clock.System.now()
                if (isNew) {
                    it[createdAt] = Clock.System.now()
                }
            }

            // Atomic Sync of Promotions
            Promotions.deleteWhere { campaignId eq campaignUuid }

            campaign.promotions.forEach { promo ->
                Promotions.insert {
                    it[campaignId] = campaignUuid
                    it[code] = promo.code
                    it[name] = promo.name
                    it[type] = promo.type.name
                    it[value] = promo.value
                    it[expiresAt] = promo.expiresAt
                    it[createdAt] = promo.createdAt
                    it[updatedAt] = promo.updatedAt
                }
            }
        }
    }
}
