package com.citacomsys.user.ui.screen

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.database.dao.UserWorkspaceBindingDao
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.inventory.api.user.model.SuspendUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.types.ProductType
import com.citacomsys.types.User
import com.citacomsys.types.UserWorkspaceBinding
import com.citacomsys.user.data.ClientUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserDetailViewModel(
    notificationService: InAppNotificationService,
    private val userRepository: ClientUserRepository,
    private val bindingDao: UserWorkspaceBindingDao,
    private val sessionManager: AppSessionManager,
) : BaseViewModel(notificationService) {
    val productType: ProductType = sessionManager.activeProductType

    private val _userState = MutableStateFlow<UiResult<User>>(UiResult.Loading)
    val userState = _userState.asStateFlow()

    private val _bindingsState =
        MutableStateFlow<UiResult<List<UserWorkspaceBinding>>>(UiResult.Loading)
    val bindingsState = _bindingsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiResult<Any>>(UiResult.Idle)
    val actionState = _actionState.asStateFlow()

    private var userId: String? = null

    fun loadUser(userId: String) {
        this.userId = userId
        viewModelScope.launch {
            _userState.value = UiResult.Loading
            userRepository.findByIdRemote(userId)
                .onSuccess { user ->
                    _userState.value = UiResult.Success(user)
                }
                .onFailure { err ->
                    _userState.value = UiResult.Error(err.message ?: "User not found on server")
                }
        }
        loadOrRefreshBinding()
    }

    fun suspendUser(userId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            val request = SuspendUserRequest(userId, reason)
            val result = userRepository.suspendUser(request).asUiResult()
            _actionState.value = result
            if (result.isSuccess) {
                showSuccess("User access suspended")
            } else {
                showError(result.getErrorOrNull())
            }
        }
    }

    fun loadOrRefreshBinding() {
        viewModelScope.launch {
            val uid = userId ?: return@launch
            _bindingsState.value = UiResult.Loading
            userRepository.getWorkspaceBindingsForUser(uid)
                .onSuccess { remoteBindings ->
                    bindingDao.saveAll(remoteBindings)
                    _bindingsState.value = UiResult.Success(remoteBindings)
                }
                .onFailure { err ->
                    _bindingsState.value = UiResult.Error(
                        err.message ?: "Failed to fetch workspace bindings from server"
                    )
                }
        }
    }

    fun activateUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            val result = userRepository.activate(userId).asUiResult()
            _actionState.value = result
            if (result.isSuccess) {
                showSuccess("User access restored")
            } else {
                showError(result.getErrorOrNull())
            }
        }
    }

    fun unbindUserFromWorkspace(userId: String, workspaceId: String) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            val request = UnbindUserFromWorkspaceRequest(
                userId = userId,
                workspaceId = workspaceId
            )
            val result = userRepository.unbindUserFromWorkspace(request).asUiResult()
            _actionState.value = result
            if (result.isSuccess) {
                showSuccess("User unbound from workspace successfully")
                loadUser(userId)
            } else {
                showError(result.getErrorOrNull())
            }
        }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            val result = userRepository.delete(userId).asUiResult()

            _actionState.value = result
            if (result.isSuccess) {
                showSuccess("Staff member removed")
                onSuccess()
            } else {
                showError(result.getErrorOrNull())
            }
        }
    }
}
