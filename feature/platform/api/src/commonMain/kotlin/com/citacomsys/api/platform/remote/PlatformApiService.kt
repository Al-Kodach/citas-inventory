package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.PlatformContent

interface PlatformApiService {
    suspend fun getWelcomeContent(): Result<PlatformContent>
}