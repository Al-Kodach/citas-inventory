package com.citacomsys.inventory.admin.appRelease

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AnimatedProgressBar
import com.citacomsys.core.resources.AppIcons
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.koinInject


@Composable
fun AdminReleaseUploadScreen(
    onDismiss: () -> Unit,
    viewModel: AdminReleaseUploadViewModel = koinInject()
) {
    val binaries by viewModel.selectedBinaries.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("msi", "exe", "dmg", "pkg", "apk")),
        onResult = { file ->
            file?.let { viewModel.appendPickedFile(it) }
        }
    )

    val darkConsoleBackground = Color(0xFF171717)
    val tealButtonColor = Color(0xFF2DD4BF)
    val onTealColor = Color(0xFF0F172A)
    val borderColor = Color(0xFF404040)

    Column(
        modifier = Modifier.fillMaxSize()
            .background(darkConsoleBackground)
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Enterprise Deployment Console",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            "Stage new production system binaries and toggle force conditions globally.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(Modifier.height(8.dp))

        // Core Version Details Forms Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.versionInput,
                onValueChange = { viewModel.versionInput = it },
                label = { Text("Build Release Version (e.g. 1.4.3)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = borderColor,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = viewModel.appFlavorInput,
                onValueChange = { viewModel.appFlavorInput = it },
                label = { Text("App Distribution Flavor Track") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = borderColor,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = MaterialTheme.shapes.medium
            )
        }

        OutlinedTextField(
            value = viewModel.changelogInput,
            onValueChange = { viewModel.changelogInput = it },
            label = { Text("Changelog Notes & Deployment Manifest Descriptions") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedBorderColor = borderColor,
                unfocusedLabelColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.medium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = viewModel.isMandatoryByForce,
                onCheckedChange = { viewModel.isMandatoryByForce = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = tealButtonColor,
                    uncheckedColor = borderColor
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Enforce as a Mandatory Security Force-Update Block on Login Screen",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // --- BINARY ASSET SELECTION STRIP ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF262626), shape = MaterialTheme.shapes.large)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Target Distribution Installer Packages",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Button(
                        onClick = { filePickerLauncher.launch() },
                        enabled = !isPublishing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tealButtonColor,
                            contentColor = onTealColor
                        ),
                        modifier = Modifier.height(56.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            "Add\nPackage\nFile",
                            style = MaterialTheme.typography.labelSmall,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (binaries.isEmpty()) {
                    Text(
                        "No installer binaries attached yet. Drop .msi, .dmg, or .apk files here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    binaries.forEach { asset ->
                        Row(
                            Modifier.fillMaxWidth().background(
                                Color(0xFF1E1E1E),
                                shape = MaterialTheme.shapes.medium
                            ).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    asset.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "OS target: ${asset.osTarget} • ${(asset.fileSize / 1024 / 1024)} MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { viewModel.removeAsset(asset) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Item",
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Execution Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                enabled = !isPublishing,
                colors = ButtonDefaults.textButtonColors(contentColor = tealButtonColor)
            ) {
                Text("Abort", fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.width(24.dp))
            
            Button(
                onClick = { viewModel.publishBuildRelease(onSuccessCallback = onDismiss) },
                enabled = !isPublishing && binaries.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF404040),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF262626),
                    disabledContentColor = Color.DarkGray
                ),
                modifier = Modifier.height(48.dp).widthIn(min = 200.dp),
                shape = CircleShape
            ) {
                if (isPublishing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = tealButtonColor,
                            strokeWidth = 2.dp
                        )
                        Text("Publishing Live Update...")
                    }
                } else {
                    Text("Publish Live Update", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (isPublishing) {
            AnimatedProgressBar(uploadProgress)
        }
    }
}
