package com.citacomsys.core.domain.error

data class FeatureNotAvailableException(val feature: String) :
    RuntimeException("Feature $feature is not available")

data class UnauthorizedException(override val message: String) : RuntimeException(message)