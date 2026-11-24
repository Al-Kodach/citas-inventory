package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class DataClassification {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    RESTRICTED
}