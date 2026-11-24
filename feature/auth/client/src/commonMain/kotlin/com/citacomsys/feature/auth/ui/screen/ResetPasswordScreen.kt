package com.citacomsys.feature.auth.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.UiActionButton
import com.citacomsys.core.resources.AppIcons
import org.koin.compose.koinInject

/**
 * A high-contrast, professional Reset Password screen.
 * Supports identifier input (Phone or Email) and a multi-phase OTP verification with dynamic length based on channel.
 */
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = koinInject()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val otpState by viewModel.otpHandler.stateFlow.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(16.dp)
                .animateContentSize(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onBack()
                            viewModel.clearState()
                        }
                    ) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1.3f))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    item {
                        // High contrast icon with branded primary color
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        AnimatedContent(
                            targetState = state.phase,
                            label = "phase-transition"
                        ) { phase ->
                            when (phase) {
                                ResetPasswordUiState.ResetPhase.IDENTIFIER -> {
                                    IdentifierPhase(
                                        identifier = state.identifier,
                                        onIdentifierChange = viewModel::identifierChange,
                                        onRequestReset = viewModel::requestReset,
                                        isLoading = state.isLoading,
                                        error = state.error
                                    )
                                }

                                ResetPasswordUiState.ResetPhase.OTP_AND_PASSWORD -> {
                                    OtpPhase(
                                        viewModel = viewModel,
                                        otp = state.otp,
                                        otpLength = otpState.otpLength,
                                        identifierMasked = otpState.identifierMasked,
                                        newPassword = state.newPassword,
                                        confirmPassword = state.confirmPassword,
                                        onOtpChange = viewModel::otpChange,
                                        onNewPasswordChange = viewModel::newPasswordChange,
                                        onConfirmPasswordChange = viewModel::confirmPasswordChange,
                                        onSubmitReset = {
                                            viewModel.submitReset {
                                                viewModel.clearState()
                                                onBack()
                                            }
                                        },
                                        isLoading = state.isLoading,
                                        isSuccess = state.isSuccess,
                                        error = state.error,
                                        onReturnToLogin = {
                                            viewModel.clearState()
                                            onBack()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentifierPhase(
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    onRequestReset: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please enter your registered phone number or email address. We'll send you a secure OTP to verify your identity.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            label = { Text("Phone or Email") },
            placeholder = { Text("e.g. +233XXXXXXXXX or user@company.com") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        Spacer(Modifier.height(28.dp))

        UiActionButton(
            text = "Request Verification Code",
            isLoading = isLoading,
            error = error,
            enabled = identifier.isNotBlank(),
            onClick = onRequestReset
        )
    }
}

@Composable
private fun OtpPhase(
    viewModel: ResetPasswordViewModel,
    otp: String,
    otpLength: Int,
    identifierMasked: String,
    newPassword: String,
    confirmPassword: String,
    onOtpChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmitReset: () -> Unit,
    isLoading: Boolean,
    isSuccess: Boolean,
    error: String?,
    onReturnToLogin: () -> Unit
) {
    val passwordMatch = newPassword == confirmPassword && newPassword.isNotBlank()
    val passwordPolicyMet = newPassword.length >= 8
    val countdown by viewModel.otpHandler.resendCountdown.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSuccess) {
            SuccessView(onReturnToLogin)
        } else {
            Text(
                text = "Enter the $otpLength-digit verification code sent to resetWorkspaceSession your password",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (identifierMasked.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = identifierMasked,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // Custom Multi-Box Digit OTP Input
            DigitOtpBoxes(
                value = otp,
                onValueChange = { if (it.length <= otpLength) onOtpChange(it) },
                otpLength = otpLength,
                enabled = !isLoading
            )

            // Resend Countdown & Request Trigger
            Spacer(Modifier.height(12.dp))
            if (countdown > 0) {
                Text(
                    text = "Resend code in ${viewModel.otpHandler.formattedTimer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                TextButton(
                    onClick = {
                        viewModel.otpChange("")
                        viewModel.requestReset()
                    },
                    enabled = !isLoading
                ) {
                    Text("Didn't receive a code? Resend Code", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(20.dp))

            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("New Secure Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                supportingText = {
                    Text("Minimum 8 characters for security")
                },
                isError = newPassword.isNotEmpty() && !passwordPolicyMet
            )

            Spacer(Modifier.height(16.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && !passwordMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !passwordMatch) {
                        Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                    } else if (passwordMatch) {
                        Text("Passwords match", color = Color(0xFF4CAF50))
                    }
                },
                trailingIcon = {
                    if (passwordMatch) {
                        Icon(AppIcons.Check, "Match", tint = Color(0xFF4CAF50))
                    }
                }
            )

            Spacer(Modifier.height(28.dp))

            // Action Button
            UiActionButton(
                text = "Update Password & Sign In",
                isLoading = isLoading,
                error = error,
                enabled = otp.length == otpLength && passwordPolicyMet && passwordMatch,
                onClick = onSubmitReset
            )
        }
    }
}

@Composable
private fun DigitOtpBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    otpLength: Int,
    enabled: Boolean
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = enabled,
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(otpLength) { index ->
                    val char = when {
                        index >= value.length -> ""
                        else -> value[index].toString()
                    }

                    val isFocused = value.length == index

                    Box(
                        modifier = Modifier
                            .size(if (otpLength > 5) 42.dp else 48.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = if (otpLength > 5) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SuccessView(
    onReturnToLogin: () -> Unit
) {
    Text(
        text = "Verification Complete",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4CAF50)
    )
    Text(
        text = "Your password has been updated. You can now access your workspace with your new credentials.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = 16.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = onReturnToLogin,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text("Return to Login", style = MaterialTheme.typography.titleMedium)
    }
}
