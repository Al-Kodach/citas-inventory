package com.citacomsys.billing.service

import com.citacomsys.api.billing.BillingEffect
import com.citacomsys.api.billing.BillingPayload
import com.citacomsys.api.billing.BillingQuote
import com.citacomsys.api.billing.PayloadType
import com.citacomsys.api.billing.SubscriptionBillingPayload
import com.citacomsys.api.billing.SmsTopupBillingPayload
import com.citacomsys.api.platform.model.AppliedPromotion
import com.citacomsys.api.platform.model.Promotion
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.platform.repository.ProductPackageRepository
import com.citacomsys.api.platform.repository.PromotionCampaignRepository
import com.citacomsys.api.platform.repository.SmsPackageRepository
import com.citacomsys.api.platform.remote.PackageProvisioningService
import com.citacomsys.billing.engine.PromotionEngine
import com.citacomsys.billing.engine.QuotePricingCalculator
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.WorkspaceResources
import com.citacomsys.core.persistence.table.tenant.WorkspaceSubscriptionsTable
import com.citacomsys.types.BillingType
import com.citacomsys.types.Money
import com.citacomsys.types.ResourcePriceBreakdown
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import kotlin.time.Clock

internal class BillingQuoteService(
    private val provisioningService: PackageProvisioningService,
    private val quotePricingCalculator: QuotePricingCalculator,
    private val promotionEngine: PromotionEngine,
    private val campaignRepository: PromotionCampaignRepository,
    private val smsPackageRepository: SmsPackageRepository,
    private val packageRepo: ProductPackageRepository,
    private val clock: Clock = Clock.System
) {

    suspend inline fun generatePackageQuote(
        provisioned: ProvisionedPackage,
        type: BillingType,
        payloadType: PayloadType,
        title: String
    ): BillingQuote {
        val desc = buildString {
            append(provisioned.packageName)
            append(" • ")
            append(provisioned.tier.name)
        }

        val campaign = provisioned.campaignId?.let { campaignRepository.get(it).getOrThrow() }

        // Adapt your payload to store your new sub-unit Long value natively
        val baseSubtotalMoney = provisioned.calculatedPrice

        return buildQuote(
            QuoteContext(
                type = type,
                title = title,
                description = desc,
                payload = SubscriptionBillingPayload(provisioned, payloadType),
                subtotal = baseSubtotalMoney
            ),
            promotions = campaign?.promotions ?: emptyList(),
            appliedPromotions = campaign?.promotions?.map {
                AppliedPromotion(
                    campaignId = campaign.id,
                    promotionCode = it.code,
                    expiresAt = it.expiresAt
                )
            }.orEmpty()
        )
    }


    suspend fun generate(request: PkgProvisionRequest): BillingQuote {
        val provisioned = provisioningService.provision(request).getOrThrow()

        return generatePackageQuote(
            provisioned,
            type = BillingType.SUBSCRIPTION_UPGRADE,
            title = "Plan Upgrade: ${provisioned.packageName}",
            payloadType = PayloadType.UPGRADE
        )
    }

    suspend fun generateRenewalQuote(
        workspaceIdId: String,
        applyPromo: Boolean = true
    ): BillingQuote {
        val (packageId, billingCycle) = WorkspaceResources.select(
            WorkspaceResources.packageId,
            WorkspaceSubscriptionsTable.billingCycle
        )
            .where { WorkspaceResources.workspaceId eq workspaceIdId.toUuid() }
            .single().let {
                it[WorkspaceResources.packageId] to it[WorkspaceSubscriptionsTable.billingCycle]
            }

        // Resolve the latest package details and the specific term they are on
        val packageDetails = packageRepo.getPackageDetails(packageId).getOrThrow()

        val currentTerm =
            packageDetails.billingTerms.find { it.billingCycle == billingCycle }
                ?: packageDetails.billingTerms.first { it.isDefault }

        // PROVISION the package fresh.
        // This is key: it pulls the LATEST PackageLimits and ProductModules from the DB
        val provisioned = provisioningService.provision(
            PkgProvisionRequest(
                packageId = packageId,
                billTermId = currentTerm.id
            )
        ).getOrElse {
            error("Package Provisioning failed for renewal: ${it.message}")
        }

        //  Handle Promotions if the term has an active campaign
        val campaign = if (applyPromo) {
            currentTerm.campaignId?.let { campaignRepository.get(it).getOrNull() }
        } else null

        val payload = SubscriptionBillingPayload(
            provisionedPackage = provisioned,
            type = PayloadType.RENEWAL
        )

        return buildQuote(
            context = QuoteContext(
                type = BillingType.SUBSCRIPTION_RENEWAL,
                title = "Subscription Subscription",
                description = "${packageDetails.packageInfo.name} (${currentTerm.billingCycle.name})",
                payload = payload,
                subtotal = provisioned.calculatedPrice
            ),
            promotions = campaign?.promotions ?: emptyList(),
            appliedPromotions = campaign?.promotions?.map {
                AppliedPromotion(
                    campaignId = campaign.id,
                    promotionCode = it.code,
                    expiresAt = it.expiresAt
                )
            }.orEmpty()
        )
    }

    suspend fun generateSmsTopupQuote(packageId: String): BillingQuote {
        val pkg = smsPackageRepository.get(packageId).getOrThrow()
        val campaign = pkg.campaignId?.let { campaignRepository.get(it).getOrThrow() }

        // Keep core subunit safety
        val basePriceMoney = pkg.price

        val promoResult = promotionEngine.apply(
            promotions = campaign?.promotions ?: emptyList(),
            subtotal = basePriceMoney
        )

        val effects = buildList {
            add(BillingEffect.AddSms(pkg.units))
            addAll(promoResult.effects)
        }

        val payload = SmsTopupBillingPayload(
            packageId = packageId,
            purchasedUnits = pkg.units,
            effects = effects,
            resourcePriceBreakdown = ResourcePriceBreakdown(),
        )

        val appliedPromotions = campaign?.promotions?.map {
            AppliedPromotion(
                campaignId = campaign.id,
                promotionCode = it.code,
                expiresAt = it.expiresAt
            )
        }

        return buildQuote(
            QuoteContext(
                type = BillingType.SMS_TOPUP,
                title = "SMS Top-up",
                description = "${payload.purchasedUnits} SMS Credits",
                subtotal = basePriceMoney,
                payload = payload,
            ),
            appliedPromotions = appliedPromotions ?: emptyList(),
            promotions = campaign?.promotions ?: emptyList()
        )
    }

    private fun buildQuote(
        context: QuoteContext,
        appliedPromotions: List<AppliedPromotion>,
        promotions: List<Promotion>
    ): BillingQuote {
        val baseQuote = BillingQuote(
            type = context.type,
            title = context.title,
            description = context.description,
            payload = context.payload,
            subtotal = context.subtotal,
            total = context.subtotal,
            appliedPromotions = appliedPromotions,
            generatedAt = clock.now()
        )

        return quotePricingCalculator.apply(
            promotions = promotions,
            quote = baseQuote
        )
    }
}


data class QuoteContext(
    val type: BillingType,
    val subtotal: Money,
    val payload: BillingPayload,
    val title: String,
    val description: String
)
