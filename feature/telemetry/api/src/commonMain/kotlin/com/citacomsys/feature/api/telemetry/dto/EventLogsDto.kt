package com.citacomsys.feature.api.telemetry.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

@Serializable
data class ApplicationEventDTO(
    val id: String,
    val action: String,
    val level: String,
    val createdAt: Instant,
    val userId: String?,
    val branchId: String?,
    val payloadData: String? = null
) {
    fun getSummaryFromPayload(): String {
        return try {
            payloadData?.let { Json.parseToJsonElement(it).jsonObject["summary"]?.jsonPrimitive?.content }
                ?: "No summary provided"
        } catch (e: Exception) {
            "Error parsing summary"
        }
    }

    fun getEntityTypeFromPayload(): String {
        return try {
            payloadData?.let { Json.parseToJsonElement(it).jsonObject["entityType"]?.jsonPrimitive?.content }
                ?: "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun getEntityIdFromPayload(): String {
        return try {
            payloadData?.let { Json.parseToJsonElement(it).jsonObject["entityId"]?.jsonPrimitive?.content }
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

@Serializable
data class IdentityEventDTO(
    val id: String,
    val action: String,
    val level: String,
    val createdAt: Instant,
    val userId: String,
    val branchId: String?,
    val payloadData: String? = null
) {
    fun getOutcomeFromPayload(): String {
        return try {
            payloadData?.let { Json.parseToJsonElement(it).jsonObject["outcome"]?.jsonPrimitive?.content }
                ?: "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun getReasonFromPayload(): String? {
        return try {
            payloadData?.let { Json.parseToJsonElement(it).jsonObject["failureReason"]?.jsonPrimitive?.content }
        } catch (e: Exception) {
            null
        }
    }
}
