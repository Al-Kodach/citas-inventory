package com.citacomsys.core.ui.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.design.components.AppScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalComplianceScreen(
    onBackClick: () -> Unit
) {
    AppScaffold(
        title = "Legal & Compliance",
        onBack = onBackClick,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Terms of Service & Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last Updated: October 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Part I: Terms of Service
            item {
                LegalSectionCard(
                    title = "Part I: Terms of Service",
                    items = listOf(
                        "1. Acceptance of Terms: By installing, accessing, or using the Citas Industrial Platform (including desktop POS clients, mobile Android applications, and Mother Hub server nodes), you agree to be bound by these Terms of Service.",
                        "2. Software License & Deployment: Citas provides a modular, multi-tenant inventory, sales, and workspace management platform. Workstations require cryptographic hardware attestation and valid organizational licensing (FleetDNA, workspace subscription tiers) to access enterprise data planes.",
                        "3. User Responsibilities & Security: Users and administrators are responsible for maintaining the confidentiality of their login credentials, sudo PINs, and workstation cryptographic keys. The platform must be used solely for lawful commercial inventory, retail, pharmacy, and warehouse operations.",
                        "4. Limitation of Liability: The platform is provided on an 'as-is' and 'as-available' basis. Citas shall not be liable for any indirect, incidental, or consequential damages arising from network interruptions, hardware failures, or uncoordinated local mesh configurations."
                    )
                )
            }

            // Part II: Privacy Policy
            item {
                LegalSectionCard(
                    title = "Part II: Privacy Policy",
                    items = listOf(
                        "1. Information We Collect: Names, emails, phone numbers, organization IDs, and RBAC roles required for tenant user management. Commercial and operational data including inventory stock levels, batch numbers, sales transactions, customer profiles, and ledger entries processed within your authorized workspaces.",
                        "2. How We Use Your Information: To facilitate multi-tenant resource consumption tracking, branch inventory routing, and secure offline synchronization. To enforce zero-trust workstation attestation, biometric verification, and geofencing/network compliance.",
                        "3. Data Storage & Security: Sensitive data and local caches are encrypted using robust cryptographic vaults (KSafe). Data synchronized across local mesh networks (Mother Hub and satellites) remains strictly isolated within your organization's private cryptographic trust boundary.",
                        "4. Your Privacy Rights: You retain full ownership of your commercial data. You may request data export, account suspension, or workspace data purging in compliance with applicable data protection regulations."
                    )
                )
            }
        }
    }
}

@Composable
private fun LegalSectionCard(title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            items.forEach { paragraph ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "•",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
