# TrustID — Identity Fabric Project Context

> Last analyzed: 2026-05-08
> Owner: Minh Phuong Dang (minhphuonglcby@gmail.com)

## 1. Tổng quan dự án

**TrustID** là một nền tảng **quản lý danh tính & nhân sự (HR)** kết hợp công nghệ **Self-Sovereign Identity (SSI)** dựa trên **Hyperledger Fabric**. Hệ thống áp dụng kiến trúc **hybrid blockchain–database**: dữ liệu nhạy cảm (PII, hợp đồng, lương) lưu off-chain (MySQL), trong khi blockchain chỉ giữ **metadata + SHA-256 hash + audit trail bất biến** và các **DID Document** theo chuẩn W3C.

Mục tiêu chính:
- Cấp & quản lý **Verifiable Credentials (VC)** cho nhân viên (Employment, Salary Range, Promotion, Termination)
- Cho phép nhân viên mang theo VC để chứng minh danh tính qua **OID4VP** (selective disclosure) — ngân hàng / nhà tuyển dụng tương lai có thể verify trực tiếp mà không cần liên hệ employer
- Đảm bảo **tính toàn vẹn & truy xuất audit** cho mọi thay đổi hồ sơ HR thông qua hash on-chain

Hệ thống gồm 3 module nằm trong cùng workspace `d:/Academy/identity-fabric/`:

| Module | Vai trò | Stack chính |
|---|---|---|
| [`fabric-network/`](fabric-network/) | Hyperledger Fabric network + chaincode `identity-ledger` | Java chaincode, Fabric 2.5, Docker |
| [`fabric-spring-backend/`](fabric-spring-backend/) | API server (HR + DID/VC + Fabric bridge) | Spring Boot 4.0 + Kotlin, JPA/MySQL, Fabric Gateway SDK |
| [`identity_frontend/`](identity_frontend/) | App di động cho nhân viên / quản lý / chief / admin | Flutter, BLoC, GoRouter, Dio, Firebase |

---

## 2. Module: `fabric-network` (Hyperledger Fabric)

### 2.1 Topology
- **2 organizations**: Org1MSP, Org2MSP
- **Peers**: peer0/peer1 cho Org1 (7051/8051) và Org2 (9051/10051)
- **Orderer**: 1 orderer.example.com (7050) — etcdraft consensus
- **CA**: ca.org1 (7054), ca.org2 (8054) — fabric-ca:1.5.7
- **Channel**: `mychannel` (single channel, MAJORITY Endorsement policy)
- **Container network**: `fabric_network` (Docker Compose), TLS bật mặc định, mTLS cho gossip

### 2.2 Chaincode `identity-ledger` (Java)
- Vị trí: [`fabric-network/chaincode/asset-transfer/`](fabric-network/chaincode/asset-transfer/)
- Fabric Chaincode Shim 2.5.0, JSON serialization bằng Genson 1.6

**Data models**:
- `IdentityRecord`: `recordId` = `{recordType}:{employeeId}`, `recordType` ∈ {PROFILE, CONTRACT, PAYROLL}, `status` ∈ {ACTIVE, REVOKED, DELETED}, `keyFields` (JSON non-sensitive), `dataHash` (SHA-256 của full off-chain data), `action`, `timestamp`, `updatedBy`
- `DIDDocument`: `did` = `did:fabric:trustid:<employeeCode>`, `publicKeyJwk` (ECDSA P-256), `controller`, `status` ∈ {ACTIVE, REVOKED}, `revokedBy`, `revokeReason`

**Transactions**:
- Submit: `InitLedger`, `UpsertRecord`, `DeleteRecord` (soft delete), `RegisterDID`, `RevokeDID`
- Evaluate: `GetRecord`, `RecordExists`, `GetRecordHistory`, `GetAllRecordsByEmployee`, `GetAllRecords`, `ResolveDID`, `VerifyRecord` (so sánh hash)
- Events: `IdentityRecordUpserted`, `IdentityRecordDeleted`, `DIDRegistered`, `DIDRevoked`

