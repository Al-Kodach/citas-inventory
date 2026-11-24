package com.citacomsys.inventory.admin.system.presentation.pkg

import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler
import com.citacomsys.inventory.admin.common.ui.AdminBaseDetailsViewModel
import com.citacomsys.types.ActionConfirmationPurpose

/**
 * ViewModel for managing the fine-grained details of a Product Package.
 */
class AdminPackageDetailsViewModel(
    private val api: SystemApiService,
    notificationService: InAppNotificationService,
    sudoAuthHandler: ActorSudoAuthHandler
) : AdminBaseDetailsViewModel<ProductPackageDetails>(
    notificationService,
    sudoAuthHandler
) {
    override suspend fun getDetails(itemId: String): ProductPackageDetails {
        return api.getPackageDetails(itemId)
    }

    /**
     * Saves the modified package configuration.
     * Guarded by Sudo-Auth as this affects pricing and resource limits for tenants.
     */
    fun savePackage(details: ProductPackageDetails) {
        authenticateAndExecute(
            title = "Update Package Configuration",
            purpose = ActionConfirmationPurpose.UPDATE_SECURITY_SETTINGS,
            action = { api.savePackage(details) }
        ) { result: Result<Unit> ->
            result.onSuccess {
                showSuccess("Package updated successfully")
                loadDetails(details.packageInfo.id)
            }.onFailure {
                showError(it.message ?: "Failed to update package")
            }
        }
    }
}
