package com.citacomsys.settings.ui.screen.profile.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.feature.settings.api.UserSecurityApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class UserChangePasswordViewModel(
    private val api: UserSecurityApi
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state = _state.asStateFlow()

    fun onCurrentChange(v: String) = _state.update { it.copy(currentPassword = v) }

    fun onNewChange(v: String) = _state.update { it.copy(newPassword = v) }

    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v) }

    fun submit() {
        if (!isValid()) return

        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            api.changePassword(s.currentPassword, s.newPassword).onSuccess {
                _state.update { it.copy(success = true, loading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(error = error.message, loading = false)
                }
            }
        }
    }

    private fun setError(error: String) {
        _state.update { it.copy(error = error) }
    }

    private fun isValid(): Boolean {
        val state = _state.value
        if (state.currentPassword.isEmpty()) {
            setError("Current password is required")
            return false
        }

        if (state.newPassword.isEmpty()) {
            setError("New password is required")
            return false
        }

        if (state.confirmPassword.isEmpty()) {
            setError("Confirm password is required")
            return false
        }

        if (state.newPassword != state.confirmPassword) {
            setError("Passwords do not match")
            return false
        }

        if (state.newPassword.length < 6) {
            setError("Password must be at least 6 characters long")
            return false
        }

        return true
    }
}
