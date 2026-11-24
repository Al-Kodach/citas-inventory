package com.citacomsys.api.auth

import kotlinx.coroutines.flow.MutableStateFlow

data class PostRegistrationCredentials(val username: String, val password: String)

/**
 * A singleton service that acts as a trigger for authentication events,
 * allowing decoupled features like Onboarding to initiate an auth flow.
 */
object AuthTrigger {
    // A flow that can hold the credentials from a new registration
    val nextLoginAttempt = MutableStateFlow<PostRegistrationCredentials?>(null)

    fun setPostRegistrationCredentials(credentials: PostRegistrationCredentials) {
        nextLoginAttempt.value = credentials
    }

    fun consumeNextLoginAttempt(): PostRegistrationCredentials? {
        val creds = nextLoginAttempt.value
        nextLoginAttempt.value = null
        return creds
    }
}
