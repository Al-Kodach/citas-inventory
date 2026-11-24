package com.citacomsys.feature.support.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface SupportNavKey : NavKey

@Serializable
data object SupportHubNavKey : SupportNavKey

@Serializable
data class SubmitTicketNavKey(val initialType: String = "SUPPORT") : SupportNavKey

@Serializable
data class SupportTicketDetailNavKey(val ticketId: String) : SupportNavKey
