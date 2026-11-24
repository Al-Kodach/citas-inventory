package com.citacomsys.settings.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.shared.displayName
import com.citacomsys.types.ProductType
import com.citacomsys.types.TenantUserRole
import com.design.components.AppScaffold
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.SettingsGroup
import com.design.components.ContactsView
import io.github.oikvpqya.compose.fastscroller.ScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.ThumbStyle
import io.github.oikvpqya.compose.fastscroller.TrackStyle
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    title: String = "Settings",
    coordinator: NavigationCoordinator = koinInject(),
    sessionManager: AppSessionManager = koinInject(),
    tokenStore: TokenStore = koinInject(),
    onHeaderClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    // Collect pre-filtered, pre-authorized layout elements
    val contributions by coordinator.activePluginSettingsContributions.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    val appUser by sessionManager.appUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    AppScaffold(title = title, onBack = onBack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Direct placement of your Upgraded Identity Header Card
                item(key = "identity_header") {
                    IdentityHeaderCard(
                        app = sessionManager.activeProductType,
                        name = appUser?.name ?: "User Profile",
                        status = appUser?.status?.displayName() ?: "Active",
                        phone = appUser?.phone?.normalized ?: "000-000-0000",
                        email = appUser?.email,
                        role = appUser?.role,
                        onClick = onHeaderClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Loop through all standard groups naturally.
                // The coordinator controls which items actually exist within them!
                SettingsGroup.entries.forEach { group ->
                    val groupContribs = contributions.filter { it.group == group }

                    if (groupContribs.isNotEmpty()) {
                        item(key = "header_${group.name}") {
                            Text(
                                text = group.name.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        groupContribs.forEach { contrib ->
                            item(key = contrib.id) {
                                RenderContribution(contrib)
                            }
                        }
                    }
                }

                item(key = "settings_footer") {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        OutlinedButton(
                            onClick = { scope.launch { tokenStore.clearUserSession() } },
                            modifier = Modifier
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null)
                            Spacer(Modifier.width(12.dp))
                            Text("LOGOUT", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                adapter = rememberScrollbarAdapter(scrollState = state),
                style = ScrollbarStyle(
                    minimalHeight = 52.dp,
                    thickness = 4.dp,
                    hoverDurationMillis = 300,
                    thumbStyle = ThumbStyle(
                        shape = RoundedCornerShape(2.dp),
                        unhoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
                    trackStyle = TrackStyle(
                        shape = RoundedCornerShape(2.dp),
                        unhoverColor = Color.Transparent,
                        hoverColor = Color.Transparent,
                    ),
                ),
                enablePressToScroll = true,
            )
        }
    }
}

@Composable
private fun RenderContribution(contribution: PluginSettingsContribution) {
    when (contribution) {
        is PluginSettingsContribution.CustomWidget -> {
            contribution.content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentityHeaderCard(
    app: ProductType,
    name: String,
    status: String,
    phone: String,
    email: String? = null,
    role: String? = null,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
    },
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Icon and Name Section (Flows Icon on top if not enough space in row)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (onClick != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Contacts Row / Column
            ContactsView(
                phone = phone,
                email = email
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Status and Role Row / Column
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (!role.isNullOrBlank()) {
                    val roleDisplayName = runCatching {
                        TenantUserRole.valueOf(role.split("_").last()).displayName(app)
                    }.getOrDefault(role)

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = roleDisplayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
