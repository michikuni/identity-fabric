# TrustID — Context dự án cho báo cáo học thuật

> **Mục đích file này**: Tổng hợp toàn bộ thông tin về dự án TrustID để dùng làm context khi yêu cầu Claude chat chỉnh sửa, hoàn thiện báo cáo `.docx` về ứng dụng blockchain trong định danh số.

---

## 1. Tên đề tài & Định vị

**Tên đề tài**: TrustID — Nền tảng Định danh Tự chủ (Self-Sovereign Identity) cho workplace credentials, xây dựng trên Hyperledger Fabric.

**Định vị học thuật**:
- Nghiên cứu và triển khai ứng dụng blockchain trong **định danh số phi tập trung** (Decentralized Identity).
- Áp dụng mô hình **SSI (Self-Sovereign Identity)** theo chuẩn W3C.
- Use-case minh họa: **workplace credentials** (chứng chỉ việc làm, kỹ năng, học vấn, chấm dứt hợp đồng…).
- HRMS (chấm công, hợp đồng, lương) đóng vai trò **Issuer minh họa**, không phải sản phẩm chính.

**Bài toán giải quyết**:
- Định danh truyền thống tập trung → dễ bị tấn công, lộ thông tin, người dùng không kiểm soát dữ liệu.
- TrustID đặt **người dùng (Holder) làm trung tâm**: tự lưu credential, tự quyết định chia sẻ thông tin nào.
- Blockchain (Hyperledger Fabric) đóng vai trò **Verifiable Data Registry** — lưu DID, Trust Registry, revocation status — không lưu PII (Personal Identifiable Information).

---

## 2. Mô hình lý thuyết — Trust Triangle (W3C)

```
        [Issuer]
       Company/Org1
     (Spring Backend)
            │ ký VC bằng HMAC-SHA256
            ▼
        [Holder]
   Employee — Flutter App
   (giữ DID + VC + private key)
            │ present VC/VP có chọn lọc
            ▼
       [Verifier]
   Bank/Gov/Recruiter
   (Verifier Portal — React SPA)
            │
            ▼
[Verifiable Data Registry]
   Hyperledger Fabric
(DID Doc + Trust Registry + Status List)
```

**4 vai trò chính**:
| Vai trò | Đại diện trong hệ thống | Chức năng |
|---|---|---|
| Issuer | Spring Backend (Org1) | Cấp Verifiable Credential, ký số bằng HMAC-SHA256, anchor lên Fabric |
| Holder | Flutter mobile app | Lưu DID + VC trong Secure Storage, sinh keypair ECDSA P-256, tạo VP |
| Verifier | React Verifier Portal | Xác minh chữ ký VC/VP, check Status List, không cần đăng nhập |
| Verifiable Data Registry | Hyperledger Fabric | Lưu DID Document, Trust Registry, Status List 2021, audit log |

---

## 3. Kiến trúc tổng thể

```
┌──────────────────────────────────────────────────────────────┐
│                    TrustID Platform                          │
│                                                              │
│   Flutter Mobile App              Verifier Portal (SPA)      │
│   (identity_frontend/)            (verifier-portal/)         │
│   - Wallet · Verifier             - Paste VC/SD-JWT          │
│   - Workplace · Profile           - Verify ngay không login  │
│         │                                  │                 │
│         └──────────────┬───────────────────┘                 │
│                        ▼                                     │
│        Spring Boot Backend (Kotlin)                          │
│        (fabric-spring-backend/)                              │
│        - REST API · JWT Auth · MFA · GDPR                    │
│        - VC Issuer · SD-JWT · Status List                    │
│                        │                                     │
│           ┌────────────┴────────────┐                        │
│           ▼                         ▼                        │
│      MySQL Database          Hyperledger Fabric 2.x          │
│      (PII + VC JSON)         (DID · Trust Registry           │
│                              · Status List · Audit log)      │
└──────────────────────────────────────────────────────────────┘
```

### Phân chia trách nhiệm dữ liệu

