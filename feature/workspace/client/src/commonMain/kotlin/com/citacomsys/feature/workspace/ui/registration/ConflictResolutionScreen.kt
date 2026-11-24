package com.citacomsys.feature.workspace.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.layout.SectionWrapper
import com.citacomsys.core.ui.result.UiResult

/**
 * Screen displayed when an existing registration is detected during the onboarding process.
 * Provides options to resume the pending registration or enter recovery for active accounts.
 */
@Composable
fun ConflictResolutionScreen(
    result: UiResult<Unit>,
    tenantName: String,
    phoneMasked: String,
    isPending: Boolean,
    onResume: () -> Unit,
    onRecover: () -> Unit,
    onBack: () -> Unit
) {
    SectionWrapper(
        title = "Account Found",
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyListView {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "An existing account was found for $phoneMasked",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tenantName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isPending) "Registration is incomplete" else "Account is active",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Text(
                        text = if (isPending) {
                            "It looks like you started a registration for this pharmacy but didn't finish. You can resume where you left off."
                        } else {
                            "This Account is already registered. If you are the owner and want to activateServices this device, please use the Recovery Mode."
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UiResultView(result)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (isPending) {
                                Button(
                                    onClick = onResume,
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    Icon(AppIcons.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Resume Registration")
                                }
                            }

                            OutlinedButton(
                                onClick = onRecover,
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Icon(AppIcons.Security, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Enter Recovery Mode")
                            }
                        }

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go Back & Use Different Details")
                        }
                    }
                }
            }
        }
    }
}
