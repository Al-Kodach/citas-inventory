package com.citacomsys.shared.util

import kotlinx.serialization.json.Json

object JsonObjAdaptor {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    inline fun <reified T> encode(value: T): String {
        return json.encodeToString(value)
    }

    inline fun <reified T> decode(jsonString: String): T? {
        if (jsonString.isBlank()) return null
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            println("JSON Decode Error: ${e.message}")
            null
        }
    }
}
