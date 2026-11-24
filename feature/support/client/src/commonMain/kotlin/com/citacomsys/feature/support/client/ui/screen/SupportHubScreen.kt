package com.citacomsys.feature.support.client.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.feature.support.api.SupportTicketRecord
import com.citacomsys.feature.support.api.TicketStatus
import com.citacomsys.feature.support.api.TicketType
import com.citacomsys.feature.support.client.ui.SupportViewModel
import com.design.components.AppScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHubScreen(
    viewModel: SupportViewModel,
    onNavigateSubmit: (String) -> Unit,
    onNavigateDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf<TicketType?>(null) }

    val filteredTickets = remember(uiState.tickets, selectedTab) {
        if (selectedTab == null) uiState.tickets else uiState.tickets.filter { it.type == selectedTab }
    }

    AppScaffold(
        title = "Support, Reviews & Issues",
        onBack = onBack,
        actions = {
            IconButton(onClick = { onNavigateSubmit("SUPPORT") }) {
                Icon(Icons.Default.Add, contentDescription = "New Request")
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateSubmit("SUPPORT") },
                icon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                text = { Text("New Ticket / Review") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Category Filter Chips
            SecondaryScrollableTabRow(
                selectedTabIndex = when (selectedTab) {
                    null -> 0
                    TicketType.SUPPORT -> 1
                    TicketType.REVIEW -> 2
                    TicketType.SUGGESTION -> 3
                    TicketType.ISSUE -> 4
                },
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == null,
                    onClick = { selectedTab = null },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == TicketType.SUPPORT,
                    onClick = { selectedTab = TicketType.SUPPORT },
                    icon = {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = { Text("Support") }
                )
                Tab(
                    selected = selectedTab == TicketType.REVIEW,
                    onClick = { selectedTab = TicketType.REVIEW },
                    icon = {
                        Icon(
                            Icons.Default.Feedback,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = { Text("Reviews") }
                )
                Tab(
                    selected = selectedTab == TicketType.SUGGESTION,
                    onClick = { selectedTab = TicketType.SUGGESTION },
                    icon = {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = { Text("Suggestions") }
                )
                Tab(
                    selected = selectedTab == TicketType.ISSUE,
                    onClick = { selectedTab = TicketType.ISSUE },
                    icon = {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    text = { Text("Bugs/Issues") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No records found. Click '+' to send feedback or report an issue.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTickets) { ticket ->
                        TicketCard(ticket = ticket, onClick = { onNavigateDetail(ticket.ticketId) })
                    }
                }
            }
        }
    }
}

@Composable
fun TicketCard(ticket: SupportTicketRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    containerColor = when (ticket.type) {
                        TicketType.SUPPORT -> MaterialTheme.colorScheme.primaryContainer
                        TicketType.REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
                        TicketType.SUGGESTION -> MaterialTheme.colorScheme.secondaryContainer
                        TicketType.ISSUE -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        ticket.type.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                StatusBadge(ticket.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(ticket.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(ticket.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)

            if (ticket.rating != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Rating: ${ticket.rating} / 5 ⭐", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StatusBadge(status: TicketStatus) {
    val container = when (status) {
        TicketStatus.OPEN -> MaterialTheme.colorScheme.surfaceVariant
        TicketStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TicketStatus.RESOLVED -> MaterialTheme.colorScheme.secondary
        TicketStatus.CLOSED -> MaterialTheme.colorScheme.outline
    }
    Surface(
        color = container,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
