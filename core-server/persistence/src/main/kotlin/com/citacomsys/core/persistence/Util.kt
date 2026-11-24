package com.citacomsys.core.persistence

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject?.getBool(key: String, default: Boolean = false): Boolean =
    this?.get(key)?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: default

fun JsonObject?.getInt(key: String, default: Int = 0): Int =
    this?.get(key)?.jsonPrimitive?.content?.toIntOrNull() ?: default

fun JsonObject?.getLong(key: String, default: Long = 0L): Long =
    this?.get(key)?.jsonPrimitive?.content?.toLongOrNull() ?: default

fun JsonObject?.getDouble(key: String, default: Double = 0.0): Double =
    this?.get(key)?.jsonPrimitive?.content?.toDoubleOrNull() ?: default

fun JsonObject.getString(key: String, default: String = ""): String =
    this[key]?.jsonPrimitive?.content ?: default