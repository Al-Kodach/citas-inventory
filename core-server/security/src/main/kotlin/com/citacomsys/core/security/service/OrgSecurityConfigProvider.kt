package com.citacomsys.core.security.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.core.security.plugin.OrgSecurityConfig
import com.citacomsys.shared.event.CitasEvent
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.WorkspaceStatus

class OrgSecurityConfigProvider(
    private val organizationRepository: OrganizationRepository,
    private val scope: CoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, OrgSecurityConfig>()

    init {
        scope.launch {
            EventBus.subscribe<CitasEvent>()
                .filter { it.isOrgTargetEvent }
                .collect { event ->
                    if (event.organizationId.isNotBlank()) {
                        logger.debug { "CitasEvent received for org $event, invalidating OrgSecurityConfig cache." }
                        invalidate(event.organizationId)
                    }
                }
        }
    }

    suspend fun get(organizationId: String): OrgSecurityConfig? {
        if (organizationId.isBlank()) return null

        cache.getIfPresent(organizationId)?.let { return it }

        return runCatching {
            val runtimeConfig = organizationRepository.getRuntimeConfig(organizationId)
            val config = OrgSecurityConfig(
                organizationId = organizationId,
                orgStatus = runtimeConfig.status,
                activeWorkspaceIds = runtimeConfig.workspaces.filter { it.status == "ACTIVE" }.map { it.id }.toSet(),
                cloudSyncedWorkspaceIds = runtimeConfig.workspaces.filter { it.deploymentMode == DeploymentMode.CLOUD_SYNCED }.map { it.id }.toSet()
            )
            cache.put(organizationId, config)
            config
        }.onFailure { e ->
            logger.error(e) { "Failed to fetch OrgSecurityConfig for organization $organizationId" }
        }.getOrNull()
    }

    fun invalidate(organizationId: String) {
        cache.invalidate(organizationId)
    }
}
