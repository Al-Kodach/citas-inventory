package com.citacomsys.kernel.model

import kotlinx.serialization.Serializable

sealed interface WsConfigurationState {
    data object Loading : WsConfigurationState

    @Serializable
    data class Configuring(
        val totalSteps: Int,
        val currentStep: Int,
        val currentStepMessage: String
    ) : WsConfigurationState

    @Serializable
    data class Error(val message: String) : WsConfigurationState

    data object Completed : WsConfigurationState
}