### 2.3 Bootstrap & deploy
- Script chính: [`fabric-network/scripts/network.sh`](fabric-network/scripts/network.sh)
- Lệnh: `network.sh up` → `createChannel` → `deployCC` → `down`
- Tạo crypto từ `crypto-config.yaml`, channel artifacts từ `configtx.yaml`

---

## 3. Module: `fabric-spring-backend` (Spring Boot + Kotlin)

### 3.1 Tech stack
- **Spring Boot 4.0.5**, Kotlin 2.2.21, Java 17
- **Database**: MySQL via Spring Data JPA (`ddl-auto=update`, không có Flyway/Liquibase)
- **Auth**: Spring Security 6 + JJWT 0.12.6 (HS256, 24h expiry)
- **Blockchain**: Hyperledger Fabric Gateway SDK 1.7.0 (gRPC + mTLS)
- **Docs**: SpringDoc OpenAPI 2.8.6 (Swagger UI)
- **Real-time**: Spring WebSocket / STOMP
- Build: Gradle Kotlin DSL

### 3.2 Cấu trúc package
Hai service chạy chung process:

**`com.mpcorp.identity`** — service HR & Identity chính (Clean Architecture):
- `application/` — DTOs, use cases (auth, employee, contract, payroll, attendance, request, company), mappers
- `domain/` — entity (Employee, Auth, Contract, Payroll, Profile, Attendance, LeaveRequest, Company), repository interfaces
- `infrastructures/`:
  - `config/` — SecurityConfig, DataInitializer
  - `fabric/` — `FabricLedgerBridge` (domain → blockchain), `FabricRetryScheduler` (30s outbox tick), `FabricOutboxService` (exponential backoff retry)
  - `persistence/` — JPA entities & repositories
  - `security/` — `JwtAuthFilter`, UserDetailsService
  - `vc/` — `VcIssuerService` (issue 4 loại VC), `VpService` (OID4VP verify), `VpSessionStore`
- `presentation/` — controllers, request/response DTOs, BearerAuthIdResolver
- `common/` — JwtUtils, enums (EmployeeRole, AccountStatus), exceptions

**`org.fabric.api`** — service Fabric Gateway thuần:
- `config/FabricGatewayConfig` — gRPC channel, load mTLS cert/key, build Gateway bean
- `controller/IdentityLedgerController` — REST endpoints gọi chaincode
- `service/IdentityLedgerService` — submit/evaluate transaction
- `websocket/` — `FabricEventPublisher` broadcast `/topic/identity`
- KHÔNG dùng Spring Security & JPA (loại trừ auto-config)

### 3.3 Domain & schema chính (MySQL)

| Bảng | Ghi chú |
|---|---|
| `auth` | id (UUID), email, phone, password (bcrypt), role (EMPLOYEE/MANAGER/CHIEF/ADMIN), status (PENDING/ACTIVE/REJECTED) |
| `employee` | auth_id, department, position, manager_id (self-ref), **did**, **public_key**, **employment_vc**, **termination_vc**, **salary_range_vc**, **promotion_vc** (LONGTEXT) |
| `profile` | name, gender, identity_type/number, dob, residence, education_level, skills (JSON) |
| `contract` | type, start/end date, probation, tax_code, social_insurance_number |
| `payroll` | salary_type, base_salary, bonus, total_income, currency, bank info |
| `attendance` | work_date, check_in_time, check_out_time |
| `leave_request` | type, dates, status, approved_by |
| `fabric_outbox_events` | outbox pattern: PENDING/RETRYING/COMPLETED/DEAD_LETTER, indexed `(event_status, next_retry_at)` |

