package com.citacomsys.feature.platform.gateway

import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.model.PromotionType
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.model.SmsPackage
import com.citacomsys.api.platform.repository.PlatformContentRepository
import com.citacomsys.api.platform.remote.*
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.api.platform.uiModel.PromotionUiState
import com.citacomsys.api.platform.uiModel.SmsPackageUiState
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.shared.displayName
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.Money
import com.citacomsys.types.ResourcePriceBreakdown
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import kotlin.time.Clock

internal class PackagePresentationController(
    private val packageService: ProductPackageService,
    private val smsPackageService: SmsPackageService,
    private val campaignService: PromotionCampaignService,
    private val billingService: BillingService,
    private val packageProvisioningService: PackageProvisioningService,
    private val platformContentRepository: PlatformContentRepository
) {

    suspend fun getPlatformContent(call: ApplicationCall) {
        val content = platformContentRepository.getPlatformContent()
        call.respond(content)
    }

    suspend fun getPublicPackageUiStates(call: ApplicationCall) {
        val packages = packageService.getPublicPackages()
        val uiStates = packages.map { mapToUiState(it) }
        call.respond(uiStates)
    }

    suspend fun getSmsPackageUiStates(call: ApplicationCall) {
        val packages: List<SmsPackage> = smsPackageService.getActive()
        val uiStates = packages.map { mapToSmsUiState(it) }
        call.respond(uiStates)
    }

    private suspend fun mapToSmsUiState(pkg: SmsPackage): SmsPackageUiState {
        val campaign = pkg.campaignId?.let { campaignService.get(it) }
        val now = Clock.System.now()

        var promoPrice: Money? = null
        var savings: Money? = null

        // Safety base configuration wrap matching currency
        val originalPriceMoney = pkg.price

        if (campaign != null && campaign.active) {
            campaign.promotions.forEach { promo ->
                when (promo.type) {
                    PromotionType.PERCENTAGE_DISCOUNT -> {
                        val discountValue =
                            (originalPriceMoney.amountMinor * promo.value.toLong() + 50) / 100
                        val discountMoney = Money(discountValue, originalPriceMoney.currencyCode)

                        savings = discountMoney
                        promoPrice = Money(
                            originalPriceMoney.amountMinor - discountValue,
                            originalPriceMoney.currencyCode
                        )
                    }

                    else -> {}
                }
            }
        }

        val remainingDays = campaign?.expiresAt?.let {
            (it - now).inWholeDays.toInt().coerceAtLeast(0)
        } ?: 0

        return SmsPackageUiState(
            id = pkg.id,
            units = pkg.units,
            originalPrice = originalPriceMoney.formatted,
            promoPrice = promoPrice?.formatted,
            savings = savings?.let { "Save ${it.formatted}" },
            hasPromo = campaign != null && campaign.active,
            campaignId = campaign?.id ?: "",
            campaignCode = campaign?.code ?: "",
            remainingDaysToEnd = remainingDays,
            effects = campaign?.promotions?.map {
                PromotionUiState(name = it.name, description = it.code, temporary = true)
            } ?: emptyList()
        )
    }

    private suspend fun mapToUiState(details: ProductPackageDetails): ProductPackageUiState {
        val pkg = details.packageInfo
        val limits =
            details.limits // Accessing our streamlined, fixed limits table abstraction data model

        return ProductPackageUiState(
            id = pkg.id,
            name = pkg.name,
            description = pkg.description,
            recommended = pkg.recommended,
            deploymentMode = details.billingTerms.firstOrNull()?.deploymentMode ?: DeploymentMode.CLOUD_SYNCED,

            // Cleanly outputting fixed metrics boundaries
            maxUsers = limits.maxUsers,
            maxDevices = limits.maxDevice,
            maxBranches = limits.maxBranches,
            maxProductsLimit = limits.maxProductsLimit,
            maxActiveBatchesLimit = limits.maxActiveBatchesLimit,
            smsQuotaPerCycle = pkg.smsQuotaPerCycle,
            historyRetentionMonths = limits.historyRetentionMonths,

            supportedModules = details.modules,
            billingTerms = details.billingTerms.map { mapBillTerm(it, details.billingTerms) }
        )
    }

    private suspend fun mapBillTerm(
        term: PackageBillingTerm,
        allTerms: List<PackageBillingTerm>
    ): BillTermUiState {
        val campaign = term.campaignId?.let { campaignService.get(it) }
        val now = Clock.System.now()

        var promoPrice: Money? = null
        var savings: Money? = null

        val basePriceMoney = term.priceAmount

        if (campaign != null && campaign.active) {
            campaign.promotions.forEach { promo ->
                when (promo.type) {
                    PromotionType.PERCENTAGE_DISCOUNT -> {
                        val discountValue =
                            (basePriceMoney.amountMinor * promo.value.toLong() + 50) / 100
                        val discountMoney = Money(discountValue, basePriceMoney.currencyCode)

                        savings = discountMoney
                        promoPrice = Money(
                            basePriceMoney.amountMinor - discountValue,
                            basePriceMoney.currencyCode
                        )
                    }

                    else -> {}
                }
            }
        }

        val remainingDays = when {
            campaign?.expiresAt != null -> (campaign.expiresAt!! - now).inWholeDays.toInt()
                .coerceAtLeast(0)

            else -> null
        }

        // --- AUTOMATED PRICING STRATEGY LOGIC ---
        val effectivePriceMinor = promoPrice?.amountMinor ?: basePriceMoney.amountMinor

        val pricePerMonthFormatted =
            if (term.billingCycle == com.citacomsys.types.BillingCycle.YEARLY) {
                Money(effectivePriceMinor / 12, basePriceMoney.currencyCode).formatted
            } else {
                Money(effectivePriceMinor, basePriceMoney.currencyCode).formatted
            }

        // Calculate automated savings percentage relative to the Monthly term
        val monthlyTerm =
            allTerms.find { it.billingCycle == com.citacomsys.types.BillingCycle.MONTHLY }
        val autoSavingsPercent =
            if (term.billingCycle == com.citacomsys.types.BillingCycle.YEARLY && monthlyTerm != null) {
                val monthlyEffective = (monthlyTerm.priceAmount.amountMinor).toDouble()
                val yearlyMonthlyEquivalent = effectivePriceMinor.toDouble() / 12
                if (yearlyMonthlyEquivalent < monthlyEffective) {
                    ((1.0 - (yearlyMonthlyEquivalent / monthlyEffective)) * 100).toInt()
                } else null
            } else null

        return BillTermUiState(
            id = term.id,
            packageId = term.packageId,
            deploymentMode = term.deploymentMode,
            billingCycleName = term.billingCycle.displayName(),
            originalPrice = basePriceMoney.formatted,
            promoPrice = promoPrice?.formatted,
            pricePerMonth = pricePerMonthFormatted,
            automatedSavingsPercent = autoSavingsPercent,
            savings = savings?.formatted,
            trialDays = term.trialDays,
            isDefault = term.isDefault,
            hasPromo = campaign != null && campaign.active,
            campaignId = campaign?.id ?: "",
            campaignCode = campaign?.code ?: "",
            remainingDaysToEnd = remainingDays,
            effects = campaign?.promotions?.map {
                PromotionUiState(name = it.name, description = it.code, temporary = true)
            } ?: emptyList()
        )
    }


    suspend fun calculateQuote(call: ApplicationCall) {
        val request = call.receive<PkgProvisionRequest>()

        // We need to return ResourcePriceBreakdown, not just line items
        val breakdown = call.transactionManager.runReadOnly {
            val provision = packageProvisioningService.provision(request).getOrThrow()

            // Map ProvisionedPackage to ResourcePriceBreakdown
            ResourcePriceBreakdown(
                lineItems = provision.lineItems,
                subtotal = provision.calculatedPrice,
                total = provision.calculatedPrice,
            )
        }
        call.respond(breakdown)
    }

    suspend fun getInvoicePreview(call: ApplicationCall) {
        val request = call.receive<PreviewRequest>()
        val preview = call.transactionManager.runReadOnly {
            billingService.invoicePreviewRequest(request)
        }
        call.respond(preview)
    }
}
