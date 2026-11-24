package com.citacomsys.api.auth

import kotlinx.serialization.Serializable


@Serializable
sealed interface AuthState {
    @Serializable

    data object Authenticating : AuthState
    @Serializable

    data object Authenticated : AuthState
    @Serializable

    data object Locked : AuthState
    @Serializable

    data object ResetPassword : AuthState
    @Serializable

    data object OtpVerification : AuthState
    @Serializable

    data object UnAuthenticated : AuthState
    @Serializable

    data class SecurityLockout(
        val isUnregistered: Boolean = false
    ) : AuthState
}
