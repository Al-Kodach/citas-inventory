package com.citacomsys.inventory.admin.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResultButton
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.sudo.OtpVerificationScreen

@Composable
fun AdminLoginScreen(
    viewModel: AdminAuthViewModel,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val passwordFocusRequester = remember { FocusRequester() }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (state.performOtp) {
            OtpVerificationScreen(
                handler = viewModel.otpHandler,
                onVerify = { code ->
                    viewModel.verifyMfa(code)
                },
                onResend = {
                    viewModel.login()
                },
                onBack = {
                    viewModel.cancelOtp()
                },
                verifyButtonText = "Verify & Bind Device"
            )
        } else {
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .padding(16.dp)
                    .animateContentSize(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Identity, username or phone
                    OutlinedTextField(
                        value = state.identity,
                        onValueChange = { viewModel.identityChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone/Username/email") },
                        leadingIcon = { Icon(AppIcons.Call, null) },
                        singleLine = true,
                        enabled = !state.isLoading,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Security Section
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.passwordChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        label = { Text("Password") },
                        leadingIcon = { Icon(AppIcons.Lock, null) },
                        singleLine = true,
                        enabled = !state.isLoading,
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (state.canLogin) {
                                    focusManager.clearFocus()
                                    viewModel.login()
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    UiResultView(
                        state = state.result,
                        errorView = {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.result.getErrorOrNull() ?: "unknown error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    UiResultButton(
                        result = state.result,
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = state.canLogin,
                        text = "Login"
                    )
                }
            }
        }
    }
}
