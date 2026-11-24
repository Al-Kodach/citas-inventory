package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("Address")
@Serializable
data class Address(
    val street: String,
    val city: String,
    val country: String
)