| Loại dữ liệu | Lưu ở đâu | Lý do |
|---|---|---|
| PII (tên, email, số điện thoại) | MySQL (off-chain) | Tuân thủ GDPR (quyền xóa) |
| VC JSON (đã ký) | MySQL | Holder download về app |
| Hash của VC (SHA-256) | Hyperledger Fabric | Verify tính toàn vẹn |
| DID Document (public key) | Hyperledger Fabric | Phi tập trung, public |
| Trust Registry (issuer hợp lệ) | Hyperledger Fabric | Verifier check on-chain |
| Status List 2021 (revocation) | Hyperledger Fabric | Bất biến, có audit |
| Chữ ký hợp đồng e-sign | Hyperledger Fabric | Non-repudiation |

---

## 4. Stack công nghệ chi tiết

### 4.1 Backend (fabric-spring-backend)
- **Ngôn ngữ**: Kotlin
- **Framework**: Spring Boot 3 (port 8080, base path `/api/v1`)
- **Authentication**: JWT (HS256, 24h expiry), Spring Security
- **Database**: MySQL 8 (`identity_db`), JPA/Hibernate
- **Fabric SDK**: Hyperledger Fabric Gateway 1.7 (Java)
- **Standards**:
  - W3C Verifiable Credentials Data Model
  - W3C DID Core
  - W3C Status List 2021 (revocation)
  - SD-JWT (Selective Disclosure JWT) — IETF draft
  - DIF Universal Resolver
- **Bảo mật bổ sung**:
  - TOTP MFA (Google Authenticator compatible) + backup codes
  - Rate limiting (Bucket4j, 10 req/min) + Account lockout (5 lần sai → khóa 15 phút)
  - GDPR Art.20 (Data Export) + Art.17 (Right to be Forgotten)
  - Device binding & session management

### 4.2 Frontend Mobile (identity_frontend)
- **Framework**: Flutter 3.x (Dart)
- **Kiến trúc**: Clean Architecture (domain / data / presentation)
- **State management**: flutter_bloc (BLoC pattern, part/part of)
- **Navigation**: go_router (ShellRoute cho bottom nav)
- **DI**: get_it (manual registration trong `core/di/injection.dart`)
- **HTTP**: Dio + JWT auth interceptor
- **Bảo mật**:
  - flutter_secure_storage (private key, VC)
  - local_auth (biometric — Face ID / fingerprint)
  - ECDSA P-256 keypair (sinh trên thiết bị, private key không bao giờ rời máy)
