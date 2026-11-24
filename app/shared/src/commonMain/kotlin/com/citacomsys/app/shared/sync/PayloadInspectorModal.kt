package com.citacomsys.app.shared.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.shared.JsonFormatter
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PayloadInspectorModal(
    request: PendingRequestEntity,
    onDismiss: () -> Unit
) {
    // Grab native runtime clipboard manager context hook safely
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isCopiedNotificationActive by remember { mutableStateOf(false) }

    // Pre-process the JSON payload text cleanly using the pure Kotlin formatter block
    val prettyPrintedJson = remember(request.payload) {
        request.payload?.let {
            JsonFormatter.format(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payload Inspector: ${request.entityType}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = request.eventAction,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.widthIn(min = 550.dp, max = 800.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Review the raw data string payload below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 📋 CLIPBOARD QUICK COPY BUTTON DOCK
                    TextButton(
                        onClick = {
                            if (request.payload != null) {
                                clipboardManager.setText(
                                    AnnotatedString(request.payload!!)
                                )
                                isCopiedNotificationActive = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isCopiedNotificationActive) Icons.Default.Check else Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isCopiedNotificationActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCopiedNotificationActive) "Copied to Clipboard!" else "Copy Raw JSON")
                    }
                }

                // 💻 High-Contrast Scrollable Terminal Code Container Block
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    color = Color(0xFF1E1E1E),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()) // Dual-axis support for wide arrays
                    ) {
                        Text(
                            text = prettyPrintedJson ?: "",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CDCFE), // Clean terminal code text scheme
                            softWrap = false           // Preserve explicit tabular structure layout lines
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    FlowRow(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tracking Code Row Token Identifier:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = request.id,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Inspector")
            }
        }
    )

    // Automatically clearAll the temporary copy confirmation toast after 2.5 seconds
    LaunchedEffect(isCopiedNotificationActive) {
        if (isCopiedNotificationActive) {
            delay(2500.milliseconds)
            isCopiedNotificationActive = false
        }
    }
}

