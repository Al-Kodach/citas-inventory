package com.citacomsys.inventory.admin.tenants

import com.citacomsys.admin.api.model.WorkspaceDetails
import com.citacomsys.admin.api.tenant.AdminPreRegisterRequest
import com.citacomsys.admin.api.tenant.AdminWorkspaceSummary
import com.citacomsys.admin.api.tenant.BranchSummary
import com.citacomsys.admin.api.tenant.OrganizationDetails
import com.citacomsys.api.device.node.DeviceRepository
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.api.organization.model.Organization
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.service.WorkspaceSubscriptionService
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.notification.service.SseService
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.table.tenant.BranchesTable
import com.citacomsys.core.persistence.table.tenant.Organizations
import com.citacomsys.core.persistence.table.tenant.WorkspaceSubscriptionsTable
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.table.user.Users
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.AuthorizationContextProvider
import com.citacomsys.core.security.service.PasswordGenerator
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.shared.event.SseMessage
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.admin.api.tenant.PublicTenantResolveResult
import com.citacomsys.core.persistence.table.tenant.WorkspaceResourceUsageTable
import com.citacomsys.core.persistence.table.tenant.WorkspaceResources
import com.citacomsys.core.persistence.table.user.UserWorkspaceBindings
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.EntityType
import com.citacomsys.types.Money
import com.citacomsys.types.PackageTier
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.ProductType
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import com.citacomsys.types.WorkspaceStatus
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock

