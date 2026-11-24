package com.citacomsys.api.sales.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
@SerialName("sales")
sealed interface SalesModuleNavKey : NavKey

@Serializable
@SerialName("sales.list")
data object SalesListNav : SalesModuleNavKey

@Serializable
@SerialName("sales.details")
data class SalesDetailsNavKey(
    val id: String,
    val wsId: String? = null
) : SalesModuleNavKey


@Serializable
@SerialName("receipt")
data class ReceiptNavKey(val id: String) : SalesModuleNavKey

@Serializable
@SerialName("pos")
sealed interface PosModuleNavKey : NavKey

@Serializable
@SerialName("pos.catalog")
data object PosNavKey : PosModuleNavKey

@Serializable
@SerialName("pos.cart")
data object PosCartNavKey : PosModuleNavKey

@Serializable
@SerialName("pos.payment")
data object PosPaymentNavKey : PosModuleNavKey


@OptIn(ExperimentalSerializationApi::class)
val salesSerializerModule: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(PosNavKey::class, PosNavKey.serializer())
        subclass(PosCartNavKey::class, PosCartNavKey.serializer())
        subclass(PosPaymentNavKey::class, PosPaymentNavKey.serializer())
        subclass(SalesListNav::class, SalesListNav.serializer())
    }
}