### 3.4 REST API surface
**Public** (no auth):
- `POST /api/v1/auth/signin`, `/signup`
- `GET /api/v1/identity/did/{did}` — DID resolve
- `GET /api/v1/identity/vc/{employment|termination|salary|promotion}/{employeeId}`
- `POST /api/v1/identity/vc/verify`, `GET .../verify-by-id?vcId=...`
- `POST /api/v1/oidc/vp/{request|submit}`, `GET .../result/{state}`
- `GET /api/v1/oidc/.well-known/openid-configuration`
- `GET /ws/**` (WebSocket), Swagger

**Protected** (JWT + role):
- `/api/v1/employee/**`, `/profile/**`, `/contract/**`, `/payroll/**`, `/attendance/**`, `/request/**`
- `/api/v1/admin/**` → ADMIN/CHIEF (account approval, salary VC issuance)
- `/api/v1/chief/**` → CHIEF/ADMIN (terminate employee, revoke DID)
- `/api/v1/manager/**` → MANAGER+ (team mgmt, attendance review)
- `/api/v1/ledger/**` → CHIEF/ADMIN (Fabric ledger ops)

### 3.5 Fabric integration pattern (quan trọng)
**Fire-and-forget + Outbox retry**:
1. Use case lưu vào MySQL (source of truth) — commit trước
2. Gọi `@Async` `FabricLedgerBridge.upsert*(entity)`
3. Bridge tính SHA-256 hash của full JSON, trích `keyFields` (non-sensitive), submit qua `IdentityLedgerService`
4. Thành công → log + WebSocket event
5. Thất bại → enqueue vào `fabric_outbox_events`
6. `FabricRetryScheduler` chạy mỗi 30s, exponential backoff `30s * 2^retryCount`, max 5 lần → DEAD_LETTER

### 3.6 Verifiable Credentials (W3C-style)
`VcIssuerService` ký bằng **HMAC-SHA256** (secret `vc.secret`) — POC; production cần Ed25519. 4 loại VC issue tự động:
- **EmploymentVC** — khi admin approve account
- **SalaryRangeVC** — khi assign payroll (band ENTRY/MID/SENIOR/EXECUTIVE, KHÔNG lộ số chính xác)
- **PromotionVC** — khi đổi role/position
- **TerminationVC** — khi terminate (kèm revoke DID)

### 3.7 OID4VP flow
- `VpSessionStore` (in-memory): state, nonce, vcType, requestedClaims, vpToken, result, expiry
- Verifier tạo request → Holder scan QR → submit VP → Verifier poll result

### 3.8 Cấu hình quan trọng
- Cert paths trong `FabricGatewayConfig` đang hard-code Linux: `/home/phuongdang/identity-fabric/fabric-network/organizations/...` — cần override khi chạy trên Windows hoặc dùng env var
- `fabric.msp-id=Org1MSP`, `channel=mychannel`, `chaincode=identity-ledger`, `peer.endpoint=localhost:7051`
- Timeouts: evaluate 5s, endorse 15s, submit 5s, commit 60s

---

## 4. Module: `identity_frontend` (Flutter)

### 4.1 Tech stack
- Dart 3.11.1+, Flutter
- **State**: `flutter_bloc` 9.1.1 + `equatable`
- **Routing**: `go_router` 17.1.0 (role-based redirect)
- **DI**: `get_it` 9.2.1 + `injectable` 3.0.0
- **HTTP**: `dio` 5.9.2 + interceptors
- **Secure storage**: `flutter_secure_storage` 10.0.0 (Keystore/Keychain)
- **Crypto**: `pointycastle` 4.0.0 (ECDSA P-256 cho DID wallet)
- **QR**: `qr_flutter` (gen) + `mobile_scanner` 7.2.0 (scan)
- **Firebase**: core, remote_config (resolve baseUrl runtime), crashlytics, messaging, analytics
- **Forms**: `formz` 0.8.0
- L10n generated, default locale **VI**, có EN

