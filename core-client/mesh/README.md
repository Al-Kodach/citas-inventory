# Citas Industrial Mesh

The Citas Industrial Mesh is a high-performance, secure, and offline-capable data plane that enables a local "Mother Hub" to coordinate a fleet of satellite workstations without requiring a persistent internet connection or complex SSL/TLS certificates.

## 🏗 Architecture: Hub-Satellite Model

The mesh operates on a transparent gateway pattern:
1.  **Mother Hub**: An authorized workstation (usually a high-performance desktop or server) that acts as a local proxy for the Citas Cloud.
2.  **Satellite Nodes**: Specialized industrial terminals (Pharmacy POS, Retail checkouts) that discover and communicate with the Mother Hub over the LAN.

### Key Components
- **`LocalMeshRelayPlugin`**: A Ktor Client plugin that intercepts outbound cloud requests and redirects them to the local Hub if one is discovered.
- **`HubRouting`**: Configures the Hub's internal Ktor server to handle satellite requests, perform local business logic, and manage fleet-wide synchronization.
- **`HubServiceGateway`**: An interface implemented by modules (Inventory, Sales) to provide specialized local processing for satellite requests.

## 🔐 Security Protocols: The Industrial Guard

To ensure absolute confidentiality and organizational isolation without the overhead of local TLS management, the mesh implements a multi-layered security model.

### 1. Symmetric LAN Encryption (Industrial Guard)
Every industrial data payload sent over the local network is encrypted using the **Organization Secret** or the **FleetDNA Secret**.
- **FleetDNA**: A derived transit key used for highly sensitive routes (Auth, Users, Customers).
- **Push Security**: All `POST/PUT` bodies (mutations) are encrypted by satellites.
- **Pull Security**: All `GET/sync` responses (delta updates) are encrypted by the Hub.

### 2. Cryptographic Node Authentication
Every satellite request is cryptographically signed by the originating hardware's private key.
- **Hardware Binding**: Satellites include their Public Key (`X-Workstation-Puk`) and a Hashed Signature (`X-Workstation-Signature`).
- **Hub Verification**: The `HubNodeAuthenticationPlugin` verifies the signature against the Hub's local **Workstation Trust Registry**.
- **Anti-Replay**: Requests include a strictly enforced 5-minute timestamp window.

### 3. Discovery Trust Protocol
Before a connection is ever attempted, nodes verify each other's organizational membership during the mDNS discovery phase.
- **Discovery Token**: Every node broadcasts a cryptographic token (`orgVerificationToken`) derived from the **Organization Secret**.
- **Mutual Verification**: The `MeshManager` on both Hub and Satellite nodes filters discovered peers. If a peer's token cannot be decrypted/verified using the local secret, it is discarded from the peer list.
- **Benefits**: Prevents unauthorized devices on the same LAN from even appearing as potential nodes, adding a preliminary layer of cloaking to the mesh.

### 4. Identity & Workspace Lineage
The mesh enforces strict organizational boundaries using specialized headers:
- **`X-Mesh-Encrypted: true`**: Signifies that the payload requires local decryption.
- **`X-Mesh-Sensitive: true`**: Triggers the use of the FleetDNA secret for decryption.
- **`X-Workspace-Id`**: Injected by satellites and enforced by the Hub to prevent cross-tenant data leakage.
- **`X-Mesh-Source: SATELLITE`**: Tells the Hub to treat the response as part of the local data plane and apply encryption.

### 3. Zero-Payload Sync Queue
When a Hub processes a satellite request, it does not store the raw data blob in its synchronization queue.
- **Pointer Pattern**: The Hub enqueues an **ID Pointer** and the **Workspace Context**.
- **Local Truth Replay**: During background replay to the Citas Cloud, the Hub pulls the most recent state directly from its local database. This ensures the cloud always receives the "Absolute Local Truth" while minimizing the Hub's storage footprint.

## 📡 Operational Flow

### Discovery (mDNS)
Nodes automatically discover the Mother Hub via Multicast DNS. The Hub broadcasts its LAN IP and specialized industrial capabilities (e.g., `PHARMACY`, `RETAIL`).

### Real-Time Signaling (SSE)
The Hub maintains a lightweight **Server-Sent Events (SSE)** stream. Whenever a state change occurs (e.g., stock is sold on Node A), the Hub "Pokes" all connected satellites to trigger an immediate delta sync, maintaining fleet-wide consistency in milliseconds.

### Bandwidth Optimization
By keeping high-frequency industrial traffic (POS sales, stock adjustments) on the LAN, the mesh reduces cloud bandwidth costs by up to 90% and ensures that industrial operations never stall during ISP outages.
