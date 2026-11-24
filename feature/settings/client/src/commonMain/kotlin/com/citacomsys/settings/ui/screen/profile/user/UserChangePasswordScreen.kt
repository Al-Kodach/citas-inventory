@file:OptIn(ExperimentalMaterial3Api::class)

package com.citacomsys.settings.ui.screen.profile.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.TextField
import com.citacomsys.core.components.SaveButton
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.management.layout.ModalScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UserChangePasswordScreen(
    viewModel: UserChangePasswordViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) {
            onBack()
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onBack
    )

    ModalScaffold(
        title = "Change Password",
        onBack = onBack
    ) {
        Column(
            Modifier.fillMaxHeight()
                .padding(24.dp)
                .widthIn(max = 840.dp)
        ) {
            TextField(
                value = state.currentPassword,
                onValueChange = viewModel::onCurrentChange,
                label = "Current Password",
                isPassword = true
            )

            TextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewChange,
                label = "New Password",
                isPassword = true
            )

            TextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label = "Confirm Password",
                isPassword = true,
            )

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(64.dp))

            SaveButton(
                onClick = viewModel::submit,
                isLoading = state.loading,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
