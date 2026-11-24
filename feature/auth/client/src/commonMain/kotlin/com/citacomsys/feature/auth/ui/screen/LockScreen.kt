package com.citacomsys.feature.auth.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.types.User

@Composable
internal fun LockScreen(
    user: User?,
    canUseBiometric: Boolean,
    onUnlock: () -> Unit,
    onLogout: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // User Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            if (user != null) {
                Text(
                    text = "user.displayName.take(1).uppercase()",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = AppIcons.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = user?.name ?: "Locked screen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "BootstrapWorkspaceResponse is locked for security",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        if (canUseBiometric) {
            Button(
                onClick = onUnlock,
                modifier = Modifier.height(56.dp).widthIn(min = 200.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(AppIcons.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Unlock with Biometric")
            }
        } else {
            Button(
                onClick = onLogout,
                modifier = Modifier.height(56.dp).widthIn(min = 200.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Return to UnAuthenticated")
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onLogout) {
            Text("Log out and switch user", color = MaterialTheme.colorScheme.error)
        }
    }
}
