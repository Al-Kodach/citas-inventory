# Citas Industrial Platform — Authentication & Zero-Trust Security Feature

## 1. Overview
The **Authentication & Security Feature** (`feature/auth`) is the core security perimeter of the Citas Industrial Platform. It implements an industrial-grade **Zero-Trust Tokenless Edge Architecture**, combining hardware-bound cryptographic workstation attestation, flexible multi-factor authentication (MFA/2FA), and hybrid offline-first Sudo session management.

---

## 2. Authentication Flow & Tokenless Workstation Security

### A. Hardware Attestation & Signatures (`WorkstationSignaturePlugin`)
- **Device-Bound RSA Keypairs**: Every workstation generates a secure cryptographic keypair stored in hardware-backed secure storage (`KSafe` / Android Keystore / Java KeyStore).
- **Request Signing**: Outgoing API requests and offline synchronization payloads are signed on the client using the workstation private key (`X-Workstation-Signature`, `X-Hardware-Id`, `X-Workstation-Puk`).
- **Anti-Replay Protection**: Timestamps (`X-Timestamp`) are validated against a strict 5-minute validity window on the server (`HubNodeAuthenticationPlugin`).
- **Cryptographic Fingerprinting**: Device build fingerprints are hashed (`X-Hardware-Fingerprint`) before transmission to protect against plaintext sniffing while ensuring strict hardware cloning defense.

---

## 3. Multi-Factor Authentication (MFA) & 2FA (`LoginService` & `MfaServiceImpl`)

### A. Dual-Channel OTP Dispatch
- **Hubtel Official OTP API**: For phone numbers / SMS, authentication challenges are dispatched via Hubtel's official OTP gateway (`https://api-otp.hubtel.com/otp/send`) ensuring carrier-grade delivery in Ghana.
- **Transactional Email OTP (Resend)**: For mandatory owner accounts and email-based verification, OTP codes are securely dispatched via Resend (`EmailGateway`).
- **Non-Blocking Background Execution**: OTP dispatches and notification queues run asynchronously in the background (`applicationScope.launch`), ensuring API login responses remain instantaneous.

### B. Enterprise MFA Policy Enforcement
- **Strict MFA Enforcement**: When a user has MFA enabled (`user.mfaEnabled == true`), 2FA challenges are strictly enforced. 
- **Recognized Device Auditing**: Trusted device recognition (`isDeviceRecognized`) and fingerprint mismatch detection (`hasFingerprintMismatch`) are logged immutably via `TelemetryAuditor` (`MFA_BYPASSED_RECOGNIZED_DEVICE`, `HARDWARE_FINGERPRINT_MISMATCH_CHALLENGE`).

---

## 4. JWT Access & Refresh Token Architecture (`UserSessionServiceImpl`)

### A. Token Issuance & Lifecycle
- **Short-Lived Access Tokens**: Issued securely upon successful authentication and MFA verification, carrying user claims, roles, scopes, and active workspace bindings.
- **Secure Refresh Token Rotation**: Long-lived refresh tokens stored securely with cryptographic SHA-256 hashing (`HashUtils.sha256`) and family tracking (`familyId`).
- **Replay & Tamper Detection**: If a revoked refresh token is re-used or tampered with, the system automatically revokes the entire token family (`refreshTokenRepository.revokeFamily`) and terminates associated user sessions to prevent token theft propagation.

---

## 5. Sudo Mode & Hybrid Session Management (`SudoManager`)

### A. Split Enforcement Architecture
1. **`requestTenantAuthentication(...)` (Offline-First)**:
   - Designed for standard tenant operators. Operates offline-first, leveraging local biometric verification (`KSafeBiometrics.verifyBiometricDirect`) with encrypted local password/PIN fallback for high operational resilience when internet connectivity is optional.
2. **`requestPlatformAdminAuthentication(...)` (Strictly Online)**:
   - Designed for platform administrators and critical activities (e.g., destructive resource deletions, billing management, global device lockouts). **Strictly forces online connection**, disallowing offline execution and delegating directly to server-side **Action Confirmation Tokens (ACT)** and server MFA challenges.

---

## 6. Security Auditing & Compliance Telemetry
Every authentication event, MFA challenge, OTP dispatch failure, token refresh violation, and fingerprint mismatch is immutably recorded through `TelemetryAuditor` (`logIdentityAccessEvent`), providing ISO-aligned audit trails for enterprise security compliance.
