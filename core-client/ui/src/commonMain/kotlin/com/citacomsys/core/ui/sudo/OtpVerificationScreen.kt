package com.citacomsys.core.ui.sudo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.security.sudo.OtpHandler
import com.design.AppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A generic, embeddable OTP verification component.
 * Configuration (title, description, identifier) and status (timer, isLoading, errors)
 * are managed by the [OtpHandler].
 */
@Composable
fun OtpVerificationScreen(
    handler: OtpHandler,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onUseRecoveryCode: (() -> Unit)? = null,
    verifyButtonText: String = "Verify & Continue",
    emergencyButtonText: String = "Use Recovery Code",
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showResend: Boolean = true
) {
    val isLoading by handler.isLoading.collectAsStateWithLifecycle()
    val errorMessage by handler.error.collectAsStateWithLifecycle()
    val countdown by handler.resendCountdown.collectAsStateWithLifecycle()
    val state by handler.stateFlow.collectAsStateWithLifecycle()
    var otpCode by remember { mutableStateOf("") }

    OtpVerificationContent(
        title = state.title,
        description = state.description,
        identifierMasked = state.identifierMasked,
        otpLength = state.otpLength,
        allowEmergencyRecovery = state.allowEmergencyRecovery,
        isLoading = isLoading,
        errorMessage = errorMessage,
        countdown = countdown,
        formattedTimer = handler.formattedTimer,
        otpCode = otpCode,
        onCodeChanged = {
            otpCode = it
            handler.onCodeChanged(it)
        },
        startResendTimer = { handler.startResendTimer() },
        onVerify = onVerify,
        onResend = onResend,
        onBack = onBack,
        onUseRecoveryCode = onUseRecoveryCode,
        verifyButtonText = verifyButtonText,
        emergencyButtonText = emergencyButtonText,
        modifier = modifier,
        showIcon = showIcon,
        showResend = showResend
    )
}

@Composable
fun OtpVerificationContent(
    title: String,
    description: String,
    identifierMasked: String,
    otpLength: Int,
    allowEmergencyRecovery: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    countdown: Int,
    formattedTimer: String,
    otpCode: String,
    onCodeChanged: (String) -> Unit,
    startResendTimer: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onUseRecoveryCode: (() -> Unit)? = null,
    verifyButtonText: String = "Verify & Continue",
    emergencyButtonText: String = "Use Recovery Code",
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showResend: Boolean = true
) {

    // Reset local state when length changes (e.g. switching to emergency mode)
    LaunchedEffect(otpLength) {
        onCodeChanged("")
    }

    LaunchedEffect(otpCode) {
        if (otpCode.length == otpLength) {
            onVerify(otpCode)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .wrapContentSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(AppIcons.ArrowBack, contentDescription = "Back")
                }
            }

            if (showIcon) {
                Icon(
                    imageVector = AppIcons.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(32.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (identifierMasked.isNotBlank()) {
                Text(
                    text = identifierMasked,
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
                    if (it.length <= otpLength) {
                        onCodeChanged(it)
                    }
                },
                otpLength = otpLength,
                enabled = !isLoading,
            )

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { onVerify(otpCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = otpCode.length == otpLength && !isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(verifyButtonText, fontWeight = FontWeight.Bold)
                }
            }

            if (showResend) {
                Spacer(Modifier.height(24.dp))

                if (countdown > 0) {
                    Text(
                        text = "Resend code in $formattedTimer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    TextButton(
                        onClick = {
                            onCodeChanged("")
                            onResend()
                            startResendTimer()
                        },
                        enabled = !isLoading
                    ) {
                        Text("Didn't receive a code? Resend")
                    }
                }
            }

            if (allowEmergencyRecovery && onUseRecoveryCode != null) {
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (showResend) "Can't access this number?" else "Use different method?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onUseRecoveryCode,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(AppIcons.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(emergencyButtonText)
                    }
                }
            }
        }
    }
}


@Composable
private fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    otpLength: Int,
    enabled: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled) {
        if (enabled) {
            delay(100.milliseconds)
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            showKeyboardOnFocus = true
        ),
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
//                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Preview(
    showBackground = true,
)
@Composable
private fun OtpVerificationScreenPreview() {
    var otpCode by remember { mutableStateOf("012345") }

    AppTheme(isDarkTheme = true) {
        OtpVerificationContent(
            title = "Verify Ownership",
            description = "Enter the verification code sent to",
            identifierMasked = "+233 24 **** 890",
            otpLength = 6,
            allowEmergencyRecovery = true,
            isLoading = false,
            errorMessage = null,
            countdown = 45,
            formattedTimer = "0:45",
            otpCode = otpCode,
            onCodeChanged = {
                otpCode = it
            },
            startResendTimer = {},
            onVerify = {},
            onResend = {},
            onBack = {},
            onUseRecoveryCode = {}
        )
    }
}

