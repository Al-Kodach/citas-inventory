# Citas Industrial Platform — Security Profile & Compliance Architecture

## 1. Executive Summary
The **Citas Industrial Platform** is designed for mission-critical retail, pharmacy, and warehouse operations where local resilience, high-speed multi-branch synchronization, and uncompromising data security are mandatory. This document profiles the platform's security architecture, zero-trust edge protocols, cryptographic verification standards, and regulatory compliance alignment.

---

## 2. Zero-Trust Architecture & Edge Security

### A. Tokenless Hardware Identity Proofing (`WorkstationSignaturePlugin`)
- **Asymmetric Workstation Signatures**: Every outgoing HTTP request and offline outbox sync request originating from a workstation terminal is cryptographically signed using a persistent, device-bound RSA keypair (`X-Workstation-Signature`, `X-Hardware-Id`, `X-Workstation-Puk`).
- **Anti-Replay Window**: Signatures include a high-precision timestamp (`X-Timestamp`) evaluated against a strict 5-minute synchronization window to prevent replay attacks.
- **Hardware Fingerprint Binding**: Workstations enforce physical hardware fingerprint validation (`X-Hardware-Fingerprint`) to defend against database or key cloning onto unauthorized devices.

### B. Action Confirmation Tokens (ACT) & Sudo Isolation
- **Privilege Interception**: High-risk operations (such as price overrides, inventory stock adjustments, refund finalization, and user capability modifications) require real-time verification via Action Confirmation Tokens and biometric/sudo authorization before mutation execution.

---

## 3. Multi-Tenant Isolation & RBAC Scoping

### A. Organization Scoping & Workspace Binding (`user_workspace_bindings`)
- **Clean Base Accounts**: User accounts are strictly scoped to parent organizations. Workspace-specific resource consumption and role overrides are managed via the `user_workspace_bindings` relational table.
- **Dynamic Capability Resolution**: `TenantUserCapabilityManager` reacts to active workspace changes and workspace bindings, enforcing least-privilege capability boundaries (`UserCapability`) dynamically.

### B. Zero-Trust Outbox & Sync Isolation (`DefaultEntitySyncEngine`)
- **Strict Scope Enforcement**: Synchronization policies (`SyncConfig`) define precise scopes (`ORGANIZATION_WIDE`, `WORKSPACE_SCOPED`, `OFFLINE_LOCAL_ONLY`).
- **Fail-Safe Security Blocks**: Unassigned workspace scopes without explicit delegated workspace identifiers automatically halt synchronization cycles, preventing unauthorized cross-tenant data leakage.

---

## 4. Local Mesh Network & mTLS Security (`MeshLanTls`)

### A. Edge Mesh Encryption
- **Certificate Pinning**: Local Mother Hub gateway communications over LAN are secured via strict mTLS and SHA-256 certificate fingerprint pinning (`MeshHubTrustManager`, `MeshTlsIdentityStore`).
- **Cryptographic Discovery Tokens**: Satellite nodes authenticate against local Mother Hub gateways using organization-bound HMAC discovery tokens (`X-Fleet-Auth`, `MeshCryptoManager`), ensuring rogue LAN devices cannot inject spurious telemetry or data mutations.

---

## 5. Regulatory Compliance & Auditability

### A. Compliance Standards Alignment
- **Data Privacy & GDPR / Local Data Protection Acts**:
  - **Data Minimization & Encryption**: PII (Customer records, ledger transactions, and audit trails) is encrypted at rest using AES/XChaCha20 encryption vaults (`KSafe`) and SQLite database encryption.
  - **Right to Erasure & Auditability**: System events (`system_events` trigger capture) and immutable local audit logs (`ClientLocalAuditLog.sq`) maintain complete lifecycle traceability.
- **ISO-Aligned Audit Trails**:
  - All inventory restocks, price changes, sales finalizations, and security assertions record precise actor IDs (`X-Actor-Id`), timestamps, and workspace bindings.
  - Telemetry logs are captured locally and synced securely via background daemons (`DefaultTelemetryBackgroundScheduler` / `KmpTelemetryLogClient`).

---

## 6. Incident Response & Resiliency
- **Circuit Breaker Protection**: Continuous monitoring detects downstream throttling or transient transport failures, tripping circuit breakers per entity type to prevent cascading server overloads.
- **Dead-Letter Quarantine**: Poison pill payloads (4xx client/validation errors) are quarantined instantly into dead-letter audit tables with diagnostic reason logs, enabling manual administrative review and resolution.
