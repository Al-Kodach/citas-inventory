package com.citacomsys.feature.workspace.ui.registration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult

@Composable
fun OnboardingSuccessScreen(
    businessName: String,
    productKey: String,
    businessCode: String,
    previewImages: UiResult<List<ImageBitmap?>>,
    onComplete: () -> Unit,
    onSavePdf: () -> Unit,
    onPrint: () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded =
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Header with Actions
            SuccessHeaderActions(onPrint = onPrint, onSavePdf = onSavePdf)

            if (isExpanded) {
                // WIDE VIEW: Side-by-Side
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 48.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WelcomeSection(businessName)
                        Spacer(Modifier.height(24.dp))
                        KeySummaryCard(productKey, businessCode)
                        Spacer(Modifier.height(24.dp))
                        SecurityAwarenessCard()
                        Spacer(Modifier.height(32.dp))
                        CompletionButton(onComplete)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "CONTRACT PREVIEW",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            ContractPreviewList(previewImages)
                        }
                    }
                }
            } else {
                // COMPACT VIEW: Tabbed Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Access Keys & Security") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Contract Preview") }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTab == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                WelcomeSection(businessName)
                                Spacer(Modifier.height(20.dp))
                                KeySummaryCard(productKey, businessCode)
                                Spacer(Modifier.height(20.dp))
                                SecurityAwarenessCard()
                                Spacer(Modifier.height(28.dp))
                                CompletionButton(onComplete)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "CONTRACT PREVIEW",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                ContractPreviewList(previewImages)
                                Spacer(Modifier.height(24.dp))
                                CompletionButton(onComplete)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessHeaderActions(onPrint: () -> Unit, onSavePdf: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrint) {
            Icon(AppIcons.Print, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Print")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onSavePdf) {
            Icon(AppIcons.Download, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save PDF")
        }
    }
}

@Composable
private fun WelcomeSection(businessName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = AppIcons.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Registration Successful!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your business account for $businessName has been successfully established and verified.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeySummaryCard(productKey: String, businessCode: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            KeySummaryItem(
                label = "Product Key",
                value = productKey,
                purpose = "Software licensing, feature entitlements, and secure cloud synchronization across business terminals."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            KeySummaryItem(
                label = "Business Code",
                value = businessCode,
                purpose = "Unique global identifier mapping your enterprise across multi-branch networks and hubs."
            )
        }
    }
}

@Composable
private fun SecurityAwarenessCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Security Notice & Key Safety Awareness",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "• Confidential Storage: Keep your Product Key and Recovery Code confidential and stored securely offline.\n" +
                       "• Zero-Sharing Policy: Never share your Recovery Code with unauthorized third parties. It grants full administrative restoration authority over your workspace.\n" +
                       "• Regular Backups: Print or securely save your activation certificate PDF containing these keys in a secure business vault.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContractPreviewList(previewImages: UiResult<List<ImageBitmap?>>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        UiResultView(state = previewImages) { bitmaps ->
            bitmaps.forEach { bitmap ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .widthIn(max = 500.dp)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Contract Page",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Preview Unavailable", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CompletionButton(onComplete: () -> Unit) {
    Button(
        onClick = onComplete,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text("Get Started to Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeySummaryItem(label: String, value: String, purpose: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(2.dp))
        Text(
            purpose,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
