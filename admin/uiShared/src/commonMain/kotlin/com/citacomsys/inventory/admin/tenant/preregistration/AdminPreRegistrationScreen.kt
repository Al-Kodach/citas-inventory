package com.citacomsys.inventory.admin.tenant.preregistration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.api.organization.model.OrganizationRegistrationResult
import com.design.components.AppScaffold
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.core.ui.result.isSuccess
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminPreRegistrationScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AdminPreRegistrationViewModel = koinViewModel()
) {
    val orgName by viewModel.orgName.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val ownerPhone by viewModel.ownerPhone.collectAsStateWithLifecycle()
    val ownerEmail by viewModel.ownerEmail.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val registrationState by viewModel.registrationState.collectAsStateWithLifecycle()

    AppScaffold(
        title = "Register Organization",
        actions = {},
        onBack = onBack
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (registrationState.isSuccess) {
                    UiResultView(
                        state = registrationState,
                    ) { record ->
                        PreRegistrationSuccessCard(
                            record = record,
                            onNewRegistration = { viewModel.reset() },
                            onDone = {
                                viewModel.reset()
                                onBack?.invoke()
                            },
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            text = "Pre-register a new parent organization and issue initial credentials for the owner. The owner will receive an email notice and can verify upon first login.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = orgName,
                            onValueChange = viewModel::updateOrgName,
                            label = { Text("Organization Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = viewModel::updateOwnerName,
                            label = { Text("Owner Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            OutlinedTextField(
                                value = ownerPhone,
                                onValueChange = viewModel::updateOwnerPhone,
                                label = { Text("Phone Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            OutlinedTextField(
                                value = ownerEmail,
                                onValueChange = viewModel::updateOwnerEmail,
                                label = { Text("Email Address") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text("Owner Username *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        registrationState.getErrorOrNull()?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = viewModel::submit,
                            enabled = !registrationState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Pre-Register Organization", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreRegistrationSuccessCard(
    record: OrganizationRegistrationResult,
    onNewRegistration: () -> Unit,
    onDone: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "Organization Pre-Registered Successfully!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "An email notification has been dispatched to the organization owner with their temporary credentials and onboarding instructions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CredentialRow("Organization ID", record.organizationId)
                CredentialRow("Owner ID", record.ownerId)
                CredentialRow("Masked Phone", record.ownerPhoneMasked)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onNewRegistration,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("New Registration")
                }
                Button(
                    onClick = onDone,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
