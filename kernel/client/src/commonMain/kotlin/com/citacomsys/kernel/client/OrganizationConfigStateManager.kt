package com.citacomsys.kernel.client

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.remote.ApiValidationException
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.kernel.service.KernelApiService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.OrgRuntimeConfig
import com.citacomsys.shared.event.SubscriptionEvent
import eu.anifantakis.lib.ksafe.KSafe
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.koin.core.Koin
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Manages organization configuration state and reacts to token & subscription events.
 */
internal class OrganizationConfigStateManager(
    private val kernelApiService: KernelApiService,
    private val sessionManager: AppSessionManager,
    private val store: PlatformRuntimeStore,
    private val scope: CoroutineScope,
    private val prefs: KSafe
) : StartupTask {
    override val id: String = "org_config_state_manager"
    override val runLevel: StartupRunLevel = StartupRunLevel.POST_ONBOARDING
    override val order: Int = 10
    override val isCritical: Boolean = true

    override suspend fun run(koin: Koin) {
        sessionManager.appUser.filterNotNull().first()
        refreshOrgConfig().getOrThrow()
    }

    private val TAG = "ORG_CONFIG_MANAGER"
    private val refreshMutex = Mutex()
    private val CACHE_TTL_MS = 20.minutes.inWholeMilliseconds

    private var lastOrgConfigRefreshAt: Long
        get() = prefs.getDirect("last_refresh", 0L)
        set(value) = prefs.putDirect("last_refresh", value)

    private var observeStateJob: Job? = null

    private fun observeState() {
        if (observeStateJob != null) return

        observeStateJob = scope.launch {
            combine(
                store.organizationConfig,
                sessionManager.appUser
            ) { config, user ->
                if (user != null && config == null) {
                    Napier.d(tag = TAG) { "User session detected but no config, refreshing config..." }
                    delay(1000.milliseconds)
                    scope.launch { refreshOrgConfig(force = true) }
                }
            }.launchIn(scope)

            EventBus.subscribe<SubscriptionEvent>().onEach { event ->
                val activeWs = sessionManager.activeWorkspaceId
                val activeOrg = sessionManager.organizationId.orEmpty()
                if (activeWs != null && (event.id == activeWs || event.organizationId == activeOrg)) {
                    Napier.d(tag = TAG) { "Received subscription event for target ${event.id}: $event" }
                    scope.launch { refreshOrgConfig(force = true) }
                }
            }.launchIn(scope)

            EventBus.subscribe<OrgRuntimeConfig>().onEach { event ->
                val activeOrg = sessionManager.organizationId.orEmpty()
                if (event.organizationId.isBlank() || event.organizationId == activeOrg) {
                    Napier.d(tag = TAG) { "Received org runtime config update: $event" }
                    scope.launch { refreshOrgConfig(force = true) }
                }
            }.launchIn(scope)
        }
    }

    suspend fun refreshOrgConfig(force: Boolean = false): Result<Unit> = runCatching {
        val cachedConfig = store.organizationConfig.value
        val now = Clock.System.now().toEpochMilliseconds()
        val isFresh = (now - lastOrgConfigRefreshAt) < CACHE_TTL_MS

        if (!force && isFresh && cachedConfig != null) {
            Napier.d(tag = TAG) { "Organization config is fresh (< 15m ago). Using cache." }
            return@runCatching
        }

        // Cache-First: If a cached config exists and force is not explicitly required,
        // return cache immediately for non-blocking startup and refresh in background.
        if (cachedConfig != null && !force) {
            Napier.d(tag = TAG) { "Cached organization config available. Using cache immediately and refreshing in background..." }
            scope.launch { executeOrgConfigFetch(force = false) }
            return@runCatching
        }

        executeOrgConfigFetch(force = force)
    }

    private suspend fun executeOrgConfigFetch(force: Boolean = false) {
        if (!refreshMutex.tryLock()) {
            Napier.d(tag = TAG) { "Organization config refresh is already in progress. Skipping duplicate request." }
            return
        }

        try {
            sessionManager.appUser.first { it != null }
            val activeOrg = sessionManager.organizationId.orEmpty()
            val cachedEtag = prefs.getDirect("org_etag_$activeOrg", "").takeIf { it.isNotBlank() }

            Napier.d(tag = TAG) { "Organization config network refresh triggered for $activeOrg (etag=$cachedEtag)" }

            val etagResult = kernelApiService.getOrganizationConfig(
                organizationId = activeOrg.ifBlank { null },
                etag = if (force) null else cachedEtag
            )

            if (etagResult.isNotModified) {
                lastOrgConfigRefreshAt = Clock.System.now().toEpochMilliseconds()
                Napier.i(tag = TAG) { "Organization config ETag matched (304 Not Modified). Using cached config." }
                return
            }

            val config = etagResult.data ?: return
            store.saveOrganizationConfig(config)
            etagResult.etag?.let { prefs.putDirect("org_etag_$activeOrg", it) }
            lastOrgConfigRefreshAt = Clock.System.now().toEpochMilliseconds()
            Napier.i(tag = TAG) { "Successfully refreshed organization config from network (New ETag: ${etagResult.etag})." }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to refresh organization config" }
            if (e is ApiValidationException) {
                store.clearAll()
                EventBus.publish(AppLifecycleEvent.UserSessionRevoked)
                throw e
            }
            if (store.organizationConfig.value != null) {
                Napier.w(tag = TAG) { "Using cached organization config due to refresh error: ${e.message}" }
            } else {
                throw e
            }
        } finally {
            refreshMutex.unlock()
            observeState()
        }
    }
}
