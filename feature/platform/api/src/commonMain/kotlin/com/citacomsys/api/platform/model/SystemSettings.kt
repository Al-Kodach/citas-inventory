package com.citacomsys.api.platform.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemSettings(
    val momoCollectionNumber: String,
    val momoCollectionName: String,
    val supportPhoneNumber: String,
    val maintenanceWindowEnabled: Boolean
)
