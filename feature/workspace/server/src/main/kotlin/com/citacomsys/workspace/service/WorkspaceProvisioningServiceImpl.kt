package com.citacomsys.workspace.service

import com.citacomsys.api.billing.service.BillingService
import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.remote.PackageProvisioningService
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.api.workspace.model.WorkspaceAccount
import com.citacomsys.api.workspace.model.WorkspaceProvisioningResult
import com.citacomsys.api.workspace.model.WorkspaceRegistrationRequest
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.api.workspace.service.BranchService
import com.citacomsys.api.workspace.service.WorkspaceProvisioningService
import com.citacomsys.api.workspace.service.WorkspaceSubscriptionService
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.kernel.model.WorkspaceResource
import com.citacomsys.kernel.model.WorkspaceResourcesUsage
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.EntityType
import com.citacomsys.types.Money
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ProductType
import com.citacomsys.types.WorkspaceStatus
import kotlin.random.Random
import kotlin.time.Clock

internal class WorkspaceProvisioningServiceImpl(
    private val workspaceRepository: WorkspaceRepository,
    private val organizationRepository: OrganizationRepository,
    private val resourceUsageRepository: WorkspaceResourceUsageRepository,
    private val auditor: TelemetryAuditor,
    private val passwordHasher: PasswordHasher,
    private val provisioningService: PackageProvisioningService,
    private val workspaceSubscriptionService: WorkspaceSubscriptionService,
    private val billingService: BillingService,
    private val branchService: BranchService,
    private val userRepo: UserRepository,
    private val notificationService: NotificationService,
    private val clock: Clock = Clock.System,
) : WorkspaceProvisioningService {

    override suspend fun provisionWorkspace(
        organizationId: String,
        request: WorkspaceRegistrationRequest,
        actorId: String
    ): WorkspaceProvisioningResult {
        val org = organizationRepository.get(organizationId).getOrThrow()
        val actor = userRepo.findById(actorId).getOrThrow()

        val pkgProvisionRequest = PkgProvisionRequest(
            packageId = request.pkg.packageId,
            billTermId = request.pkg.billTermId
        )

        val duplicate =
            workspaceRepository.findDuplicate(organizationId, request.type, request.name)
        if (duplicate != null) {
            throw IllegalArgumentException("A workspace with the ID '${request.name}' already exists in this organization.")
        }

        val provisioned = provisioningService.provision(pkgProvisionRequest).getOrThrow()

        val workspaceResource = WorkspaceResource(
            packageId = provisioned.packageId,
            name = provisioned.packageName,
            tier = provisioned.tier,
            modules = provisioned.modules,
            maxUsers = provisioned.maxUsers,
            maxDevice = provisioned.maxDevices,
            maxBranches = provisioned.maxBranches,
            maxProductsLimit = provisioned.maxProductsLimit,
            maxActiveBatchesLimit = provisioned.maxActiveBatchesLimit,
            historyRetentionMonths = provisioned.historyRetentionMonths,
            updatedAt = clock.now()
        )

        val productKey = generateProductKey()
        val businessCode = generateBusinessCode(org.businessCode, request.name, request.type)

        val workspace = workspaceRepository.create(
            organizationId = organizationId,
            name = request.name,
            packageTier = provisioned.tier,
            businessType = request.type,
            productKey = productKey,
            deploymentMode = provisioned.deploymentMode,
            businessCode = businessCode,
            address = request.address,
            email = request.email,
            phone = request.phone,
            status = WorkspaceStatus.ACTIVE
        ).getOrThrow()

        val workspaceId = workspace.id

        resourceUsageRepository.saveWorkspaceResourceAllocation(workspaceId, workspaceResource)
        workspaceSubscriptionService.activateTrial(workspaceId, provisioned)

        workspaceRepository.saveAccount(
            workspaceId,
            WorkspaceAccount(
                workspaceId = workspaceId,
                organizationId = organizationId,
                balance = Money.zero(provisioned.calculatedPrice.currencyCode),
                paymentProviderCustomerId = null,
                defaultPaymentMethodId = null,
                updatedAt = clock.now()
            )
        )

        request.branding?.let {
            workspaceRepository.saveBrand(workspaceId, it)
        }

        resourceUsageRepository.saveResourceUsage(
            workspaceId,
            WorkspaceResourcesUsage(
                productCountUsed = 0,
                activeBatchesCount = 0,
                userCount = 0, // Owner account does not consume workspace resource slots
                deviceCount = 0,
                branchCount = 1,
                updatedAt = clock.now()
            )
        )

        branchService.save(
            workspaceId = workspaceId,
            branch = Branch(
                workspaceId = workspaceId,
                name = request.mainBranchName,
                locationCode = "MAIN-1",
                contactPhone = request.phone?.normalized,
                email = request.email,
                isActive = true
            )
        )

        auditor.logApplicationEvent(
            action = "WORKSPACE_CREATED",
            entityType = EntityType.TENANT.name,
            entityId = workspaceId,
            summary = "Workspace ${workspace.productKey} provisioned successfully for ${org.name}",
            level = TelemetryLevel.INFO
        )

        EventBus.publish(SubscriptionEvent(wsId = organizationId, organizationId = organizationId))

        val email = org.email ?: actor.email
        val phone = org.phone ?: actor.phone.normalized
        // Send workspace creation notification to owner

        notificationService.sendWorkspaceProvision(
            email = email,
            phone = PhoneNumber(phone),
            businessName = request.name,
            businessCode = org.businessCode,
            productKey = productKey,
            recoveryCode = ""
        )

        return WorkspaceProvisioningResult(
            workspaceId = workspaceId,
            productKey = productKey,
            businessCode = org.businessCode
        )
    }

    override suspend fun previewInvoice(pkg: PkgProvisionRequest): InvoicePreviewUiState {
        return billingService.invoicePreviewRequest(
            PreviewRequest.Subscription(
                pkg.packageId,
                pkg.billTermId
            )
        )
    }

    private fun generateProductKey(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..4).joinToString("-") {
            (1..4).map { chars[Random.nextInt(chars.length)] }
                .joinToString("")
        }
    }

    private fun generateBusinessCode(
        orgBusinessCode: String,
        workspaceName: String,
        type: ProductType
    ): String {
        val cleanName = workspaceName.filter { it.isLetterOrDigit() }.take(4).uppercase()
        val suffix = Random.nextInt(100, 999)
        return "${orgBusinessCode}_${type.name.take(3)}_${cleanName}$suffix"
    }
}
