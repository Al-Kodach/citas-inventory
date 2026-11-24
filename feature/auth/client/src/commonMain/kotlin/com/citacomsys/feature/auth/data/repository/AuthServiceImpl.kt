package com.citacomsys.feature.auth.data.repository

import com.citacomsys.api.auth.service.AuthApiService
import com.citacomsys.core.database.dao.UserDao
import com.citacomsys.core.database.dao.UserWorkspaceBindingDao
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.remote.isErrorRetriable
import com.citacomsys.core.security.EnrolledUser
import com.citacomsys.core.security.OfflineAuthenticator
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.UserWorkspaceBindingEvent
import com.citacomsys.types.ActionToken
import com.citacomsys.types.AuthChallengeContext
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.RemoteAuthResult
import com.citacomsys.types.User
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
internal class AuthServiceImpl(
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore,
    private val offlineAuthenticator: OfflineAuthenticator,
    private val sessionManager: AppSessionManager,
    private val userDao: UserDao,
    private val userWorkspaceBindingDao: UserWorkspaceBindingDao,
    private val scope: CoroutineScope
) : AuthService {

    private var pendingChallenge: AuthChallengeContext? = null

    init {
        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.UserSessionRevoked>().collect {
                tokenStore.clearUserSession()
                sessionManager.setWorkspaceBindings(emptyList())
                sessionManager.setAppUser(null)
            }
        }

        scope.launch {
            EventBus.subscribe<UserWorkspaceBindingEvent>().collect { event ->
                if (event.userId == sessionManager.currentUserId) {
                    Napier.i("UserWorkspaceBindingEvent received for active user ${event.userId}. Refreshing user workspace bindings...")
                    authApi.fetchSelfWorkspaceBindings().onSuccess { bindings ->
                        userWorkspaceBindingDao.saveAll(bindings)
                        sessionManager.setWorkspaceBindings(bindings)
                    }
                    if (sessionManager.activeWorkspaceId == event.workspaceId && event.action == "DELETED" || event.action == "REVOKED") {
                        EventBus.publish(AppLifecycleEvent.ExistWorkspace)
                    }
                }
            }
        }
    }

    override suspend fun login(identity: String, password: String): RemoteAuthResult {

        val cloudResult = runCatching {
            authApi.login(identity, password)
        }

        if (cloudResult.isSuccess) {
            when (val response = cloudResult.getOrThrow()) {
                is RemoteAuthResult.MfaRequired -> {
                    pendingChallenge = response.challenge
                }

                is RemoteAuthResult.VerificationRequired -> {
                    pendingChallenge = response.challenge
                }

                is RemoteAuthResult.Success -> {
                    pendingChallenge = null
                    val session = response.session
                    sessionManager.setAppUser(session.user)
                    sessionManager.setWorkspaceBindings(session.workspaceBindings)
                    tokenStore.save(session.tokens)
                    userDao.save(UserDao.asEntity(session.user))
                    userWorkspaceBindingDao.saveAll(session.workspaceBindings)

                    offlineAuthenticator.enroll(
                        EnrolledUser(
                            userId = session.user.id,
                            identity = session.user.username
                        ),
                        password
                    )
                }

                else -> {}
            }

            return cloudResult.getOrThrow()
        }

        // If network failed (timeout/500), fall through to offline.
        // If 401/403, return the error immediately.
        val error = cloudResult.exceptionOrNull()
        if (!isErrorRetriable(error)) return cloudResult.getOrThrow()

        // ATTEMPT OFFLINE LOGIN (Fallback or Standalone)
        return verifyLocally(identity, password).getOrThrow()
    }

    override suspend fun logout() {
        sessionManager.setAppUser(null)
        sessionManager.setWorkspaceBindings(emptyList())

        if (tokenStore.accessToken.value == null) return
        val accessToken = tokenStore.accessToken.value!!

        GlobalScope.launch {
            runCatching {
                authApi.logout(accessToken)
            }.onSuccess {
                Napier.d("Successfully revoked user session")
            }.onFailure {
                Napier.e("Failed to revoke user session", it)
            }
        }

        tokenStore.clearUserSession()
    }

    override suspend fun generateActionToken(
        password: String,
        purpose: String,
        reason: String?
    ): ActionToken {
        return authApi.generateAct(password, purpose, reason)
    }

    override suspend fun requestPasswordReset(identifier: String): Result<AuthChallengeContext> =
        runCatching {
            authApi.requestPasswordReset(identifier)
        }

    override suspend fun submitPasswordReset(
        challenge: AuthChallengeContext,
        otp: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        authApi.submitPasswordReset(challenge, otp, newPassword)
    }

    override suspend fun enrollAuthUser(
        tokens: AuthTokens,
        user: User,
        password: String,
    ) {
        offlineAuthenticator.enroll(
            EnrolledUser(
                userId = user.id,
                identity = user.username
            ),
            password
        )
        userDao.save(UserDao.asEntity(user))
        sessionManager.setAppUser(user)
        tokenStore.save(tokens)
    }

    override suspend fun verifyMfa(otpCode: String): Result<AuthenticatedSession> = runCatching {
        val session = authApi.verifyMfa(otpCode, pendingChallenge)
        pendingChallenge = null

        sessionManager.setAppUser(session.user)
        sessionManager.setWorkspaceBindings(session.workspaceBindings)
        tokenStore.save(session.tokens)
        userDao.save(UserDao.asEntity(session.user))
        userWorkspaceBindingDao.saveAll(session.workspaceBindings)

        session
    }

    private suspend fun verifyLocally(
        identity: String,
        password: String
    ): Result<RemoteAuthResult> {
        val identityNormalized = PhoneNumber.tryCreate(identity)?.normalized ?: identity

        val credential =
            offlineAuthenticator.authenticate(identityNormalized, password).getOrThrow()

        val entity = userDao.findById(credential.userId).first() ?: error("Invalid credentials")

        val user = UserDao.asDomain(entity)
        val bindings = userWorkspaceBindingDao.getBindingsForUser(user.id).first()

        sessionManager.setAppUser(user)
        sessionManager.setWorkspaceBindings(bindings)

        tokenStore.saveAccessToken("OFFLINE")

        return Result.success(
            RemoteAuthResult.Success(
                session = AuthenticatedSession(
                    AuthTokens(
                        "OFFLINE",
                        "OFFLINE"
                    ),
                    user = user,
                    workspaceBindings = bindings
                )
            )
        )
    }


}
