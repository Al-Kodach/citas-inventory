package com.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.design.Dimensions

@Composable
fun AdminAppStatusBar(
    modifier: Modifier = Modifier,
    title: String = "App",
    logoPainter: Painter? = null,
    isDev: Boolean = false,
    badgeCount: Int = 0,
    hasCriticalAlert: Boolean = false,
    onNotificationsClick: (() -> Unit)? = null,
    onThemeToggle: (() -> Unit)? = null,
    onFontScaleToggle: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    fontScale: Int = 1,
    systemControls: @Composable RowScope.() -> Unit = {}
) {
    val iconSize = Dimensions.iconSize(fontScale)
    val barVerticalPadding = if (fontScale > 1) 4.dp else 4.dp

    Surface(
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF323639).copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
                .statusBarsPadding()
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = barVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Branding (Optimized for small screens)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp + iconSize)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = (16 + (fontScale - 1) * 2).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isDev) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "DEV",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Right: Controls (Compact & scrollable/wrap-safe for small screens)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (badgeCount > 0 && onNotificationsClick != null) {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier.size(iconSize + 10.dp)
                        ) {
                            BadgedBox(
                                badge = { Badge { Text(badgeCount.toString()) } }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    "Notifications",
                                    tint = if (hasCriticalAlert) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }

                    if (onThemeToggle != null) {
                        IconButton(
                            onClick = onThemeToggle,
                            modifier = Modifier.size(iconSize + 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }

                    if (onFontScaleToggle != null) {
                        IconButton(
                            onClick = onFontScaleToggle,
                            modifier = Modifier.size(iconSize + 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Toggle Font Size",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }

                    systemControls()
                }
            }
        }
    }
}
