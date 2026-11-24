package com.citacomsys.feature.organization.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.layout.AdaptiveRegistrationLayout
import com.design.BrandPurple

@Composable
fun BusinessInfoScreen(
    draft: OrgRegistrationDraft,
    onUpdate: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(draft.organizationName) }

    AdaptiveRegistrationLayout(
        title = "Step 1: Company Profile",
        onBack = onBack,
        icon = AppIcons.Business,
        instructionTitle = "Establish your business entity",
        instructionText = "Every industrial workspace (Pharmacy, Retail, etc.) is anchored to a legal Organization. Enter your registered company id to begin."
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onUpdate(it)
            },
            label = { Text("Organization Name*") },
            placeholder = { Text("e.g., Citacom Systems LTD") },
            leadingIcon = { Icon(AppIcons.Business, null, tint = BrandPurple) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPurple,
                focusedLabelColor = BrandPurple,
                focusedLeadingIconColor = BrandPurple
            )
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            Text("Continue to Administrator Setup", fontWeight = FontWeight.Bold)
        }
    }
}
