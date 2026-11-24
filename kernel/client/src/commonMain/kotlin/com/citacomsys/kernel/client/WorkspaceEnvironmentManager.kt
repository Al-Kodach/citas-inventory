package com.citacomsys.kernel.client

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.kernel.service.KernelApiService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.shared.event.WorkspaceEvent
import eu.anifantakis.lib.ksafe.KSafe
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Dedicated manager for refreshing workstation profiles across the workstation registry,
 * reacting to workspace/subscription events, and ensuring in-flight & TTL-cached refreshes per workspaceId are optimized.
 */
class WorkspaceEnvironmentManager(
    private val kernelApiService: KernelApiService,
    private val store: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager,
    private val prefs: KSafe,
    private val scope: CoroutineScope
) {
    private val TAG = "WORKSPACE_ENV_MANAGER"
    private val inFlightWsRefreshes = mutableSetOf<String>()
    private val wsRefreshLock = Mutex()

    private val lastWsRefreshTimestamps = mutableMapOf<String, Long>()
    private val CACHE_TTL_MS = 15.minutes.inWholeMilliseconds

    init {
        observeWorkspaceEvents()
    }

    private fun observeWorkspaceEvents() {
        EventBus.subscribe<SubscriptionEvent>().onEach { event ->
            val targetWs = event.id
            val isRelevant = targetWs == sessionManager.activeWorkspaceId || store.getRegistry()
                .containsKey(targetWs)
            if (isRelevant) {
                Napier.d(tag = TAG) { "Received subscription event for target $targetWs. Refreshing targeted workspace..." }
                scope.launch { refreshWorkspaceEnvironment(workspaceId = targetWs, force = true) }
            }
        }.launchIn(scope)

        EventBus.subscribe<WorkspaceEvent>().onEach { event ->
            val targetWs = event.workspaceId ?: sessionManager.activeWorkspaceId
            if (!targetWs.isNullOrBlank() && (targetWs == sessionManager.activeWorkspaceId || store.getRegistry()
                    .containsKey(targetWs))
            ) {
                Napier.d(tag = TAG) { "Received workspace event for $targetWs. Refreshing targeted workspace..." }
                scope.launch { refreshWorkspaceEnvironment(workspaceId = targetWs, force = true) }
            }
        }.launchIn(scope)
    }

    /**
     * Refreshes a single workspace environment, deduplicating in-flight requests and checking 15m Cache TTL unless [force] is true.
     */
    suspend fun refreshWorkspaceEnvironment(
        workspaceId: String,
        force: Boolean = false
    ): Result<Unit> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastRefreshed = lastWsRefreshTimestamps[workspaceId] ?: 0L
        val isFresh = (now - lastRefreshed) < CACHE_TTL_MS

        if (!force && isFresh && store.getWorkspaceConfig(workspaceId) != null) {
            Napier.d(tag = TAG) { "Workspace $workspaceId profile is fresh (< 15m ago). Skipping network call." }
            return@runCatching
        }

        val shouldExecute = wsRefreshLock.withLock {
            if (inFlightWsRefreshes.contains(workspaceId)) {
                false
            } else {
                inFlightWsRefreshes.add(workspaceId)
                true
            }
        }

        if (!shouldExecute) {
            Napier.d(tag = TAG) { "Refresh for workspace $workspaceId is already in progress. Skipping duplicate request." }
            return@runCatching
        }

        try {
            val cachedEtag = prefs.getDirect("ws_etag_$workspaceId", "").takeIf { it.isNotBlank() }
            val etagResult = kernelApiService.refresh(workspaceId, etag = if (force) null else cachedEtag)

            if (etagResult.isNotModified) {
                lastWsRefreshTimestamps[workspaceId] = Clock.System.now().toEpochMilliseconds()
                Napier.i(tag = TAG) { "Workspace $workspaceId ETag matched (304 Not Modified). Using cached profile." }
                return@runCatching
            }

            val (config, state) = etagResult.data ?: return@runCatching
            store.updateWorkspaceConfig(config)
            store.updateWorkspaceState(state)
            etagResult.etag?.let { prefs.putDirect("ws_etag_$workspaceId", it) }
            lastWsRefreshTimestamps[workspaceId] = Clock.System.now().toEpochMilliseconds()
            Napier.i(tag = TAG) { "Successfully refreshed container profile & state for workspace: $workspaceId (New ETag: ${etagResult.etag})" }
        } finally {
            wsRefreshLock.withLock {
                inFlightWsRefreshes.remove(workspaceId)
            }
        }
    }

    suspend fun refreshWorkspace(workspaceId: String, force: Boolean = false): Result<Unit> {
        return refreshWorkspaceEnvironment(workspaceId, force)
    }

    /**
     * Refreshes registered workspaces, prioritizing the active workspace and skipping fresh caches.
     */
    suspend fun refreshAllRegisteredWorkspaces(force: Boolean = false): Result<Unit> = runCatching {
        val registry = store.getRegistry()
        if (registry.isEmpty()) {
            Napier.d(tag = TAG) { "No registered workstation profiles found in registry." }
            return@runCatching
        }

        val activeWsId = sessionManager.activeWorkspaceId
        Napier.i(tag = TAG) { "Checking state for ${registry.size} registered workspace container(s)..." }

        registry.forEach { (workspaceId, _) ->
            val isCurrentActive = workspaceId == activeWsId
            refreshWorkspaceEnvironment(workspaceId = workspaceId, force = force || isCurrentActive)
        }
    }
}
