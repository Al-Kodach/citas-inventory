# Citacom Systems Inventory SaaS Platform

A multi-workspace, offline-capable SaaS platform built on Kotlin Multiplatform (KMP). Featuring a robust security architecture, high-fidelity telemetry, and domain-specific extensions (Pharmacy, Retail, etc.) built on a resilient synchronization kernel.

## 🚀 Platform Architecture

### 1. Multi-Tenant Secure Isolation
- **Tenant & Branch Logic**: Strict data isolation using UUID-based workspace partitioning and localized branch management.
- **Hardware-Isolated Encryption**: Sensitive data (Customer PII) is stored in a dedicated `CustomerDatabase` encrypted at rest using 256-bit hardware-isolated keys via **`ksafe`**.
- **Privacy-First (PII Masking)**: Automated database-level masking for sensitive fields (e.g., `•••• ••• 4821`) ensuring PII never touches the UI layer.

### 2. Resilient Offline-First Kernel
- **Push-then-Pull Sync**: Mutations are queued locally and replayed with **Urgency Scoring**, ensuring critical business data (Sales, Billing) is synchronized first.
- **Circuit Breaking**: Automated failure protection that "trips" the sync pipe for specific modules under unstable network conditions to conserve bandwidth and battery.
- **Decentralized Storage**: Specialized databases for different concerns:
    - `inventory.db`: Business logic and catalog.
    - `telemetry.db`: Forensics and diagnostic auditing.
    - `customer.db`: Encrypted PII and CRM.

### 3. Unified Telemetry Hub
- **High-Fidelity Auditing**: Every sensitive operation (Auth, Config Change, Sync) generates a cryptographically verified track in the hardware ledger.
- **Forensic Visibility**: Color-coded diagnostic screens provide real-time oversight of local and remote execution health.

## 🏛️ Platform Principles

The Citacom Platform is designed around a core set of architectural principles:

- **Offline-First**: Features must operate without network connectivity whenever feasible.
- **Server Authoritative**: The cloud remains the source of truth; clients maintain synchronized operational snapshots.
- **Immutable Snapshots**: Snapshot tables represent synchronized state and are never modified directly by local business logic.
- **Security by Default**: Hardware encryption, least-privilege access, and auditability are baked into every feature.
- **Privacy by Design**: Data minimization, classification, and workspace-defined retention policies are strictly enforced.
- **Modular Extension**: Product Apps (Pharmacy, Retail, etc.) extend the shared platform through well-defined extension points.
- **Governance-First Engineering**: Every feature documents its architecture, data lifecycle, and compliance before release.

---

## 📂 Project Structure

- **`:core-client`**: Shared KMP modules for database management, resilient network clients, and high-fidelity UI components.
- **`:core-server`**: High-performance Ktor server providing multi-workspace persistence and paged synchronization endpoints.
- **`:adminApp`**: Global Platform Console for managing tenants, releases, and platform-wide security policies.
- **`:pharmacyApp`**: Specialized POS and clinical inventory terminal with offline-first health record capabilities.
- **`:shared`**: Common data types, security enums, and event-bus definitions used across all layers.

---

## 🛡️ Feature Governance Standard (v1.0)

Every feature introduced into the platform is treated as a complete platform service. Before a feature is considered production-ready, it must include its own documentation covering:

### 1. Data Governance & Classification
Every entity handled by a feature must be classified:

| Entity | Classification | Notes |
| :--- | :--- | :--- |
| **Customer** | Confidential | Personally Identifiable Information (PII) |
| **Receipt Pref** | Internal | Operational settings |
| **Product Snapshot** | Internal | Operational data |
| **Stock Snapshot** | Internal | Inventory state |

### 2. Implementation Requirements
- **Architecture**: Domain model, database entities, safe views, and sync flow.
- **Security**: Authorization model, capability enforcement, and secret handling.
- **Privacy**: Consent management, retention policies, and export/deletion support.
- **Synchronization**: Queue ordering, parent-child dependencies, and failure recovery.
- **Performance**: Query optimization, pagination, and latency targets.
- **Observability**: Telemetry events, metrics, and health check scenarios.

### 3. Definition of Done
A feature is production-ready only when:
- [x] **Functional**: Business requirements implemented and verified.
- [x] **Secure**: Hardware encryption and capability checks verified.
- [x] **Private**: Data classification and masking logic applied.
- [x] **Auditable**: Telemetry events and audit logging implemented.
- [x] **Tested**: Functional, security, and performance test cases passing.
- [x] **Documented**: Feature-specific README, APIs, and Schemas finalized.

---

## 🛠️ Setup & Development

### Build Prerequisites
- **JDK 17+**
- **Android Studio 2026.1.2+**
- **SQLDelight 2.0+**

### Building the Project
```bash
./gradlew build
```

### Running Applications
- **Admin Desktop**: `./gradlew admindesktop:run`
- **Pharmacy Desktop**: `./gradlew :pharmacyApp:desktop:run`

---

> [!NOTE]
> Citacom Platform is more than code—it is a governed ecosystem. This documentation ensures security, compliance, and maintainability across all SaaS fleet deployments.
