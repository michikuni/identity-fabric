# TrustID — Self-Sovereign Identity Platform

> **Định vị**: Nền tảng SSI (Self-Sovereign Identity) cho workplace credentials, xây dựng trên Hyperledger Fabric.
> HRMS (chấm công, hợp đồng, lương) là **use-case minh họa** vai trò Issuer — không phải sản phẩm chính.

---

## Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────┐
│               TrustID Platform                      │
│                                                     │
│  Flutter Mobile App          Verifier Portal        │
│  (identity_frontend/)        (verifier-portal/)     │
│         │                          │                │
│         └──────────┬───────────────┘                │
│                    ▼                                │
│        Spring Boot Backend                          │
│        (fabric-spring-backend/)                     │
│                    │                                │
│         ┌──────────┴──────────┐                     │
│         ▼                     ▼                     │
│     MySQL DB          Hyperledger Fabric            │
│     (JPA/Hibernate)   (fabric-network/)             │
└─────────────────────────────────────────────────────┘
```

### Thành phần

| Thư mục | Công nghệ | Vai trò |
|---|---|---|
| `identity_frontend/` | Flutter 3.x | Mobile app (Employee / Manager / Chief / Admin) |
| `verifier-portal/` | Vite 5 + React 18 + TypeScript + Tailwind | Standalone Verifier Portal (không cần tài khoản) |
| `fabric-spring-backend/` | Kotlin + Spring Boot 3 | REST API, VC issuance, SD-JWT, Status List, MFA, GDPR |
| `fabric-network/` | Hyperledger Fabric 2.x (Java chaincode) | Ledger bất biến — DID, VC records, Trust Registry, Contract Signatures |

---

## Tính năng chính

### Phase 0 — SSI-first Navigation (✅ Done)
- Bottom nav SSI-first theo từng role: **Wallet · Verifier · Workplace · Profile**
- Tab **Workplace** gom toàn bộ HRMS use-cases (Attendance, Requests, Payroll…)
- **Issuer Console** (Admin Dashboard) với 2 section KPI: SSI KPIs + Operations KPIs

### Phase 1 — W3C Credential Stack (✅ Done)

| Feature | Mô tả |
|---|---|
| **Status List 2021** (4.1) | Badge ACTIVE / REVOKED cho mỗi VC trong Wallet; tự động check khi Verifier verify |
| **SD-JWT Selective Disclosure** (4.2) | Skill & Education credentials: holder chọn field nào tiết lộ |
| **Verifier Portal** (4.3) | SPA độc lập, paste VC / SD-JWT → verify ngay, không cần login |
| **DIF Universal Resolver** (4.4) | `GET /1.0/identifiers/{did}` trả DID Document chuẩn W3C |
| **Biometric Unlock** (4.5) | ECDSA P-256 signing gate bằng fingerprint / Face ID |
| **Trust Registry on-chain** (4.6) | Danh sách trusted issuers lưu trên Fabric, Verifier Portal hiển thị |
| **E-sign Contract** (4.7) | Ký hợp đồng bằng wallet key, anchor chữ ký lên Fabric |

### Phase 2 — Security & Compliance (✅ Done)

| Feature | Mô tả |
|---|---|
| **TOTP 2FA** (5.1) | Setup QR → Google Authenticator → backup codes |
| **Audit Log on-chain** (5.2) | Timeline lịch sử thay đổi record từng employee, lọc theo loại |
| **TrainingVC + NDA-AcceptedVC** (5.3) | 2 loại VC mới cho training và NDA |
| **Rate Limiting + Account Lockout** (5.4) | Bucket4j 10 req/min, lock 15 phút sau 5 lần sai |
| **GDPR Export + Erasure** (5.5) | Art.20 Data Export, Art.17 Right to be Forgotten |
| **Device Binding & Session List** (5.6) | Track active devices, logout từng device hoặc tất cả |

---

## Cài đặt & Chạy

### Yêu cầu

- Java 17+, Gradle 8+
- Flutter 3.19+
- Node.js 20+ (cho verifier-portal)
- Docker + Docker Compose (cho Hyperledger Fabric)
- MySQL 8

### 1. Khởi động Hyperledger Fabric

```powershell
cd fabric-network
.\start.ps1          # Hoặc: docker-compose up -d
```

### 2. Backend Spring Boot

```powershell
cd fabric-spring-backend

# Cấu hình application.properties:
#   SD_JWT_SECRET=your-secret
#   spring.datasource.url=jdbc:mysql://localhost:3306/trustid

