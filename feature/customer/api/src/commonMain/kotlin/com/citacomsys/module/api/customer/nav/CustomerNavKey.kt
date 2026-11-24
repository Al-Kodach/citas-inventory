package com.citacomsys.module.api.customer.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("customer")
sealed interface CustomerNavKey : NavKey

@Serializable
@SerialName("customer.list")
data object CustomerListNav : CustomerNavKey

@Serializable
@SerialName("customer.registration")
data class CustomerRegistrationNav(val customerId: String? = null) : CustomerNavKey

@Serializable
@SerialName("customer.ledger")
data class CustomerDetailsNav(
    val customerId: String,
    val workspaceId: String
) : CustomerNavKey

