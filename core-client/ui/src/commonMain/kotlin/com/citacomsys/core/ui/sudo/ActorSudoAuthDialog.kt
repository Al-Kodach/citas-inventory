package com.citacomsys.core.ui.sudo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler

/**
 * Shared UI component for handling Sudo Authentication challenges (Password/OTP).
 * Observed the state flows from [ActorSudoAuthHandler] to notify appropriate prompts.
 */
@Composable
fun ActorSudoAuthDialogs(
    sudoManager: ActorSudoAuthHandler
) {
    val uiState by sudoManager.uiState.collectAsStateWithLifecycle()

    val otpHandler = sudoManager.otpHandler

    val otpState by otpHandler.stateFlow.collectAsStateWithLifecycle()
    val countdown by otpHandler.resendCountdown.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    if (uiState.showPasswordPrompt) {
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoading) sudoManager.dismissPrompts()
            },
            title = { Text("Identity Verification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Biometric authentication failed or is unavailable. Please enter your password to authorize this sensitive operation.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Account Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )

                    AnimatedVisibility(uiState.errorMessage != null) {
                        uiState.errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sudoManager.passwordConfirmed(password)
                    },
                    enabled = password.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Authorize")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { sudoManager.dismissPrompts() },
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showOtpPrompt) {
        var otpCode by remember { mutableStateOf("") }

        LaunchedEffect(uiState.otpLength) {
            otpCode = ""
            otpHandler.onCodeChanged("")
        }

        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoading) sudoManager.dismissPrompts()
            },
            title = { Text(otpState.title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (uiState.showIcon) {
                        Icon(
                            imageVector = AppIcons.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(32.dp))
                    }

//                    Text(
//                        text = otpState.title,
//                        style = MaterialTheme.typography.headlineSmall,
//                        fontWeight = FontWeight.Bold,
//                        textAlign = TextAlign.Center
//                    )
//
//                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = otpState.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (otpState.identifierMasked.isNotBlank()) {
                        Text(
                            text = otpState.identifierMasked,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(48.dp))

                    OtpInputField(
                        value = otpCode,
                        onValueChange = {
                            if (it.length <= uiState.otpLength) {
                                otpCode = it
                                otpHandler.onCodeChanged(it)
                            }
                        },
                        otpLength = uiState.otpLength,
                        enabled = !uiState.isLoading
                    )

                    AnimatedVisibility(uiState.errorMessage != null) {
                        uiState.errorMessage?.let {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (uiState.showResend) {
                        Spacer(Modifier.height(24.dp))
                        if (countdown > 0) {
                            Text(
                                text = "Resend code in ${otpHandler.formattedTimer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    otpHandler.onCodeChanged("")
//                                    sudoManager.onResendOtpAuthRequest()
                                    otpHandler.startResendTimer()
                                },
                                enabled = !uiState.isLoading
                            ) {
                                Text("Didn't receive a code? Resend")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { sudoManager.otpConfirm(otpCode) },
                    enabled = otpCode.length == uiState.otpLength && !uiState.isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(otpState.verifyButtonText, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { sudoManager.dismissPrompts() },
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    otpLength: Int,
    enabled: Boolean
) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.all { it.isDigit() }) {
                onValueChange(it)
            }
        },
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
                            .size(if (otpLength > 6) 28.dp else 48.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = if (otpLength > 6) MaterialTheme.typography.titleMedium
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