- **Localization**: EN + VI (flutter_localizations)
- **Color palette**: Deep navy primary (#1A237E), gold accent (#F59E0B)

### 4.3 Verifier Portal (verifier-portal)
- **Framework**: Vite 5 + React 18 + TypeScript + Tailwind CSS
- **Đặc điểm**: SPA độc lập, KHÔNG cần đăng nhập
- **Chức năng**: Paste VC / SD-JWT → verify chữ ký + check Status List on-chain → hiển thị trusted issuers
- **Dev URL**: http://localhost:5173 (proxy `/api` và `/1.0` → backend)

### 4.4 Blockchain Layer (fabric-network)
- **Hyperledger Fabric 2.x**
- **Chaincode**: Java (`asset-transfer`)
- **Network topology**:
  - 1 Orderer
  - 2 Peers (Org1)
  - 1 CA (Org1 CA)
  - Channel: `mychannel`
- **Chaincode chính**: `IdentityLedger.java`

---

## 5. Kiến trúc 4 layer của backend (quan trọng cho báo cáo)

```
┌─────────────────────────────────────────────────┐
│ Layer 1: Presentation                           │
│   presentation/controller/                      │
│   AuthController, AdminController, ...          │
│   ─ Xử lý HTTP request/response                 │
└─────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────┐
│ Layer 2: Application (Use Cases)                │
│   application/usecase/                          │
│   SignInUseCase, ApproveEmployeeUseCase, ...    │
│   ─ Business logic, orchestration               │
└─────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────┐
│ Layer 3: Infrastructure                         │
│   infrastructures/                              │
│   ├── persistence/  → MySQL (JPA Repository)    │
│   ├── fabric/       → Blockchain Bridge (async) │
│   └── vc/           → VC Issuer, SD-JWT, ...    │
└─────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────┐
│ Layer 4: Fabric Gateway (org.fabric.api)        │
│   - FabricGatewayConfig (gRPC + TLS + X.509)    │
│   - IdentityLedgerService                       │
│   - submitTransaction / evaluateTransaction     │
└─────────────────────────────────────────────────┘
                    │
                    ▼
            Hyperledger Fabric Peer
            (Chaincode IdentityLedger.java)
```

---

## 6. Cơ chế đọc/ghi blockchain (chi tiết kỹ thuật)

### 6.1 Hai loại transaction trên Fabric

| Tiêu chí | `submitTransaction` (WRITE) | `evaluateTransaction` (READ) |
|---|---|---|
| Dùng cho | UpsertRecord, RegisterDID, DeleteRecord | GetRecord, GetHistory, VerifyRecord |
| Luồng | Endorser → Orderer → Committer | Chỉ 1 Peer cục bộ |
| Có thay đổi ledger | Có | Không |
| Tốc độ | ~2–5 giây | ~milliseconds |
| Trong chaincode | `stub.putStringState()` | `stub.getStringState()` |

### 6.2 Chaincode `IdentityLedger.java` — các transaction chính

| Transaction | Loại | Key format | Mục đích |
|---|---|---|---|
| `CreateProfile` / `UpsertRecord` | SUBMIT | `profile:{employeeId}`, `contract:{id}`, … | Ghi/update hồ sơ |
| `RegisterDID` | SUBMIT | `did:{didString}` | Đăng ký DID lên ledger |
| `DeleteRecord` | SUBMIT | (soft delete, status="DELETED") | Giữ audit trail |
| `UpdateStatusListEntry` | SUBMIT | `statuslist:{listId}` | Revoke / unrevoke VC |
| `RegisterIssuer` / `IsTrustedIssuer` | SUBMIT/EVALUATE | `trustregistry:{did}` | Trust Registry on-chain |
| `RecordSignature` / `GetSignatures` | SUBMIT/EVALUATE | `signature:{contractId}:{did}` | E-sign hợp đồng |
| `GetRecord` | EVALUATE | range theo prefix | Đọc record hiện tại |
| `GetRecordHistory` | EVALUATE | `getHistoryForKey` | Audit timeline đầy đủ |
| `VerifyRecord` | EVALUATE | so sánh hash | Kiểm tra tính toàn vẹn |

### 6.3 Pattern lưu dữ liệu kép (Dual-write Pattern)

Mọi thao tác ghi đều theo **2 bước**:

```
Bước 1: Lưu MySQL (source of truth, commit ngay)
        ↓
Bước 2: Async ghi blockchain (fire-and-forget với @Async)
        ↓
   ┌────┴────┐
   ▼         ▼
Thành công   Thất bại (network/endorsement error)
   │             │
   ▼             ▼
Hoàn tất     Outbox Pattern:
             - Lưu vào bảng `fabric_outbox_events`
             - Status = PENDING, nextRetryAt = +30s
             - Scheduler chạy mỗi 5 phút retry
             - Exponential backoff: 30s → 60s → 120s → 240s → 480s
             - Sau 5 lần fail → DEAD_LETTER (cần can thiệp thủ công)
```

**Tại sao thiết kế dual-write?**
- MySQL: nhanh, hỗ trợ query phức tạp, xóa được PII (GDPR Art.17).
- Blockchain: chậm hơn nhưng bất biến, có audit trail, không lưu PII (chỉ lưu hash).
- Outbox đảm bảo **eventual consistency**: kể cả khi Fabric down, dữ liệu vẫn lên ledger sau khi network khôi phục.

---

## 7. Tính năng đã triển khai (theo phase)

### Phase 0 — SSI-first Navigation
- Bottom nav theo role: **Wallet · Verifier · Workplace · Profile**
- Tab Workplace gom toàn bộ HRMS use-cases (Attendance, Requests, Payroll)
- Issuer Console (Admin Dashboard) với 2 KPI section: SSI KPIs + Operations KPIs

### Phase 1 — W3C Credential Stack
| Tính năng | Chuẩn liên quan | Mô tả |
|---|---|---|
| **Status List 2021** | W3C Status List 2021 | Bitstring 131072 entry; Verifier auto-check ACTIVE/REVOKED |
| **SD-JWT Selective Disclosure** | IETF SD-JWT draft | Skill & Education VC — holder chọn field nào tiết lộ |
| **Verifier Portal** | — | SPA độc lập, không cần login |
| **DIF Universal Resolver** | DIF spec | `GET /1.0/identifiers/{did}` trả DID Document chuẩn W3C |
| **Biometric Unlock** | FIDO-like | ECDSA P-256 signing gate bằng fingerprint/Face ID |
| **Trust Registry on-chain** | — | Danh sách trusted issuers lưu trên Fabric |
| **E-sign Contract** | — | Ký hợp đồng bằng wallet key, anchor lên Fabric |

### Phase 2 — Security & Compliance
| Tính năng | Mô tả |
|---|---|
| **TOTP 2FA** | Setup QR → Google Authenticator → backup codes |
| **Audit Log on-chain** | Timeline lịch sử thay đổi từng employee, lọc theo loại record |
| **TrainingVC + NDA-AcceptedVC** | 2 loại VC mới cho training và NDA |
| **Rate Limiting + Account Lockout** | Bucket4j 10 req/min, lock 15 phút sau 5 lần sai |
| **GDPR Export + Erasure** | Art.20 Data Export, Art.17 Right to be Forgotten |
| **Device Binding & Session List** | Track active devices, logout từng device hoặc tất cả |

---

## 8. Các loại Verifiable Credential trong hệ thống

| Loại VC | Issuer cấp khi nào | credentialSubject chính |
|---|---|---|
| **EmploymentVC** | Admin duyệt tài khoản | department, position, employmentStatus, startDate |
| **SalaryRangeVC** | Admin/Chief issue thủ công | salaryBand (BAND_A, SENIOR…), currency, position |
| **PromotionVC** | Chief đổi chức vụ | department, oldPosition, newPosition |
| **TerminationVC** | Chief chấm dứt HĐ | department, position, terminationReason |
| **SkillVC (SD-JWT)** | Admin issue SD-JWT | Danh sách skills (holder chọn lựa tiết lộ) |
| **EducationVC (SD-JWT)** | Admin issue SD-JWT | Bằng cấp, trường, năm (chọn lựa tiết lộ) |
| **TrainingVC** | Admin issue sau khóa training | courseId, courseName, completedAt |
| **NDA-AcceptedVC** | Khi user accept NDA | ndaVersion, acceptedAt, ipAddress |

**Cơ chế ký VC**:
- Thuật toán: **HMAC-SHA256** (secret key riêng cho Issuer)
- Lý do dùng HMAC thay vì ECDSA cho VC: đơn giản hóa POC; production nên migrate sang Ed25519/ECDSA
- Verifier xác minh bằng cách gọi backend `/api/v1/identity/vc/verify`
- DID public key lưu trên Fabric dùng cho VP signature (holder ký, không phải issuer ký)

---

## 9. Selective Disclosure — SD-JWT (điểm sáng học thuật)

**Vấn đề**: VC truyền thống lộ toàn bộ credentialSubject khi holder present. Ví dụ employee phải tiết lộ TẤT CẢ skills khi chứng minh chỉ 1 skill.

**Giải pháp SD-JWT**:
1. Issuer băm từng claim với salt riêng: `hash_i = SHA256(salt_i || claim_i)`.
2. JWT chỉ chứa **danh sách hash**, không chứa giá trị claim gốc.
3. Salt + claim gốc gửi riêng cho holder qua "disclosure".
4. Khi present, holder chỉ gửi disclosure của field muốn tiết lộ.
5. Verifier băm lại từng disclosure và đối chiếu với hash trong JWT.

**Ví dụ**: Skills [Java, Kotlin, Python, Go, Rust, ML, K8s, Docker, AWS, GCP]
- Holder chỉ present Kotlin + Docker → Verifier biết được 2 skills này thuộc credential gốc
- 8 skills còn lại VẪN ẨN HOÀN TOÀN, Verifier không biết tồn tại

→ Đảm bảo **data minimization** (GDPR principle), zero-knowledge nhẹ.

---

## 10. Status List 2021 — Cơ chế revocation

**Vấn đề**: Sau khi cấp VC, làm sao revoke (thu hồi) mà vẫn giữ tính phi tập trung?

**Giải pháp Status List 2021**:
1. Một bitstring lớn (131072 bit) lưu trên Fabric.
2. Mỗi VC có `credentialStatus.statusListIndex` trỏ đến bit thứ N.
3. Bit = 0 → ACTIVE. Bit = 1 → REVOKED.
4. Khi Chief terminate employee → backend gọi `UpdateStatusListEntry` chaincode → set bit N = 1.
5. Verifier khi verify VC → fetch Status List từ `GET /api/v1/status-list/{listId}` → check bit → biết REVOKED.

**Ưu điểm**:
- Privacy-preserving: 1 bitstring chứa trạng thái của 131072 VC → Verifier không biết đang check VC nào (k-anonymity).
- Cache-friendly: 1 file Status List dùng cho nhiều verify.
- Bất biến: mỗi lần update bit là 1 transaction trên Fabric → có audit.

---

## 11. Hyperledger Fabric — Tại sao chọn?

| Lý do | Giải thích |
|---|---|
| **Permissioned blockchain** | Phù hợp với enterprise — biết rõ identity của participant qua MSP |
| **Không cần token/cryptocurrency** | Không tốn gas fee, không phụ thuộc giá token |
| **Smart contract (chaincode) bằng Java/Go** | Quen thuộc với dev backend, không cần học Solidity |
| **Privacy support** | Private Data Collections, Channel-level isolation |
| **High throughput** | ~3500 TPS, đủ cho enterprise scale |
| **Tách Endorsement & Ordering** | Linh hoạt hơn Ethereum (PoW/PoS) |
| **MSP (Membership Service Provider)** | Quản lý identity của các Org bằng X.509 CA |

---

## 12. Demo Flow (cho hội đồng / báo cáo)

```
1. Mở Flutter app → Wallet tab
   └── Thấy EmploymentVC với badge [ACTIVE] (Status List 2021)
   └── Thấy DID card: did:fabric:trustid:42

2. Skill SD-JWT card → "Present with Selective Disclosure"
   └── Chọn 3/10 skills → Fingerprint xác thực → Build VP
   └── App tạo QR chứa VP

3. Mở Verifier Portal (localhost:5173)
   └── Paste SD-JWT presentation → Verify
   └── Hiển thị disclosedClaims (3 skills), 7 skills ẩn hoàn toàn
   └── Trust Registry tab: list trusted issuers từ Fabric

4. Admin/Chief terminate employee
   └── Backend gọi UpdateStatusListEntry → bit N = 1 trên Fabric
   └── Wallet badge đổi thành [REVOKED]
   └── Verifier verify lại → "VC revoked (status list ... index ...)"

5. Contract tab → "Sign with Biometric"
   └── Fingerprint → ECDSA P-256 sign hợp đồng
   └── Backend gọi RecordSignature → anchor lên Fabric
   └── Ledger screen hiển thị transaction hash

6. Profile → Security → Active Sessions → Logout other devices

7. Profile → Privacy → Export My Data (GDPR Art.20)
   └── Download JSON chứa toàn bộ data của user

8. Audit log screen (Admin)
   └── Gọi GetRecordHistory chaincode
   └── Hiển thị toàn bộ lịch sử thay đổi của 1 employee (txId, timestamp, action)
```

---

## 13. Đánh giá — Đạt được & Hạn chế

### Đạt được
- Triển khai đầy đủ Trust Triangle (Issuer / Holder / Verifier / Registry).
- Áp dụng 4 chuẩn W3C/IETF: VC Data Model, DID Core, Status List 2021, SD-JWT.
- Selective Disclosure + Biometric Unlock — phù hợp với GDPR data minimization.
- Outbox Pattern đảm bảo eventual consistency giữa MySQL và Fabric.
- Tuân thủ GDPR Art.17 (xóa PII trong MySQL, hash vẫn còn trên Fabric).
- Audit log on-chain — non-repudiation cho contract signatures.
- MFA, rate limiting, device binding — đáp ứng OWASP best practices.

### Hạn chế / Hướng phát triển
- VC ký bằng **HMAC-SHA256** thay vì ECDSA/Ed25519 → cần migrate cho production.
- Chỉ 1 Org (Org1) trong Fabric network → cần đa Org để demo cross-organization trust.
- Chưa hỗ trợ đầy đủ **OID4VC / OID4VP** (OpenID for Verifiable Credentials).
- Status List size cố định 131072 → cần cơ chế shard cho scale lớn hơn.
- `spring.jpa.hibernate.ddl-auto=update` cho dev — production cần Flyway/Liquibase migration.
- Chưa có Zero-Knowledge Proof đầy đủ (BBS+ signatures, AnonCreds).
- Trust Registry chưa có UI cho Admin đăng ký/thu hồi issuer từ mobile.

---

## 14. Cấu trúc thư mục dự án

```
identity-fabric/
├── fabric-network/                    # Hyperledger Fabric (Docker)
│   ├── chaincode/asset-transfer/      # Java chaincode
│   │   └── src/.../IdentityLedger.java
│   ├── organizations/                 # CA + crypto material
│   ├── docker/                        # docker-compose
│   └── start.ps1                      # Khởi động network
│
├── fabric-spring-backend/             # Spring Boot (Kotlin)
│   └── src/main/kotlin/
│       ├── com/mpcorp/identity/       # Business logic
│       │   ├── application/usecase/
│       │   ├── infrastructures/
│       │   │   ├── fabric/            # FabricLedgerBridge (async + Outbox)
│       │   │   ├── persistence/       # JPA + MySQL
│       │   │   └── vc/                # VC Issuer, SD-JWT, Status List
│       │   └── presentation/controller/
│       └── org/fabric/api/            # Fabric Gateway Layer
│           ├── config/FabricGatewayConfig.kt
│           └── service/IdentityLedgerService.kt
│
├── identity_frontend/                 # Flutter app (Dart)
│   └── lib/
│       ├── core/
│       │   ├── wallet/                # SD-JWT, VC, biometric
│       │   ├── network/               # Dio + JWT interceptor
│       │   └── security/
│       └── presentation/features/
│           ├── wallet/                # VC cards + SD-JWT cards
│           ├── verifier/              # QR scan + verify
│           ├── admin/                 # Issuer Console
│           ├── contract/              # E-sign
│           ├── security/              # MFA, Sessions
│           └── profile/               # GDPR Privacy
│
└── verifier-portal/                   # React SPA
    └── src/
        ├── pages/
        └── lib/trustid-client.ts      # API client
```

---

## 15. API Reference (tóm tắt)

### Authentication
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/sign-in` | Đăng nhập, trả JWT |
| POST | `/api/v1/auth/sign-up` | Đăng ký tài khoản |
| POST | `/api/v1/mfa/validate` | Validate TOTP |

### VC & Identity
| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/v1/status-list/{listId}` | Status List 2021 VC |
| POST | `/api/v1/identity/vc/verify` | Verify VC + Status List |
| GET | `/1.0/identifiers/{did}` | DIF Universal Resolver |
| GET | `/api/v1/trust-registry/issuers` | Danh sách trusted issuers |

### SD-JWT
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/sd-jwt/issue/skill/{employeeId}` | Issue Skill SD-JWT |
| POST | `/api/v1/sd-jwt/present` | Build selective presentation |
| POST | `/api/v1/sd-jwt/verify` | Verify SD-JWT |

### Ledger (Blockchain)
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/ledger/records` | Upsert record lên Fabric |
| GET | `/api/v1/ledger/records/{id}/{type}` | Đọc record từ Fabric |
| GET | `/api/v1/ledger/records/{id}/{type}/history` | Audit log on-chain |
| GET | `/api/v1/ledger/records/{id}/{type}/verify?hash=...` | Verify integrity |

### Admin
| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/issuer-stats` | SSI KPI dashboard |
| POST | `/api/v1/admin/employees/{id}/issue-training-vc` | Issue TrainingVC |
| POST | `/api/v1/contracts/{id}/sign` | Anchor e-signature on Fabric |

---

## 16. Cấu hình & biến môi trường quan trọng

```properties
# JWT
jwt.secret=<HS256 secret 64+ ký tự>
jwt.expiration=86400000          # 24h

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/identity_db
spring.datasource.username=root
spring.datasource.password=password

# VC HMAC signing
vc.secret=${VC_SECRET:vc-secret-trustid-org1-2026}
vc.issuer-did=did:fabric:trustid:org1

# Status List 2021
vc.status-list.id=employment-status-list-1
vc.status-list.size=131072
vc.status-list.base-url=http://localhost:8080/api/v1/status-list

# SD-JWT
sd-jwt.secret=${SD_JWT_SECRET:sd-jwt-secret-trustid-org1-2026}

# Fabric Gateway
fabric.peer.endpoint=localhost:7051
fabric.gateway.mspId=Org1MSP
fabric.gateway.certPath=/path/to/User1@org1/cert.pem
fabric.gateway.keyPath=/path/to/User1@org1/key.pem
fabric.channelName=mychannel
fabric.chaincodeName=asset-transfer
```

---

## 17. Tài liệu tham khảo (cho phần References của báo cáo)

| # | Nguồn |
|---|---|
| 1 | W3C Verifiable Credentials Data Model 1.1 — https://www.w3.org/TR/vc-data-model/ |
| 2 | W3C Decentralized Identifiers (DIDs) v1.0 — https://www.w3.org/TR/did-core/ |
| 3 | W3C Status List 2021 — https://www.w3.org/TR/vc-status-list/ |
| 4 | IETF SD-JWT Draft — https://datatracker.ietf.org/doc/draft-ietf-oauth-selective-disclosure-jwt/ |
| 5 | DIF Universal Resolver — https://github.com/decentralized-identity/universal-resolver |
| 6 | Hyperledger Fabric Documentation — https://hyperledger-fabric.readthedocs.io/ |
| 7 | NIST SP 800-63-3 Digital Identity Guidelines |
| 8 | GDPR — Regulation (EU) 2016/679 (Art.17, Art.20) |
| 9 | OWASP Authentication Cheat Sheet |
| 10 | OpenID for Verifiable Credential Issuance (OID4VCI) |

---

## 18. Cách dùng file này khi yêu cầu Claude chỉnh sửa báo cáo

**Mẫu prompt khi paste lên Claude chat**:

```
Tôi đang viết báo cáo học thuật về ứng dụng blockchain trong định danh số.
Đây là context của dự án TrustID mà tôi đã xây dựng:

[paste toàn bộ nội dung project_context.md]

Bây giờ tôi cần bạn giúp [VIỆC CỤ THỂ — ví dụ:]:
- Viết lại phần Mở đầu (1.500 từ) theo phong cách học thuật
- Tóm tắt chương 2 thành abstract 250 từ
- Bổ sung phần so sánh TrustID với các SSI framework khác (Hyperledger Indy, ION, Sovrin)
- Sửa câu văn ở đoạn này cho học thuật hơn: [paste đoạn]
- Đề xuất danh mục Tài liệu tham khảo cho phần [X]

Yêu cầu chung:
- Tone học thuật, dùng đại từ "chúng tôi" / "tác giả"
- Trích dẫn chuẩn (IEEE / APA)
- Khi nhắc đến số liệu kỹ thuật, dựa trên context ở trên
```

---

*File context này được cập nhật ngày 2026-05-17. Khi dự án thay đổi đáng kể (thêm phase mới, đổi stack, …), cần cập nhật lại trước khi dùng cho prompt mới.*
