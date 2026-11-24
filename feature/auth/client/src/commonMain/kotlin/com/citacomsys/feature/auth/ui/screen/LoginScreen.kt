package com.citacomsys.feature.auth.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.security.EnrolledUser
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val loginState by authViewModel.loginFormState.collectAsStateWithLifecycle()

    LoginScreen(
        state = loginState,
        onIdentityChanged = authViewModel::identityChanged,
        onPasswordChange = authViewModel::passwordChanged,
        onForgotPassword = onForgotPassword,
        onBack = onBack,
        onLogin = { authViewModel.login() },
        onSelectUser = authViewModel::selectUser
    )
}

@Composable
internal fun LoginScreen(
    state: LoginState,
    onIdentityChanged: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onSelectUser: (EnrolledUser?) -> Unit,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    // Independent UI state for switching between list and input
    var forceManualInput by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
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
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProjectLogos.CitasProjectLogo(size = 56.dp, showWordmark = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (state.selectedUser != null) "Welcome Back" else "Sign In",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1.3f)) // Offset for the back button
                }

                // Identity Section
                AnimatedContent(
                    targetState = Pair(state.selectedUser, forceManualInput),
                    transitionSpec = {
                        (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                    },
                    label = "IdentitySection"
                ) { (user, manual) ->
                    when {
                        user != null -> {
                            SelectedUserPreview(user = user, onDeselect = { onSelectUser(null) })
                        }

                        state.enrolledUsers.isNotEmpty() && !manual -> {
                            EnrolledUsersList(
                                users = state.enrolledUsers,
                                onSelect = { onSelectUser(it) },
                                onAddNew = { forceManualInput = true }
                            )
                        }

                        else -> {
                            IdentityInput(
                                value = state.identity,
                                onValueChange = onIdentityChanged,
                                isLoading = state.isLoading,
                                focusRequester = passwordFocusRequester,
                                onBackToList = if (state.enrolledUsers.isNotEmpty()) {
                                    { forceManualInput = false }
                                } else null
                            )
                        }
                    }
                }

                // Security Section
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester),
                    label = { Text(if (state.selectedUser != null) "Enter PIN or Password" else "Password") },
                    leadingIcon = { Icon(AppIcons.Lock, null) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (state.identity.isNotEmpty() && state.password.isNotEmpty()) {
                                focusManager.clearFocus()
                                onLogin()
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

                if (state.selectedUser == null) {
                    TextButton(onClick = onForgotPassword, enabled = !state.isLoading) {
                        Text("Forgot Password?")
                    }
                }

                if (state.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onLogin()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.isLoading && (state.identity.isNotBlank() || state.selectedUser != null) && state.password.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        val label =
                            if (state.selectedUser != null) "UNLOCK WORKSPACE" else "SIGN IN"
                        Text(label, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedUserPreview(user: EnrolledUser, onDeselect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                AppIcons.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = user.identity,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onDeselect) {
            Text("Switch Account", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IdentityInput(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onBackToList: (() -> Unit)?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("username/email/phone") },
        leadingIcon = { Icon(AppIcons.Call, null) },
        trailingIcon = onBackToList?.let {
            {
                IconButton(onClick = it) {
                    Icon(AppIcons.Close, "Back to list")
                }
            }
        },
        singleLine = true,
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusRequester.requestFocus() }
        )
    )
}

@Composable
private fun EnrolledUsersList(
    users: List<EnrolledUser>,
    onSelect: (EnrolledUser) -> Unit,
    onAddNew: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Select Account",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        users.forEach { user ->
            Surface(
                onClick = { onSelect(user) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.Person,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        user.identity,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        AppIcons.ChevronRight,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onAddNew,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(AppIcons.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Another Account")
        }
    }
}
