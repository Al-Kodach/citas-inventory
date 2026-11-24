package com.citacomsys.types

/**
 * Metadata about regulatory compliance supported by a feature.
 */
data class RegulatoryCompliance(
    val id: String,
    val label: String,
    val description: String,
    val isActive: Boolean = true
)

/**
 * Interface for features to contribute to the platform's compliance posture.
 */
interface ComplianceContribution {
    val id: String
    val privacyPolicyAddendum: String? get() = null
    val termsOfServiceAddendum: String? get() = null
    
    /**
     * Set of regulatory standards this service implements.
     * e.g. PCI-DSS, GDPR, HIPAA
     */
    val standards: List<RegulatoryCompliance> get() = emptyList()
}
