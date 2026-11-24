package com.citacomsys.core.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.citacomsys.core.notification.CitasNotificationType
import com.citacomsys.core.notification.InAppNotification
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.resources.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun NotificationHost(
    service: InAppNotificationService,
    modifier: Modifier = Modifier
) {
    val notification by service.currentNotification.collectAsState()
    val scope = rememberCoroutineScope()

    // --- AUTO-DISMISS TIMER FOR NON-SYSTEM NOTIFICATIONS ---
    LaunchedEffect(notification?.id) {
        val current = notification ?: return@LaunchedEffect
        if (!current.isSystem) {
            delay(current.durationMillis.milliseconds)
            service.dismiss()
        }
    }

    // --- CRITICAL SYSTEM MODAL (ONLY FOR EXPLICIT SYSTEM ALERTS) ---
    var showModal by remember { mutableStateOf(false) }
    var modalData by remember { mutableStateOf<InAppNotification?>(null) }

    LaunchedEffect(notification?.id) {
        val current = notification
        if (current != null && current.isSystem) {
            modalData = current
            showModal = true
        }
    }

    if (showModal && modalData != null) {
        AlertDialog(
            onDismissRequest = {
                showModal = false
                scope.launch { service.dismiss() }
            },
            title = {
                Text(modalData?.title ?: "System Alert")
            },
            text = { Text(modalData?.message ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    showModal = false
                    scope.launch { service.dismiss() }
                }) {
                    Text("Acknowledge")
                }
            },
            icon = {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }

    // --- NON-INTERRUPTIVE SLIDING TOP BANNER ---
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top)
            .padding(top = 16.dp)
            .zIndex(9999f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notification != null && !(notification?.isSystem ?: false),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            notification?.let {
                NotificationItem(
                    notification = it,
                    onDismiss = { scope.launch { service.dismiss() } }
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: InAppNotification,
    onDismiss: () -> Unit
) {
    val (containerColor, contentColor, icon) = when (notification.type) {
        CitasNotificationType.SUCCESS -> Triple(
            Color(0xFF2E7D32),
            Color.White,
            AppIcons.CheckCircle
        )
        CitasNotificationType.INFO -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            AppIcons.Info
        )
        CitasNotificationType.SYNCING -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            AppIcons.Refresh
        )
        CitasNotificationType.WARNING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            AppIcons.Warning
        )
        CitasNotificationType.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            AppIcons.Error
        )
    }

    Surface(
        modifier = Modifier
            .widthIn(min = 280.dp, max = 560.dp)
            .padding(horizontal = 16.dp)
            .clickable { onDismiss() },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )

            Text(
                text = notification.message,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
