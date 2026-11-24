package com.citacomsys.feature.organization.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.layout.AdaptiveRegistrationLayout
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.shared.validation.Validators
import com.citacomsys.shared.validation.validate
import com.design.BrandPurple


@Composable
fun OwnerInfoScreen(
    draft: OrgRegistrationDraft,
    uiState: UiResult<OrganizationRegistrationResult>,
    onUpdate: (name: String, phone: String, email: String, username: String, password: String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(draft.ownerName) }
    var phone by remember { mutableStateOf(draft.ownerPhone) }
    var password by remember { mutableStateOf(draft.ownerPassword) }
    var confirmPassword by remember { mutableStateOf(draft.ownerPassword) }
    var email by remember { mutableStateOf(draft.ownerEmail) }
    var username by remember { mutableStateOf(draft.username) }

    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val passwordPolicyMet =
        validate(password, Validators.required(), Validators.minLength(8)).isValid
    val passwordsMatch = password == confirmPassword && password.isNotEmpty()
    val emailValid = validate(email, Validators.required(), Validators.email()).isValid

    AdaptiveRegistrationLayout(
        title = "Step 2: Security Anchor",
        onBack = onBack,
        icon = AppIcons.Shield,
        instructionTitle = "Create the Primary Owner",
        instructionText = "This account will hold ultimate administrative control over the Organization. For security, we recommend using the owner's legal details and a strong password."
    ) {
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPurple,
            focusedLabelColor = BrandPurple,
            focusedLeadingIconColor = BrandPurple
        )
        val textFieldShape = RoundedCornerShape(8.dp)

        // Full Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onUpdate(it, phone, email, username, password) },
            label = { Text("Full Legal Name*") },
            leadingIcon = { Icon(AppIcons.Person, null, tint = BrandPurple) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = textFieldShape,
            colors = textFieldColors
        )

        // Phone
        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    phone = it
                    onUpdate(name, it, email, username, password)
                }
            },
            prefix = { Text("+233") },
            label = { Text("Mobile Phone*") },
            leadingIcon = { Icon(AppIcons.Call, null, tint = BrandPurple) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            shape = textFieldShape,
            colors = textFieldColors
        )

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onUpdate(name, phone, it, username, password) },
            label = { Text("Business Email*") },
            leadingIcon = { Icon(AppIcons.Email, null, tint = BrandPurple) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = textFieldShape,
            colors = textFieldColors
        )

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; onUpdate(name, phone, email, it, password) },
            label = { Text("username*") },
            leadingIcon = { Icon(AppIcons.AccountCircle, null, tint = BrandPurple) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            shape = textFieldShape,
            colors = textFieldColors
        )

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onUpdate(name, phone, email, username, it) },
            label = { Text("Create Password*") },
            leadingIcon = { Icon(AppIcons.Lock, null, tint = BrandPurple) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            supportingText = { Text("Minimum 8 characters") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            shape = textFieldShape,
            colors = textFieldColors
        )

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password*") },
            leadingIcon = { Icon(AppIcons.LockReset, null, tint = BrandPurple) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                if (passwordsMatch) {
                    Icon(AppIcons.Check, null, tint = Color(0xFF2E7D32))
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            shape = textFieldShape,
            colors = textFieldColors
        )

        if (!uiState.isLoading) {
            UiResultView(uiState)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = name.isNotBlank() && phone.isNotBlank() && emailValid && passwordPolicyMet && passwordsMatch,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("Submit Information", fontWeight = FontWeight.Bold)
        }
    }
}