### 4.2 Kiến trúc — Clean Architecture, feature-first
```
lib/
├── core/               # DI, firebase, network, routes, storage, wallet, qr, themes, locale
├── data/               # datasources/, models/, repositories/
├── domain/             # entities/, repositories/, usecases/
├── presentation/features/
│   ├── auth/, onboarding/, splash/
│   ├── attendance/, requests/, directory/, contract/, payroll/, profile/, home/, company/
│   ├── wallet/         # DID + VC display + QR generation
│   ├── verifier/       # OID4VP verifier (2 modes)
│   ├── cccd/           # Vietnamese CCCD scan during onboarding
│   ├── chief/, admin/, manager/, ledger/
└── l10n/               # app_en.arb, app_vi.arb
```

### 4.3 Bootstrap (`lib/main.dart`)
1. `WidgetsFlutterBinding.ensureInitialized()` + lock portrait + transparent status bar
2. `configureDependencies()` — Firebase init → RemoteConfig fetch baseUrl → ApiClient.init → register services trong GetIt
3. `LocaleCubit.load()` từ SharedPreferences (default VI)
4. `ICrashlyticsService.runWithCrashReporting()` wrap `runApp(TrustIdApp)`
5. Root: `TrustIdApp` = MultiBlocProvider(LocaleCubit + AuthBloc) + MaterialApp.router

### 4.4 Routing (`lib/core/routes/app_router.dart`)
- `/` Splash → `/auth/sign-in` | `/auth/sign-up` | `/auth/onboarding/cccd-scan` → `/auth/onboarding/profile`
- ShellRoute `/app/*` (bottom nav, persistent):
  - Common: `/home`, `/profile`, `/attendance` (+ `/history`, `/timesheet`), `/requests` (+ `/create`), `/directory`, `/contract`, `/payroll`, `/company`, `/wallet`, `/verifier`
  - Manager: `/app/manager/requests`, `/app/manager/timesheet`
  - Chief: `/app/chief`, `/app/admin/pending-accounts`
  - Admin: `/app/admin`, `/app/admin/ledger`
  - Chief+Admin: `/app/ledger`
- Bottom nav items thay đổi theo role (EMPLOYEE / MANAGER / CHIEF / ADMIN)
- Redirect guard: chưa đăng nhập → `/auth/sign-in`; sai role → `/app/home`

### 4.5 BLoC chính
- **AuthBloc** (global): SignInSubmitted, SignUpSubmitted, AuthLoggedOut → tích hợp Analytics (set userId/role)
- **LocaleCubit** (global): toggle VI ↔ EN, persist SharedPreferences
- Feature-scoped: AttendanceBloc, RequestBloc, DirectoryBloc, CompanyBloc
- Wallet & Verifier dùng StatefulWidget thuần (chưa BLoC)

### 4.6 Identity wallet ([`lib/core/wallet/wallet_service.dart`](identity_frontend/lib/core/wallet/wallet_service.dart))
- `generateAndSave()` — tạo P-256 keypair (PointyCastle), save privateKey hex + publicKey JWK vào SecureStorage (idempotent)
- DID format: `did:fabric:trustid:<employeeNumericId>`
- Onboarding flow: scan CCCD → tạo keypair local → submit publicKey với signup → admin approve → backend register DID + issue EmploymentVC
- VCs lưu encrypted on-device (Keystore/Keychain)
- Schema definitions: [`lib/core/wallet/vc_schemas.dart`](identity_frontend/lib/core/wallet/vc_schemas.dart)

### 4.7 OID4VP flow
- `VpBuilder.build()` tạo W3C VP với selective disclosure (chỉ field user chọn)
- Proof: HMAC-SHA256 + nonce (POC)
- Verifier mode A: scan VC/VP QR → POST `/identity/vc/verify`
- Verifier mode B: tạo VP Request QR → holder scan → submit → verifier poll `/oidc/vp/result/{state}`

