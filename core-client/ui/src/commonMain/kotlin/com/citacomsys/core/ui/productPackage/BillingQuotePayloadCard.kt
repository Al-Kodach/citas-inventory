package com.citacomsys.core.ui.productPackage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.citacomsys.api.billing.*
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.types.ResourcePriceBreakdown

/**
 * A specialized card that automatically inspects a [BillingQuote] and its payload
 * to display the correct quota items and price breakdown.
 */
@Composable
fun BillingQuotePayloadCard(
    quote: BillingQuote,
    modifier: Modifier = Modifier
) {
    val payload = quote.payload

    val (title, subtitle, quotas, breakdown) = when (payload) {
        is SubscriptionBillingPayload -> {
            val pkg = payload.provisionedPackage
            listOf(
                pkg.packageName,
                pkg.tier.name,
                buildList {
                    add(QuotaItem("Users", pkg.maxUsers.toString(), AppIcons.People))
                    add(QuotaItem("Devices", pkg.maxDevices.toString(), AppIcons.Devices))
                    add(QuotaItem("SMS Quota", pkg.smsQuotaPerCycle.toString(), AppIcons.Sms))
                    add(QuotaItem("Stores", pkg.maxBranches.toString(), AppIcons.LocationCity))
                    add(QuotaItem("Products Catalog", pkg.maxProductsLimit.toString(), AppIcons.Inventory2))
                    add(QuotaItem("Active Batches", pkg.maxActiveBatchesLimit.toString(), AppIcons.Inventory))
                    add(QuotaItem("History Retention", pkg.historyRetentionMonths.toString(), AppIcons.History))
                },
            )
        }

        is SmsTopupBillingPayload -> {
            listOf(
                "SMS Top-up",
                "${payload.totalSms} Credits",
                listOf(QuotaItem("Total SMS", payload.totalSms.toString(), AppIcons.Sms)),
                payload.resourcePriceBreakdown
            )
        }
    }

    QuotaDetailsCard(
        title = title as String,
        subtitle = subtitle as String,
        quotas = quotas as List<QuotaItem>,
        resourcePriceBreakdown = breakdown as ResourcePriceBreakdown,
        discounts = quote.discounts,
        taxes = quote.taxes,
        modifier = modifier
    )
}
