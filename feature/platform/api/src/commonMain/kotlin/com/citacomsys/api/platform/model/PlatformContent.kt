package com.citacomsys.api.platform.model

import kotlinx.serialization.Serializable


@Serializable
data class PlatformFeature(
    val id: String,
    val title: String,
    val featureIcon: String,
    val module: String?,
    val description: String,
    val orderRank: Int,
    val isActive: Boolean
)

@Serializable
data class PlatformDemo(
    val id: String,
    val title: String,
    val videoUrl: String?,
    val imageUrl: String?,
    val orderRank: Int,
    val isActive: Boolean
)

@Serializable
data class PlatformTestimonial(
    val id: String,
    val name: String,
    val designation: String,
    val message: String,
    val imageUrl: String?,
    val orderRank: Int,
    val isActive: Boolean
)


@Serializable
data class PlatformContent(
    val features: List<PlatformFeature>,
    val demos: List<PlatformDemo>,
    val testimonials: List<PlatformTestimonial>
)