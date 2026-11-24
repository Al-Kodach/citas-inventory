package com.citacomsys.feature.platform.data.repository

import com.citacomsys.api.platform.model.PlatformContent
import com.citacomsys.api.platform.model.PlatformDemo
import com.citacomsys.api.platform.model.PlatformFeature
import com.citacomsys.api.platform.model.PlatformTestimonial
import com.citacomsys.api.platform.repository.PlatformContentRepository
import com.citacomsys.core.persistence.table.MarketingContentTable
import com.citacomsys.core.persistence.transaction.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

internal class ExposedPlatformContentRepository : PlatformContentRepository {
    override suspend fun getPlatformContent(): PlatformContent = dbQuery {
        val features = MarketingContentTable
            .selectAll()
            .where { (MarketingContentTable.contentType eq "FEATURE") and (MarketingContentTable.isActive eq true) }
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
            .where { (MarketingContentTable.contentType eq "DEMO") and (MarketingContentTable.isActive eq true) }
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
            .where { (MarketingContentTable.contentType eq "TESTIMONIAL") and (MarketingContentTable.isActive eq true) }
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

    override suspend fun saveFeature(feature: PlatformFeature): Result<Unit> = runCatching {
        dbQuery {
            MarketingContentTable.upsert {
                it[id] = feature.id.takeIf { id -> id.isNotBlank() } ?: UUID.randomUUID().toString()
                it[contentType] = "FEATURE"
                it[title] = feature.title
                it[description] = feature.description
                it[orderRank] = feature.orderRank
                it[isActive] = feature.isActive
            }
        }
    }

    override suspend fun saveDemo(demo: PlatformDemo): Result<Unit> = runCatching {
        dbQuery {
            MarketingContentTable.upsert {
                it[id] = demo.id.takeIf { id -> id.isNotBlank() } ?: UUID.randomUUID().toString()
                it[contentType] = "DEMO"
                it[title] = demo.title
                it[mediaUrl] = demo.videoUrl ?: demo.imageUrl
                it[orderRank] = demo.orderRank
                it[isActive] = demo.isActive
            }
        }
    }

    override suspend fun saveTestimonial(testimonial: PlatformTestimonial): Result<Unit> =
        runCatching {
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
            }
        }
}