### 4.8 QR codec ([`lib/core/qr/vc_qr_payload_codec.dart`](identity_frontend/lib/core/qr/vc_qr_payload_codec.dart))
- Ưu tiên: `vcid:<id>` (~40 chars) → fallback `vcz1:` (gzip+base64url) → raw JSON
- Selective disclosure: `vcid:<id>?fields=field1,field2`

### 4.9 API client ([`lib/core/network/api_client.dart`](identity_frontend/lib/core/network/api_client.dart))
- Dio, base URL từ Firebase RemoteConfig (fallback hard-coded `http://188.122.1.106:8080/api/v1`)
- Timeouts 15s
- Interceptors:
  - **AuthInterceptor**: tự gắn `Bearer <jwt>` từ SecureStorage; 401 → clear storage + redirect `/auth/sign-in`
  - **LogInterceptor**: log request/response (dev)
- ApiException wrap DioException

### 4.10 CCCD onboarding (Vietnamese-specific)
- Parse QR pipe-delimited: `id|cccdNumber|name|dob|gender|address|issueDate`
- ddMMyyyy date, auto-detect gender (Nữ/Female/1 → FEMALE)
- Validate fields → submit cùng signup

### 4.11 Theming
- Material 3, light only (chưa có dark)
- Font Inter, primary blue TrustID, 14px button radius, 16px card radius
- Vietnamese labels mặc định: Chấm công, Đơn từ, Nhân viên...

---

## 5. Mô hình tích hợp end-to-end

### 5.1 Approval flow (cấp VC + DID)
1. User signup → `auth.status=PENDING` (MySQL)
2. Admin gọi `/api/v1/admin/approve` → `auth.status=ACTIVE`
3. Backend async:
   - `FabricLedgerBridge.registerDID(did, publicKeyJwk)` → chaincode `RegisterDID`
   - `VcIssuerService.issueEmploymentVC(employeeId)` → save vào `employee.employment_vc`
4. App pull VC qua `GET /identity/vc/employment/{employeeId}` → lưu SecureStorage

### 5.2 Hash integrity flow (mọi update profile/contract/payroll)
1. Use case save MySQL → commit
2. `@Async FabricLedgerBridge.upsert*()` → SHA-256 toàn bộ entity → submit `UpsertRecord(employeeId, recordType, status, keyFields, dataHash, ...)`
3. Verify sau này: client gửi data → backend tính hash → gọi chaincode `VerifyRecord` so sánh

### 5.3 Verification flow (bên ngoài)
1. Verifier (bank, employer) tạo VP request → QR
2. Employee scan QR → wallet chọn fields → tạo VP → submit
3. Backend verify: kiểm HMAC proof + expiry → check DID `ResolveDID` chaincode → return result
4. Verifier poll `/oidc/vp/result/{state}`

---

## 6. Lưu ý vận hành & gotchas

- **Cert paths hard-code Linux** trong [`fabric-spring-backend`](fabric-spring-backend/) `FabricGatewayConfig` (`/home/phuongdang/...`) → cần env var hoặc symlink khi chạy Windows
- **Schema management**: dùng Hibernate `ddl-auto=update` — KHÔNG có migration tool, cần cẩn thận khi thay đổi entity
- **VC proof là HMAC-SHA256** — POC, không phải Ed25519/EdDSA chuẩn production
- **VpSessionStore in-memory** — không scale ngang được (cần Redis nếu deploy multi-instance)
- **Outbox table không có job dọn DEAD_LETTER** — cần manual intervention
- **Frontend baseUrl `http://188.122.1.106:8080`** trong [`api_constants.dart`](identity_frontend/lib/core/network/api_constants.dart) là Windows host của WSL2 backend dev — Firebase RemoteConfig override khi production
- **Single channel + 2 orgs** — chỉ phù hợp dev/demo, không phải topology production
- Workspace có nhiều file design / spec markdown ở root: [`TECHNICAL_DOCUMENT.md`](TECHNICAL_DOCUMENT.md), [`TrustId-v2.md`](TrustId-v2.md), [`blockchain.md`](blockchain.md), [`credential.md`](credential.md), [`figma-design-spec.md`](figma-design-spec.md), [`salaryVC.md`](salaryVC.md), [`trustid-uc-detailed.md`](trustid-uc-detailed.md), [`QR.md`](QR.md), [`report.md`](report.md), [`next.md`](next.md) — nguồn tham chiếu thiết kế chi tiết.

