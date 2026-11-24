package com.citacomsys.shared.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ApiWrapperSerializer::class)
data class ApiWrapper<T>(
    val data: T
)

class ApiWrapperSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<ApiWrapper<T>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(encoder: Encoder, value: ApiWrapper<T>) {
        dataSerializer.serialize(encoder, value.data)
    }

    override fun deserialize(decoder: Decoder): ApiWrapper<T> {
        return ApiWrapper(dataSerializer.deserialize(decoder))
    }
}
