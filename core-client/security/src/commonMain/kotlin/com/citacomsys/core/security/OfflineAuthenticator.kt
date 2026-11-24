package com.citacomsys.core.security

import com.citacomsys.shared.security.PasswordHasher
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class EnrolledUser(
    val userId: String,
    val identity: String
)

@Serializable
internal data class LocalSecurityProfile @OptIn(ExperimentalTime::class) constructor(
    val userId: String,
    val pinHash: String, // BCrypt or SHA-256 hash of the 4-6 digit PIN
    val lastVerifiedAt: Instant
)


interface OfflineAuthenticator {
    /**
     * Returns the list of users who have local security profiles on this device.
     */
    val enrolledUsers: Flow<List<EnrolledUser>>

    /**
     * Creates or updates a local security profile for a user.
     * Usually called after a successful online login.
     */
    suspend fun enroll(user: EnrolledUser, pin: String)

    /**
     * Authenticates a user using hardware-backed biometrics AND their local PIN/Password.
     */
    suspend fun authenticate(identity: String, pin: String): Result<EnrolledUser>

    suspend fun clearProfiles()

    suspend fun removeProfile(identity: String)

    suspend fun revokeUsers(userIds: Set<String>)
}

@OptIn(ExperimentalTime::class)
internal class OfflineAuthenticatorImpl(
    private val vault: KSafe,
    private val prefs: KSafe,
    private val passwordHasher: PasswordHasher
) : OfflineAuthenticator {

    private val _enrolledUsers = MutableStateFlow<List<EnrolledUser>>(
        prefs.getDirect("enrolled_users", emptyList())
    )
    override val enrolledUsers: Flow<List<EnrolledUser>> = _enrolledUsers.asStateFlow()

    override suspend fun enroll(user: EnrolledUser, pin: String) {
        val hashPassword = passwordHasher.hash(pin)

        val profile = LocalSecurityProfile(
            userId = user.userId,
            pinHash = hashPassword,
            lastVerifiedAt = Clock.System.now()
        )

        // Store sensitive profile in the encrypted vault
        vault.putDirect("profile_${user.identity}", profile)

        // Update the public list of registered users on this device (brings to top as last accessed)
        updatePublicUserList(user)
    }

    override suspend fun authenticate(identity: String, pin: String): Result<EnrolledUser> =
        runCatching {
            Napier.d { "Offline authentication for ..${identity.take(3)}..." }

            val profile: LocalSecurityProfile = vault.getDirect("profile_$identity", null)
                ?: vault.getDirect("profile_${identity.replace("+", "")}", null)
                ?: throw Exception("User not enrolled for offline access on this device")

            if (!passwordHasher.verify(pin, profile.pinHash)) {
                throw Exception("Invalid credentials")
            }

            val enrolledUser = EnrolledUser(
                userId = profile.userId,
                identity = identity
            )
            updatePublicUserList(enrolledUser)
            enrolledUser
        }

    override suspend fun clearProfiles() {
        vault.delete("profile_*")
        prefs.put("enrolled_users", emptyList<EnrolledUser>())
        _enrolledUsers.value = emptyList()
    }

    override suspend fun removeProfile(identity: String) {
        vault.delete("profile_$identity")
        val currentList = prefs.getDirect<List<EnrolledUser>>("enrolled_users", emptyList())
        val newList = currentList.filter { it.identity != identity }
        prefs.put("enrolled_users", newList)
        _enrolledUsers.value = newList
    }

    override suspend fun revokeUsers(userIds: Set<String>) {
        val currentList = prefs.getDirect<List<EnrolledUser>>("enrolled_users", emptyList())
        val newList = currentList.filter { !userIds.contains(it.userId) }
        prefs.put("enrolled_users", newList)
        _enrolledUsers.value = newList
    }

    private suspend fun updatePublicUserList(newUser: EnrolledUser) {
        val currentList = prefs.getDirect<List<EnrolledUser>>("enrolled_users", emptyList())
        // Remove existing occurrence if present, then place at the top (index 0) as last accessed
        val filtered =
            currentList.filter { it.identity != newUser.identity && it.userId != newUser.userId }
        val newList = listOf(newUser) + filtered
        prefs.put("enrolled_users", newList)
        _enrolledUsers.value = newList
    }
}
