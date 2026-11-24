package com.citacomsys.core.domain.service

import androidx.compose.ui.graphics.vector.ImageVector
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.ProductType
import com.citacomsys.types.User
import com.citacomsys.types.UserWorkspaceBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class WorkStationInfo(
    val appName: String = "Citas App",
    val username: String = "",
    val deviceName: String = "",
    val orgName: String = "",

    val icon: ImageVector? = null,
    val isOrgMode: Boolean = false,

    val activeWorkspaceName: String = "",
    val activeBranchName: String = "",
    val activeProductType: ProductType = ProductType.INVENTORY,

    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val website: String? = null,
    val taxId: String? = null
) {
    val logo = if (isOrgMode) extractLogo(orgName) else extractLogo(activeWorkspaceName)
}

private fun extractLogo(name: String): String {
    val cleanName = name.replace(Regex("[\\-/_+.,:=]+"), " ")
    val words = cleanName.trim().split("\\s+".toRegex())
        .map { word -> word.filter { it.isLetterOrDigit() } }
        .filter { it.isNotBlank() }

    val letterWords = words.filter { word -> word.any { it.isLetter() } }

    return when {
        letterWords.size >= 2 -> {
            (letterWords[0].first { it.isLetter() }
                .toString() + letterWords[1].first { it.isLetter() }.toString()).uppercase()
        }

        letterWords.size == 1 -> {
            val lettersOnly = letterWords[0].filter { it.isLetter() }
            lettersOnly.take(2).uppercase()
        }

        words.isNotEmpty() -> {
            words[0].take(2).uppercase()
        }

        else -> ""
    }
}

interface AppSessionManager : WorkspaceSessionManager {
    val appUser: StateFlow<User?>
    val currentUserId: String?
    val actorName: String?

    val userWorkspaceBindings: Flow<List<UserWorkspaceBinding>>

    val organizationId: String?

    val workStationInfo: StateFlow<WorkStationInfo>

    /**
     * Lock the device to this workspace only
     */
    val isWorkspaceLocked: StateFlow<Boolean>

    suspend fun setAppUser(user: User?)

    /**
     * Check if the user is authorized to access the branch or workspace
     */
    suspend fun isAuthorized(workspaceId: String, branchId: String): String?
    suspend fun setWorkspaceBindings(bindings: List<UserWorkspaceBinding>)

    suspend fun setWorkspaceLocked(locked: Boolean, lockWsId: String)

    suspend fun recordAccessedBranch(workspaceId: String, branchId: String)
    fun getAccessedBranches(): Set<String>

    suspend fun getUser(): User?

    fun resolveWriteBranchId(): String

    /**
     *  Active workspace deployment mode check
     */
    fun isSubscribed(): Boolean

    /**
     * Relay or Active workspace deployment mode check
     */
    fun isSubscribed(wsId: String): Boolean

    fun isCloudDeployed(wsId: String): Boolean
}
