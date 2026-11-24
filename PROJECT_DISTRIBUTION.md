# Citas Inventory SaaS & Industrial Mesh — App Distribution Architecture

This document outlines the current architectural state and future direction of application distribution, deployment modes, hardware integration, and commercial licensing for the **Citas Inventory SaaS Platform**.

---

## 1. Executive Summary

Citas Inventory SaaS is a multi-workspace, offline-capable enterprise inventory and point-of-sale (POS) platform built on **Kotlin Multiplatform (KMP)**. It supports flexible distribution across **Android (Mobile/Tablet)**, **Desktop (Windows/macOS/Linux JVM)**, **Web**, and **Cloud/On-Premise Server (Ktor)**.

---

## 2. Core Distribution & Deployment Modes

The platform supports two distinct **Deployment Modes** (`DeploymentMode`), allowing businesses to choose how their data and infrastructure are governed:

### A. Cloud Synced SaaS (`CLOUD_SYNCED`)
* **Target**: Standard retail shops, multi-branch businesses, and cloud-first operations.
* **Architecture**: Clients synchronize in real-time or via background sync engines directly with the Citas Cloud server.
* **Public Registration Policy**: **Workspace registration and tenant onboarding are not exposed to the public internet for now.** Due to limited infrastructure resources and team bandwidth, self-service public registration is restricted. All tenant provisioning, activation, and workspace licensing are managed through controlled administrative workflows.
* **Billing Cycles**: Flexible monthly (`MONTHLY`) and annual (`YEARLY`) subscription cycles.

### B. Local Offline Edge Mesh (`OFFLINE_LOCAL`)
* **Target**: Industrial warehouses, remote depots, and retail stores with intermittent or zero internet connectivity.
* **Architecture**: Uses the **Citas Industrial Mesh** protocol (Mother Hub & Satellite nodes). One local workstation acts as the secure encrypted local "Mother Hub" (Netty TLS server), while other local POS terminals ("Satellites") sync securely over local Wi-Fi/LAN.
* **Network Boundary**: Operates entirely within secure local area networks (LAN) / private intranets without public internet exposure.
* **Security**: Tenant-isolated PKCS12 keystores (`mesh-hub-$orgId-tls.p12`), SHA-256 TLS fingerprint pinning (`MeshHubTrustManager`), and cryptographic workstation authentication (`HubNodeAuthenticationPlugin`).
* **Billing Cycles**: Capped strictly to **annual (`YEARLY`) commitments** to align with enterprise on-premise licensing and reduce operational support churn.

---

## 3. Product Packages & Pricing Matrix

Managed through `ProductPackage` and `PackageBillingTerm`:
* **Lite Edition (`LITE`)**: Essential inventory for single-user offline or local shops.
* **Starter Edition (`STARTER`)**: Flexible cloud-synced or local mesh edge for small teams.
* **Professional (`PRO`)**: Advanced multi-device suite supporting high-performance local Industrial Mesh & Cloud sync.
* **Enterprise (`ENTERPRISE`)**: Custom multi-branch organization solutions with complete local edge mesh and cloud hybrid support.

---

## 4. Hardware Integration: Barcode Scanning & Thermal Printing

The platform features native hardware peripherals support via dedicated KMP client modules (`core-client:barcode` and `:device-service`):

1. **Barcode & QR Scanning**:
   - **Mobile/Tablet**: Camera-based real-time scanning powered by **KScan** and ML Kit (`ScannerScreen`).
   - **Desktop (PC/POS Terminals)**: Zero-driver **Keyboard Wedge (HID)** support where handheld USB/Bluetooth barcode guns type directly into focused input fields, instantly triggering database index lookups.
   - **Model**: Lightweight `ScannedBarcode` model (`rawValue`, `format`, `timestamp`).

2. **FEFO (First-Expired, First-Out) Batch Pricing**:
   - POS checkout automatically queries active stock batches sorted by expiry date ascending (`ORDER BY expiry_date ASC`), ensuring older stock is sold first and deriving prices from the soonest-expiring batch.

3. **Thermal Label & Receipt Printing**:
   - Integrated with `:device-service` (`DeviceService.print(...)` and `generateContentPdf(...)`).
   - Generates QR/Barcode labels via `generateQrBitmap(...)` (powered by **qrose** and ZXing) and formats product metadata into thermal print jobs for receipt and barcode label printers.

---

## 5. Future Strategic Direction

1. **Autonomous Edge Mesh Failover**:
   - Enhancing satellite nodes to seamlessly transition between local Mother Hub mesh and central cloud replication with zero data loss.
2. **Advanced Hardware Peripherals**:
   - Expanding direct USB/Bluetooth serial communication for electronic cash drawers, weighing scales, and industrial thermal label printers (Zebra/EPSON ESC/POS).
3. **Enterprise SIEM & Cross-Branch Audit Logging**:
   - Deepening automated compliance auditing, telemetry tracing (Perfetto), and remote workstation attestation.