---

## 7. Roles & permissions tóm tắt

| Role | Quyền chính |
|---|---|
| **EMPLOYEE** | Self-profile, attendance check-in/out, leave request, view own VC, scan VP, verify others |
| **MANAGER** | + Approve subordinate leave requests, view team timesheet |
| **CHIEF** | + Terminate employee (revoke DID + issue TerminationVC), HR management, view ledger |
| **ADMIN** | + Approve pending accounts, system dashboard, full ledger ops, manage all entities |

## Research Contribution

This project contributes:
- A hybrid blockchain-database architecture for HR identity management
- Integration of SSI concepts into enterprise HR workflows
- A practical DID + VC implementation using Hyperledger Fabric
- Selective disclosure verification via OID4VP
- Hash-based integrity verification for HR records
- An asynchronous blockchain synchronization model using Outbox Pattern

## Why Hyperledger Fabric

Hyperledger Fabric was selected because:
- Permissioned blockchain phù hợp dữ liệu HR nội bộ
- Fine-grained access control
- Lower transaction cost compared to public blockchain
- Better privacy model
- Enterprise-oriented architecture
- Deterministic endorsement policies
- No cryptocurrency dependency

## Threat Model

The system considers:
- Identity forgery
- Unauthorized profile modification
- Insider tampering
- Replay attacks
- Credential leakage
- DID impersonation
- Database compromise
- Blockchain node compromise

Mitigation strategies:
- SHA-256 integrity hashing
- DID verification
- JWT authentication
- mTLS Fabric communication
- Immutable audit trail
- Selective disclosure

## Experimental Evaluation Plan

The evaluation focuses on:
- DID registration latency
- VC issuance latency
- VP verification latency
- Blockchain synchronization overhead
- Hash verification performance
- REST API response time
- Fabric transaction commit time

Environment:
- Dockerized Fabric network
- MySQL 8
- Spring Boot backend
- Flutter Android client

Metrics:
- Average response time
- Throughput
- Success rate
- Retry count
- CPU and memory usage

## Research Questions

This research aims to answer the following questions:

1. How can blockchain improve integrity and trust in HR identity management systems?

2. Can Self-Sovereign Identity (SSI) concepts be practically integrated into enterprise HR workflows?

3. How effective is a hybrid blockchain-database architecture compared to fully centralized identity systems?

4. What are the trade-offs between on-chain integrity verification and off-chain operational storage?

5. How does asynchronous blockchain synchronization affect consistency and system reliability?

## System Limitations

Current limitations include:

- VC proof mechanism currently uses HMAC-SHA256 instead of production-grade Ed25519 signatures
- Single-channel Fabric topology is not optimized for enterprise-scale deployment
- In-memory VP session store does not support horizontal scaling
- Hibernate ddl-auto=update lacks controlled schema migration
- Blockchain synchronization remains eventually consistent
- The system still depends on a centralized backend API layer
- No zero-knowledge proof (ZKP) implementation

## Future Improvements

Potential future improvements:

- Replace HMAC proof with Ed25519 / BBS+ signatures
- Integrate Zero-Knowledge Proof (ZKP)
- Use Redis for distributed VP session storage
- Introduce multi-channel or multi-consortium Fabric topology
- Implement decentralized storage (IPFS)
- Add revocation registry for VC lifecycle management
- Improve DID interoperability with external SSI ecosystems