.\gradlew bootRun
# Chạy tại http://localhost:8080
```

**Biến môi trường quan trọng:**

```properties
vc.issuer-did=did:fabric:trustid:org1
vc.status-list.id=employment-status-list-1
vc.status-list.size=131072
vc.status-list.base-url=http://localhost:8080/api/v1/status-list
sd-jwt.secret=${SD_JWT_SECRET:sd-jwt-secret-trustid-org1-2026}
```

### 3. Flutter Mobile App

```powershell
cd identity_frontend
flutter pub get
flutter run
```

Đổi `baseUrl` trong [api_constants.dart](identity_frontend/lib/core/network/api_constants.dart) sang IP backend nếu chạy trên thiết bị thật.

### 4. Verifier Portal

```powershell
cd verifier-portal
npm install
npm run dev     # http://localhost:5173
npm run build   # → dist/
```

Dev proxy tự forward `/api` và `/1.0` → `http://localhost:8080`.

---

## API Reference (tóm tắt)

### Authentication
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/auth/sign-in` | — | Đăng nhập, trả JWT |
| POST | `/api/v1/auth/sign-up` | — | Đăng ký tài khoản |
| POST | `/api/v1/mfa/validate` | — | Validate TOTP second factor |

### VC & Identity
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/status-list/{listId}` | — | Status List 2021 VC |
| GET | `/api/v1/status-list/{listId}/entry?index=N` | — | Check single entry |
| POST | `/api/v1/identity/vc/verify` | — | Verify W3C VC (+ auto Status List check) |
| GET | `/1.0/identifiers/{did}` | — | DIF Universal Resolver |
| GET | `/api/v1/trust-registry/issuers` | — | Danh sách trusted issuers |

### SD-JWT
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/sd-jwt/issue/skill/{employeeId}` | ADMIN/CHIEF | Issue Skill SD-JWT |
| POST | `/api/v1/sd-jwt/issue/education/{employeeId}` | ADMIN/CHIEF | Issue Education SD-JWT |
| GET | `/api/v1/sd-jwt/{employeeId}/skill` | — | Sync Skill SD-JWT to mobile |
| POST | `/api/v1/sd-jwt/present` | — | Build selective presentation |
| POST | `/api/v1/sd-jwt/verify` | — | Verify SD-JWT presentation |

### Admin / Issuer
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/admin/issuer-stats` | ADMIN/CHIEF | SSI KPI dashboard |
| POST | `/api/v1/admin/employees/{id}/issue-training-vc` | ADMIN/CHIEF | Issue TrainingVC |
| POST | `/api/v1/contracts/{id}/sign` | Authenticated | Anchor e-signature on Fabric |
| GET | `/api/v1/audit/employees/{id}/history` | ADMIN/CHIEF | On-chain audit log |

### Security & Privacy
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/mfa/setup` | ADMIN/CHIEF | Generate TOTP QR |
| POST | `/api/v1/mfa/verify-setup` | ADMIN/CHIEF | Confirm code → enable MFA |
| GET | `/api/v1/sessions` | Authenticated | Active device sessions |
| DELETE | `/api/v1/sessions/{deviceId}` | Authenticated | Logout device |
| GET | `/api/v1/me/export-data` | Authenticated | GDPR Art.20 data export |
| DELETE | `/api/v1/me/data` | Authenticated | GDPR Art.17 erasure |

---

## Cấu trúc Flutter App

```
identity_frontend/lib/
├── core/
│   ├── network/
│   │   ├── api_client.dart            # Dio client + JWT interceptor
│   │   └── api_constants.dart         # Tất cả endpoint paths
│   ├── routes/
│   │   └── app_router.dart            # GoRouter — SSI-first navigation
│   ├── security/
│   │   └── biometric_service.dart     # local_auth wrapper
│   ├── storage/
│   │   └── secure_storage.dart        # flutter_secure_storage
│   └── wallet/
│       ├── sd_jwt_holder.dart         # Parse SD-JWT + build presentation
│       ├── wallet_service.dart        # ECDSA P-256 keygen + biometric sign
│       ├── vc_schemas.dart
│       └── vp_builder.dart
└── presentation/features/
    ├── admin/
    │   ├── admin_dashboard_screen.dart    # Issuer Console (SSI KPIs + quick actions)
    │   ├── audit_log_screen.dart          # On-chain audit timeline
    │   ├── issue_sd_jwt_screen.dart       # Issue Skill / Education SD-JWT
    │   └── pending_accounts_screen.dart
    ├── contract/
    │   ├── contract_screen.dart
    │   └── contract_sign_screen.dart      # E-sign với biometric + Fabric anchor
    ├── profile/
    │   ├── profile_screen.dart
    │   └── gdpr_privacy_screen.dart       # Export Data + Delete My Data
    ├── security/
    │   ├── mfa_setup_screen.dart          # TOTP QR setup + backup codes
    │   └── sessions_screen.dart           # Active devices + logout
    ├── verifier/
    │   └── verifier_scan_screen.dart      # QR scanner + verify (Mode A/B) + SD-JWT result
    ├── wallet/
    │   ├── wallet_screen.dart             # VC cards + SD-JWT cards + status badges + biometric toggle
    │   └── disclosure_picker_screen.dart  # Selective disclosure UI
    └── workplace/
        └── workplace_screen.dart          # HRMS hub (Attendance, Requests…)
