package com.citacomsys.shared.event

import com.citacomsys.types.PlatformUUID
import com.citacomsys.types.ProductType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant


interface CitasEvent {
    val organizationId: String get() = ""
    val importance: EventImportance get() = EventImportance.OPTIONAL

    /**
     * Context ID of the event: returns specific workspace ID if present for business events,
     * otherwise falls back to organizationId.
     */
    val id: String
        get() = when (this) {
            is EntityChangeEvent -> workspaceId ?: organizationId
            is SubscriptionEvent -> wsId.ifBlank { organizationId }
            is WorkspaceEvent -> workspaceId ?: organizationId
            is UserWorkspaceBindingEvent -> workspaceId ?: organizationId
            is WorkstationBindingEvent -> workspaceId ?: organizationId
            is WorkstationSecurityEvent -> workspaceId ?: organizationId
            is BusinessSecurityEvent -> workspaceId ?: organizationId
            is DeviceSecurityEvent -> workspaceId ?: organizationId
            is DeviceUpdatedEvent -> workspaceId ?: organizationId
            is UserCountEvent -> workspaceId ?: organizationId
            is BillingEvent -> workspaceId ?: organizationId
            is ThemeEvent -> targetId.ifBlank { organizationId }
            else -> organizationId
        }

    val isOrgTargetEvent: Boolean
        get() = when (this) {
            is SubscriptionEvent,
            is ModulePolicyVersionEvent,
            is BusinessSecurityEvent,
            is WorkstationSecurityEvent,
            is OrgRuntimeConfig -> true

            else -> false
        }

    val isBusiness: Boolean
        get() = when (this) {
            is EntityChangeEvent,
            is SubscriptionEvent,
            is WorkspaceEvent,
            is UserWorkspaceBindingEvent,
            is WorkstationBindingEvent,
            is BusinessSecurityEvent,
            is WorkstationSecurityEvent,
            is BillingEvent,
            is ThemeEvent -> true

            else -> false
        }
}

/**
 * Data Events: Focus on transactional business data changes (products, stocks, movements, orders, sales).
 */
interface DataEvent : CitasEvent

/**
 * Operation Events: Focus on operational and administrative changes (billing, subscriptions, bindings, workspaces, branding, users).
 */
interface OperationEvent : CitasEvent

/**
 * Security Events: Focus on security-critical changes (sessions, hardware, device revocation, workstation binding invalidations).
 */
interface SecurityEvent : CitasEvent

@Serializable
enum class EventImportance {
    CRITICAL,
    OPTIONAL
}

@Serializable
sealed class AppLifecycleEvent : CitasEvent {
    @Serializable
    data object Bootstrapping : AppLifecycleEvent()

    @Serializable
    data object PostOnboarding : AppLifecycleEvent()

    @Serializable
    data object CoreReady : AppLifecycleEvent()

    @Serializable
    data object ForceUpdateRequired : AppLifecycleEvent()

    @Serializable
    data object WorkstationReady : AppLifecycleEvent()

    @Serializable
    data object WorkspaceInfraConfiguration : AppLifecycleEvent()

    @Serializable
    data class WorkspaceSelected(
        val workspaceId: String,
        val branchId: String,
        val isWsLocked: Boolean = false,
        val isBranchLocked: Boolean = false,
        val productType: ProductType
    ) : AppLifecycleEvent()

    @Serializable
    data class WorkspaceRestore(
        val workspaceId: String,
        val branchId: String,
        val isWsLocked: Boolean = false,
        val isBranchLocked: Boolean = false,
    ) : AppLifecycleEvent()

    @Serializable
    data object WorkspaceInfraReady : AppLifecycleEvent()
    data object WorkspaceConfigCompleted : AppLifecycleEvent()

    @Serializable
    data object WorkspaceActive : AppLifecycleEvent()

    data object ExistWorkspace : AppLifecycleEvent()

    @Serializable
    data object OrganizationActive : AppLifecycleEvent()

    @Serializable
    data object UserSessionRevoked : AppLifecycleEvent()

    override val importance: EventImportance = EventImportance.CRITICAL
}


@Serializable
@SerialName("ThemeEvent")
data class ThemeEvent(
    val targetId: String,
    val isOrg: Boolean = false,
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    companion object {
        const val name: String = "event.theme.updated"
    }
}


@Serializable
@SerialName("BillEvent")
data class BillingEvent(
    val workspaceId: String? = null,
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    companion object {
        const val name: String = "event.billing"
    }
}

@Serializable
@SerialName("RuntimeConfigEvent")
data class OrgRuntimeConfig(
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    companion object {
        const val ID: String = "org.runtime.config"
    }
}