class AdminTenantService(
    private val workspaceRepository: WorkspaceRepository,
    private val auditor: TelemetryAuditor,
    private val passwordHasher: PasswordHasher,
    private val notificationService: NotificationService,
    private val deviceRepository: DeviceRepository,
    private val branchRepository: BranchRepository,
    private val subscriptionService: WorkspaceSubscriptionService,
    private val authorizationContextProvider: AuthorizationContextProvider,
    private val sseService: SseService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Fetches a paginated, searchable list of all organizations (parent entities).
     */
    suspend fun fetchOrganizations(query: PageQuery): PageResult<Organization> {
        return Organizations.queryPage(
            pageQuery = query,
            searchColumns = listOf(
                Organizations.name,
                Organizations.businessCode
            ),
            sortMap = mapOf(
                "createdAt" to Organizations.createdAt,
                "ID" to Organizations.name,
                "status" to Organizations.status,
                "businessCode" to Organizations.businessCode,
                "phone" to Organizations.phone,
                "email" to Organizations.email
            ),
            filterBuilder = {
                andWhere { Organizations.id neq SystemActors.SYSTEM_ID.toUuid() }
                andWhere { Organizations.businessCode neq "SYSTEM" }
                andWhere { Organizations.businessCode neq "PLATFORM" }
            },
            defaultSort = Organizations.createdAt,
            mapper = { it.toOrganization() }
        )
    }

    private fun ResultRow.toOrganization() = Organization(
        id = this[Organizations.id].value.toString(),
        name = this[Organizations.name],
        ownerId = this[Organizations.ownerId].toString(),
        businessCode = this[Organizations.businessCode],
        email = this[Organizations.email],
        phone = this[Organizations.phone],
        fleetDna = this[Organizations.fleetDna] ?: this[Organizations.id].toString(),
        status = OrgStatus.valueOf(this[Organizations.status].uppercase()),
        createdAt = this[Organizations.createdAt],
        updatedAt = this[Organizations.updatedAt]
    )

    /**
     * Pre-registers a new organization and owner account by platform admin (without binding device).
     */
    suspend fun preRegisterOrganization(request: AdminPreRegisterRequest): OrganizationRegistrationResult =
        dbQuery {
            logger.info { "Admin pre-registering organization: ${request.organizationName}" }

            val exists =
                Organizations.selectAll()
                    .where {
                        Organizations.name eq request.organizationName or
                                (Organizations.phone eq request.ownerPhone.normalized) or
                                (Organizations.email eq request.ownerEmail)
                    }
                    .count() > 0
            if (exists) {
                error("An organization with this ID is already registered.")
            }

            val ownerExist = Users.selectAll()
                .where {
                    Users.username eq request.username or
                            (Users.phone eq request.ownerPhone.normalized) or
                            (Users.email eq request.ownerEmail)
                }.count() > 0

            if (ownerExist) {
                error("Owner account already registered.")
            }

            val businessCode = BusinessCodeGenerator.generate(request.organizationName)
            val fleetDna = "dna_${UUID.randomUUID().toString().take(12)}"
            val orgId = UUID.randomUUID()
            val ownerId = UUID.randomUUID()
            val tempPassword = PasswordGenerator.generateSecurePassword(12)

            Organizations.insert {
                it[Organizations.id] = orgId
                it[Organizations.name] = request.organizationName
                it[Organizations.businessCode] = businessCode
                it[Organizations.fleetDna] = fleetDna
                it[Organizations.status] = OrgStatus.PENDING_VERIFICATION.name
                it[Organizations.email] = request.ownerEmail?.ifBlank { null }
                it[Organizations.phone] = request.ownerPhone.normalized
                it[Organizations.ownerId] = ownerId
                it[Organizations.createdAt] = Clock.System.now()
                it[Organizations.updatedAt] = Clock.System.now()
            }

            val hashedPassword = passwordHasher.hash(tempPassword)

            Users.insert {
                it[Users.id] = ownerId
                it[Users.organizationId] = orgId
                it[Users.name] = request.ownerName
                it[Users.username] = request.username
                it[Users.phone] = request.ownerPhone.normalized
                it[Users.email] = request.ownerEmail?.ifBlank { null }
                it[Users.passwordHash] = hashedPassword
                it[Users.role] = "TENANT_OWNER"
                it[Users.capabilities] = "[]"
                it[Users.status] = UserStatus.REQUIRED_PHONE_VERIFICATION
                it[Users.userType] = UserType.TENANT
                it[Users.mfaEnabled] = true
                it[Users.isPhoneVerified] = false
                it[Users.createdAt] = Clock.System.now()
                it[Users.updatedAt] = Clock.System.now()
            }

            runCatching {
                notificationService.sendPreRegistrationNotice(
                    email = request.ownerEmail,
                    phone = request.ownerPhone,
                    orgName = request.organizationName,
                    businessCode = businessCode,
                    adminName = request.ownerName,
                    userId = ownerId.toString(),
                    tempPassword = tempPassword
                )
            }.onFailure {
                logger.error { "Failed to send pre-registration notice email to ${request.ownerEmail}: ${it.message}" }
            }

            // Provision initial Demo PRO Trial Workspace
            val workspaceId = UUID.randomUUID()
            val productKey = "pk_${UUID.randomUUID().toString().take(12).lowercase()}"

            Workspaces.insert {
                it[Workspaces.id] = workspaceId
                it[Workspaces.organizationId] = orgId
                it[Workspaces.name] = "${request.organizationName} Main Store"
                it[Workspaces.productType] = ProductType.RETAIL
                it[Workspaces.email] = request.ownerEmail?.ifBlank { null }
                it[Workspaces.phone] = request.ownerPhone.normalized
                it[Workspaces.deploymentMode] = DeploymentMode.CLOUD_SYNCED
                it[Workspaces.status] = WorkspaceStatus.ACTIVE
                it[Workspaces.productKey] = productKey
                it[Workspaces.businessCode] = "${businessCode}-01"
                it[Workspaces.createdAt] = Clock.System.now()
                it[Workspaces.updatedAt] = Clock.System.now()
            }

            // Demo PRO Quota Limits (Full PRO Feature Access with Demo Quotas)
            WorkspaceResources.insert {
                it[WorkspaceResources.workspaceId] = workspaceId
                it[WorkspaceResources.packageId] = "demo-pro-trial"
                it[WorkspaceResources.packageName] = "Demo PRO Trial"
                it[WorkspaceResources.tier] = PackageTier.PRO
                it[WorkspaceResources.maxUsers] = 3            // Demo Limit: 3 Users
                it[WorkspaceResources.maxDevice] = 2           // Demo Limit: 2 Workstations
                it[WorkspaceResources.maxBranches] = 2         // Demo Limit: 2 Branches
                it[WorkspaceResources.maxProductsLimit] = 100  // Demo Limit: 100 Products
                it[WorkspaceResources.maxActiveBatchesLimit] = 50
                it[WorkspaceResources.retentionMonths] = 3
                it[WorkspaceResources.updatedAt] = Clock.System.now()
            }

            WorkspaceResourceUsageTable.insert {
                it[WorkspaceResourceUsageTable.workspaceId] = workspaceId
                it[WorkspaceResourceUsageTable.productCountUsed] = 0
                it[WorkspaceResourceUsageTable.activeBatchesCount] = 0
                it[WorkspaceResourceUsageTable.userCount] = 1
                it[WorkspaceResourceUsageTable.deviceCount] = 0
                it[WorkspaceResourceUsageTable.branchCount] = 1
                it[WorkspaceResourceUsageTable.updatedAt] = Clock.System.now()
            }

            UserWorkspaceBindings.insert {
                it[UserWorkspaceBindings.id] = UUID.randomUUID()
                it[UserWorkspaceBindings.userId] = ownerId
                it[UserWorkspaceBindings.workspaceId] = workspaceId
                it[UserWorkspaceBindings.role] = "TENANT_OWNER"
                it[UserWorkspaceBindings.capabilities] = "[\"ALL\"]"
                it[UserWorkspaceBindings.isActive] = true
                it[UserWorkspaceBindings.createdAt] = Clock.System.now()
                it[UserWorkspaceBindings.updatedAt] = Clock.System.now()
            }

            auditor.logApplicationEvent(
                action = "ORGANIZATION_PRE_REGISTERED",
                entityType = EntityType.TENANT.name,
                entityId = orgId.toString(),
                summary = "Organization pre-registered: ${request.organizationName} ($businessCode)",
                level = TelemetryLevel.INFO
            )

            OrganizationRegistrationResult(
                organizationId = orgId.toString(),
                ownerId = ownerId.toString(),
                ownerPhoneMasked = request.ownerPhone.mask
            )
        }

    /**
     * Public Tenant & Server Resolution method to determine tenant/server environment.
     */
    suspend fun resolveTenantEnvironment(query: String): PublicTenantResolveResult = dbQuery {
        val sanitized = query.trim()
        val userRow = Users.selectAll().where {
            (Users.phone eq sanitized) or
                    (Users.email eq sanitized) or
                    (Users.username eq sanitized)
        }.singleOrNull()

        val orgRow = if (userRow != null) {
            val orgId = userRow[Users.organizationId]
            Organizations.selectAll().where { Organizations.id eq orgId }.singleOrNull()
        } else {
            Organizations.selectAll().where {
                (Organizations.businessCode eq sanitized) or
                        (Organizations.phone eq sanitized) or
                        (Organizations.email eq sanitized)
            }.singleOrNull()
        }

        if (orgRow == null) {
            PublicTenantResolveResult(found = false)
        } else {
            val envName = System.getenv("ENV")?.lowercase() ?: "staging"
            val serverUrl = if (envName == "production" || envName == "prod")
                "https://api.citacomsys.com/"
            else
                "https://api.citacomsys.com/staging/"

            PublicTenantResolveResult(
                found = true,
                organizationName = orgRow[Organizations.name],
                businessCode = orgRow[Organizations.businessCode],
                status = orgRow[Organizations.status],
                serverEnvironment = envName,
                recommendedServerUrl = serverUrl
            )
        }
    }

    /**
     * Fetches complete organization summary details including total recurring amount and workspaces/branches summary.
     */
    suspend fun getOrganizationDetails(orgId: String): OrganizationDetails = dbQuery {
        val uuid = orgId.toUuid()
        val orgRow = Organizations.selectAll()
            .where { Organizations.id eq uuid }
            .singleOrNull() ?: error("Organization $orgId not found")

        val workspaceRows = Workspaces
            .leftJoin(WorkspaceSubscriptionsTable)
            .selectAll()
            .where { Workspaces.organizationId eq uuid }
            .toList()

        var totalMinor = 0L
        val currency = "USD"

        val workspaces = workspaceRows.map { row ->
            val workspaceUuid = row[Workspaces.id].value
            val workspaceIdStr = workspaceUuid.toString()
            val recurringAmountMinor =
                row.getOrNull(WorkspaceSubscriptionsTable.recurringAmountMinor) ?: 0L
            val currencyCode = row.getOrNull(WorkspaceSubscriptionsTable.currencyCode) ?: "USD"
            totalMinor += recurringAmountMinor

            val branchRows = BranchesTable.selectAll()
                .where { BranchesTable.workspaceId eq workspaceUuid }
                .toList()

            val branches = if (branchRows.isEmpty()) {
                listOf(
                    BranchSummary(
                        id = workspaceIdStr,
                        name = row[Workspaces.name] + " (Main)",
                        locationCode = "MAIN",
                        isActive = true
                    )
                )
            } else {
                branchRows.map { bRow ->
                    BranchSummary(
                        id = bRow[BranchesTable.id].value.toString(),
                        name = bRow[BranchesTable.name],
                        locationCode = bRow[BranchesTable.locationCode],
                        isActive = bRow[BranchesTable.isActive]
                    )
                }
            }

            AdminWorkspaceSummary(
                id = workspaceIdStr,
                name = row[Workspaces.name],
                status = row[Workspaces.status],
                recurringAmount = Money(recurringAmountMinor, currencyCode),
                branches = branches,
                createdAt = row[Workspaces.createdAt]
            )
        }

        OrganizationDetails(
            id = orgRow[Organizations.id].value.toString(),
            name = orgRow[Organizations.name],
            businessCode = orgRow[Organizations.businessCode],
            email = orgRow[Organizations.email],
            phone = orgRow[Organizations.phone],
            status = orgRow[Organizations.status].let { OrgStatus.valueOf(it.uppercase()) },
            createdAt = orgRow[Organizations.createdAt],
            totalRecurringAmount = Money(totalMinor, currency),
            workspaces = workspaces
        )
    }

    suspend fun suspendOrganization(orgId: String, reason: String): Result<Unit> = runCatching {
        dbQuery {
            val uuid = orgId.toUuid()
            Organizations.update({ Organizations.id eq uuid }) {
                it[Organizations.status] = OrgStatus.SUSPENDED.name
                it[Organizations.updatedAt] = Clock.System.now()
            }
            Workspaces.update({ Workspaces.organizationId eq uuid }) {
                it[Workspaces.status] = WorkspaceStatus.SUSPENDED
                it[Workspaces.suspendedReason] = reason
                it[Workspaces.updatedAt] = Clock.System.now()
            }

            authorizationContextProvider.invalidateOrganization(orgId)
            sseService.broadcast(
                SseMessage(
                    event = "ORGANIZATION_SUSPENDED",
                    data = orgId,
                    topic = "TENANT_ADMIN",
                    organizationId = orgId
                )
            )

            auditor.logApplicationEvent(
                action = "ORGANIZATION_SUSPENDED",
                entityType = EntityType.TENANT.name,
                entityId = orgId,
                summary = "Organization and child workspaces suspended. Reason: $reason",
                level = TelemetryLevel.WARNING
            )
        }
    }

    suspend fun activateOrganization(orgId: String): Result<Unit> = runCatching {
        dbQuery {
            val uuid = orgId.toUuid()
            Organizations.update({ Organizations.id eq uuid }) {
                it[Organizations.status] = OrgStatus.ACTIVE.name
                it[Organizations.updatedAt] = Clock.System.now()
            }

            Workspaces.update({ Workspaces.organizationId eq uuid }) {
                it[Workspaces.status] = WorkspaceStatus.ACTIVE
                it[Workspaces.suspendedReason] = null
                it[Workspaces.updatedAt] = Clock.System.now()
            }

            authorizationContextProvider.invalidateOrganization(orgId)
            sseService.broadcast(
                SseMessage(
                    event = "ORGANIZATION_ACTIVATED",
                    data = orgId,
                    topic = "TENANT_ADMIN",
                    organizationId = orgId
                )
            )

            auditor.logApplicationEvent(
                action = "ORGANIZATION_ACTIVATED",
                entityType = EntityType.TENANT.name,
                entityId = orgId,
                summary = "Organization and child workspaces activated.",
                level = TelemetryLevel.INFO
            )
        }
    }

    suspend fun getWorkspaceDetails(id: String): WorkspaceDetails = dbQuery {
        val workspace = workspaceRepository.get(id).getOrNull() ?: error("Workspace not found")
        val account = workspaceRepository.getAccount(id)
        val subscription = subscriptionService.getSubscriptionSummary(workspace.id)
        val bindings = deviceRepository.listBindings(workspace.organizationId, workspace.id)
        val branches = branchRepository.getAll(workspace.id)
            .map {
                BranchSummary(
                    id = it.id,
                    name = it.name,
                    locationCode = it.locationCode,
                    isActive = it.isActive
                )
            }

        WorkspaceDetails(
            appName = workspace.name,
            account = account,
            subscription = subscription,
            workstations = bindings,
            branches = branches
        )
    }

    suspend fun suspendWorkspace(id: String, reason: String): Result<Unit> = runCatching {
        val workspace = workspaceRepository.get(id).getOrThrow()
        workspaceRepository.updateStatus(id, WorkspaceStatus.SUSPENDED, reason).getOrThrow()
        authorizationContextProvider.invalidateWorkspace(id)
        sseService.broadcast(
            SseMessage(
                event = "WORKSPACE_SUSPENDED",
                data = id,
                topic = "TENANT_ADMIN",
                organizationId = workspace.organizationId,
                workspaceId = id
            )
        )
        auditor.logApplicationEvent(
            action = "WORKSPACE_SUSPENDED",
            entityType = EntityType.TENANT.name,
            entityId = id,
            summary = "Workspace suspended. Reason: $reason",
            level = TelemetryLevel.WARNING
        )
    }

    suspend fun activateWorkspace(id: String): Result<Unit> = runCatching {
        val workspace = workspaceRepository.get(id).getOrThrow()
        workspaceRepository.updateStatus(id, WorkspaceStatus.ACTIVE).getOrThrow()
        authorizationContextProvider.invalidateWorkspace(id)
        sseService.broadcast(
            SseMessage(
                event = "WORKSPACE_ACTIVATED",
                data = id,
                topic = "TENANT_ADMIN",
                organizationId = workspace.organizationId,
                workspaceId = id
            )
        )
        auditor.logApplicationEvent(
            action = "WORKSPACE_ACTIVATED",
            entityType = EntityType.TENANT.name,
            entityId = id,
            summary = "Workspace activated.",
            level = TelemetryLevel.INFO
        )
    }

    suspend fun deleteWorkspace(id: String): Result<Unit> = runCatching {
        val workspace = workspaceRepository.get(id).getOrThrow()
        workspaceRepository.updateStatus(id, WorkspaceStatus.DELETED).getOrThrow()
        authorizationContextProvider.invalidateWorkspace(id)
        sseService.broadcast(
            SseMessage(
                event = "WORKSPACE_DELETED",
                data = id,
                topic = "TENANT_ADMIN",
                organizationId = workspace.organizationId,
                workspaceId = id
            )
        )
        auditor.logApplicationEvent(
            action = "WORKSPACE_DELETED",
            entityType = EntityType.TENANT.name,
            entityId = id,
            summary = "Workspace marked as deleted (Soft Delete).",
            level = TelemetryLevel.WARNING
        )
    }
}

object BusinessCodeGenerator {

    private val normalizeRegex = Regex("[^a-z0-9]+")

    /**
     * Example:
     * "MediPlus Pharmacy Ltd" -> "MEDIplus-pharmacy-8A9F2B1C"
     */
    fun generate(
        businessName: String,
        suffixLength: Int = 8
    ): String {
        require(businessName.isNotBlank()) {
            "Business ID cannot be blank"
        }

        val slug = businessName
            .trim()
            .lowercase()
            .replace("&", "and")
            .replace(normalizeRegex, "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(32)

        val suffix = randomSuffix(suffixLength)

        return "$slug-$suffix".uppercase()
    }

    private fun randomSuffix(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        return buildString {
            repeat(length) {
                append(chars.random())
            }
        }
    }
}
