package com.citacomsys.feature.auth.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.auth.AuthState
import com.citacomsys.core.splash.SplashScreen
import com.citacomsys.core.ui.SecurityLockoutScreen
import com.citacomsys.core.ui.sudo.OtpVerificationScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthHostScreen(
    onAuthSuccess: () -> Unit,
    goBack: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    Crossfade(
        targetState = authState,
        label = "auth_transition",
        modifier = Modifier.fillMaxSize()
    ) { state ->
        when (state) {
            AuthState.UnAuthenticated -> LoginRoute(
                onForgotPassword = authViewModel::navigateToResetPassword,
                onBack = goBack,
                authViewModel = authViewModel
            )

            AuthState.ResetPassword -> ResetPasswordScreen(
                onBack = authViewModel::navigateToLogin
            )

            is AuthState.OtpVerification -> OtpVerificationScreen(
                handler = authViewModel.otpHandler,
                onBack = authViewModel::navigateToLogin,
                onVerify = authViewModel::otpVerify,
                onResend = authViewModel::login
            )

            AuthState.Locked -> LockScreen(
                user = null,
                canUseBiometric = false,
                onUnlock = {},
                onLogout = authViewModel::logout
            )

            AuthState.Authenticating -> SplashScreen("Authenticating...")

            AuthState.Authenticated -> LaunchedEffect(Unit) { onAuthSuccess() }

            is AuthState.SecurityLockout -> SecurityLockoutScreen(
                title = if (state.isUnregistered) "Unrecognized Device" else "Access Revoked",
                description = if (state.isUnregistered) {
                    "This device has not been registered on the Citas Platform. Please complete the device binding process to continue."
                } else {
                    "Your access from this device has been administratively suspended for security reasons."
                },
                primaryActionLabel = if (state.isUnregistered) "Bind Device" else "Return to Login",
                onPrimaryAction = authViewModel::logout
            )
        }
    }
}