```

---

## Cấu trúc Chaincode (Hyperledger Fabric)

```
fabric-network/chaincode/asset-transfer/src/main/java/
└── org/hyperledger/fabric/samples/
    ├── IdentityLedger.java     # Main chaincode — tất cả transactions
    └── StatusListRecord.java   # DataType cho Status List
```

**Transactions chính:**

| Transaction | Type | Key format |
|---|---|---|
| `CreateProfile` | SUBMIT | `PROFILE:{employeeId}` |
| `UpdateStatusListEntry` | SUBMIT | `statuslist:{listId}` |
| `RegisterIssuer` / `IsTrustedIssuer` | SUBMIT/EVALUATE | `trustregistry:{did}` |
| `RecordSignature` / `GetSignatures` | SUBMIT/EVALUATE | `signature:{contractId}:{did}` |
| `GetRecordHistory` | EVALUATE | range scan |

---

## Demo Flow (Hội đồng)

```
1. Mở app → Wallet tab
   └── Thấy EmploymentVC với badge [ACTIVE] (Status List 2021)

2. Skill SD-JWT card → "Present with Selective Disclosure"
   └── Chọn 3/10 skills → Fingerprint → Build presentation

3. Verifier Portal (localhost:5173)
   └── Paste SD-JWT presentation → Verify
   └── Thấy disclosedClaims (chỉ 3 skills), 7 skills ẩn hoàn toàn

4. Admin: Issuer Console → Chief terminate employee
   └── Wallet badge đổi thành [REVOKED]
   └── Verifier verify lại → "VC revoked (status list ... index ...)"

5. Contract tab → "Sign with Biometric"
   └── Fingerprint → ECDSA P-256 → Anchor signature lên Fabric
   └── Ledger screen hiển thị transaction hash

6. Trust Registry tab trong Verifier Portal
   └── Danh sách trusted issuers on-chain

7. Profile → Security → Active Sessions → Logout other devices

8. Profile → Privacy → Export My Data (GDPR Art.20)
```

---

## Các file tham chiếu quan trọng

| Khái niệm | File |
|---|---|
| API constants (tất cả endpoints) | [api_constants.dart](identity_frontend/lib/core/network/api_constants.dart) |
| Router + SSI navigation | [app_router.dart](identity_frontend/lib/core/routes/app_router.dart) |
| SD-JWT model (parse + present) | [sd_jwt_holder.dart](identity_frontend/lib/core/wallet/sd_jwt_holder.dart) |
| Biometric + ECDSA signing | [wallet_service.dart](identity_frontend/lib/core/wallet/wallet_service.dart) |
| Status List service | [StatusListService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/StatusListService.kt) |
| SD-JWT issuer | [SdJwtIssuer.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtIssuer.kt) |
| Trust Registry API | [TrustRegistryController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/TrustRegistryController.kt) |
| Chaincode (tất cả transactions) | [IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) |
| Verifier Portal API client | [trustid-client.ts](verifier-portal/src/lib/trustid-client.ts) |

---

## Việc còn lại (Phase 3+)

- [ ] **Production hardening**: HMAC → Ed25519/ECDSA, public domain cho `vc.status-list.base-url`, rotate `sd-jwt.secret`
- [ ] **Flyway/Liquibase migration**: thay thế `ddl-auto=update` cho môi trường production
- [ ] **OID4VC / OID4VP full spec**: nâng cấp VP flow theo chuẩn OpenID4VC
- [ ] **Wallet sync TrainingVC / NDA-AcceptedVC**: thêm card loại mới trong wallet_screen
- [ ] **ContractSignScreen route**: điều hướng từ ContractDetailScreen → `/app/contract/:id/sign`
- [ ] **Trust Registry UI**: Admin register/revoke issuers từ mobile app

---

*TrustID — Built with Hyperledger Fabric · W3C VC Data Model · SD-JWT · DIF Universal Resolver*