@Serializable
@SerialName("SubscriptionEvent")
data class SubscriptionEvent(
    val wsId: String,
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val ID: String = "workspace.subscription.change"
    }
}

@Serializable
@SerialName("WorkspaceEvent")
data class WorkspaceEvent(
    val workspaceId: String? = null,
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    companion object {
        const val ID: String = "workspace.updated"
    }
}

@Serializable
@SerialName("UserWorkspaceBindingEvent")
data class UserWorkspaceBindingEvent(
    val userId: String,
    val workspaceId: String? = null,
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    companion object {
        const val ID: String = "user.workspace.binding.updated"
    }
}

@Serializable
@SerialName("WorkstationBindingEvent")
data class WorkstationBindingEvent(
    val hardwareId: String,
    val workspaceId: String? = null,
    val action: String = "UPDATE",
    override val organizationId: String = ""
) : OperationEvent {
    companion object {
        const val name: String = "workstation.binding.updated"
    }
}

@Serializable
@SerialName("SecurityBridgeEvent")
data class SecurityBridgeEvent(
    val workspaceId: PlatformUUID? = null,
    val userId: String? = null,
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    init {
        require(workspaceId != null || userId != null) {
            "Either workspaceId or actorId must be provided"
        }
    }

    companion object {
        fun fromUserId(userId: String) = SecurityBridgeEvent(userId = userId)
        const val name: String = "event.security.bridge"
    }
}

@Serializable
@SerialName("ModulePolicyVersionEvent")
data class ModulePolicyVersionEvent(
    val version: Long,
    val workspaceId: PlatformUUID,
) : OperationEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name: String = "event.module.policy"
    }
}

@Serializable
@SerialName("EntityChangeEvent")
data class EntityChangeEvent(
    val seq: Long,
    val entityType: String, // "BILLING", "USER", "TENANT"
    val entityId: String,
    override val organizationId: String,
    val workspaceId: String?,
    val branchId: String?,
    val action: String = "SYNC", // "CREATED", "UPDATED", "DELETED"
    val timestamp: Instant,
) : DataEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name = "event.data.updated"
    }
}

@Serializable
data object RootedDeviceDetected : SecurityEvent

@Serializable
data object DebugAttachedDetected : SecurityEvent

@Serializable
data object DeviceFingerprintMismatch : SecurityEvent

@Serializable
data object DeviceLocked : SecurityEvent

@Serializable
data object DeviceRevoked : SecurityEvent

@Serializable
@SerialName("SecurityViolationEvent")
data class SecurityViolationEvent(
    val violation: String,
    val reason: String
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name = "event.security.violation"
    }
}

@Serializable
@SerialName("UserCountEvent")
data class UserCountEvent(
    val count: Int,
    val type: String,
    override val organizationId: String,
    val workspaceId: String? = null
) : OperationEvent {
    override val importance: EventImportance = EventImportance.OPTIONAL

    companion object {
        const val name = "event.user.count"
    }
}

@Serializable
@SerialName("WorkstationSecurityEvent")
data class WorkstationSecurityEvent(
    override val organizationId: String,
    val workspaceId: String? = null,
    val hardwareId: String,
    val reason: String
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        val name = "event.security.invalidation"
    }
}

@Serializable
@SerialName("BusinessSecurityEvent")
data class BusinessSecurityEvent(
    override val organizationId: String,
    val workspaceId: String? = null,
    val reason: String = "Workspace security invalidation"
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name = "event.workspace.security.invalidation"
    }
}

@Serializable
@SerialName("DeviceRevokedEvent")
data class DeviceRevokedEvent(
    override val organizationId: String,
    val hardwareId: String,
    val reason: String = "Hardware device globally revoked"
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name = "event.device.revoked"
    }
}

@Serializable
@SerialName("DeviceUpdatedEvent")
data class DeviceUpdatedEvent(
    override val organizationId: String,
    val hardwareId: String,
    val workspaceId: String? = null
) : OperationEvent {
    companion object {
        const val name = "event.device.updated"
    }
}

@Serializable
@SerialName("DeviceSecurityEvent")
data class DeviceSecurityEvent(
    override val organizationId: String,
    val hardwareId: String,
    val workspaceId: String? = null,
    val action: String = "UPDATE",
    val reason: String = "Hardware security invalidation"
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name = "event.device.security.invalidation"
    }
}

@Serializable
@SerialName("UserSessionRevokedEvent")
data class UserSessionRevokedEvent(
    override val organizationId: String,
    val sessionId: String,
    val userId: String? = null,
    val hardwareId: String? = null,
    val reason: String = "User session revoked"
) : SecurityEvent {
    override val importance: EventImportance = EventImportance.CRITICAL

    companion object {
        const val name = "event.user.session.revoked"
    }
}
