package com.citacomsys.api.billing.navkeys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TenantBillingNav : NavKey

@Serializable
@SerialName("core.billing.hub")
data object SubscriptionHubNav : TenantBillingNav // The main entry

@Serializable
sealed interface SubscriptionManagementRoute : TenantBillingNav

@Serializable
data object SubscriptionManagementNav : SubscriptionManagementRoute

@Serializable
data object UpgradeReviewNav : SubscriptionManagementRoute


@Serializable
data object BillingHistoryNav : TenantBillingNav

@Serializable
data class HistoryInvoiceDetailsNav(val invoiceId: String) : TenantBillingNav

@Serializable
data class InvoiceDetailsNav(val invoiceId: String) : TenantBillingNav

@Serializable
data object SmsTopupNav : TenantBillingNav

@Serializable
data class SmsPurchaseReviewNav(val packageId: String) : TenantBillingNav

