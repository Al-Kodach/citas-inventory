package com.citacomsys.api.platform.repository

import com.citacomsys.api.platform.model.PlatformContent
import com.citacomsys.api.platform.model.PlatformDemo
import com.citacomsys.api.platform.model.PlatformFeature
import com.citacomsys.api.platform.model.PlatformTestimonial

interface PlatformContentRepository {
    suspend fun getPlatformContent(): PlatformContent
    suspend fun saveFeature(feature: PlatformFeature): Result<Unit>
    suspend fun saveDemo(demo: PlatformDemo): Result<Unit>
    suspend fun saveTestimonial(testimonial: PlatformTestimonial): Result<Unit>
}