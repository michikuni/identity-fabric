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

## 1.5. So sánh với các SSI framework hiện có (Related Work)

Trước khi quyết định tự xây dựng TrustID trên Hyperledger Fabric, tác giả khảo sát các nền tảng SSI tiêu biểu nhằm xác định khoảng trống nghiên cứu và biện luận lựa chọn stack.

### 1.5.1. Bảng so sánh

| Tiêu chí | Hyperledger Indy / Aries | Sovrin Network | Microsoft Entra Verified ID | ION (DIF) | EBSI | Veramo | **TrustID** |
|---|---|---|---|---|---|---|---|
| Loại ledger | Permissioned DLT riêng (sovrin-node) | Public permissioned (Indy) | Public (Sidetree on Bitcoin) | Public (Sidetree on Bitcoin) | Permissioned (Hyperledger Besu) | Agnostic (plugin) | Permissioned (Fabric 2.x) |
| DID method | `did:sov`, `did:indy` | `did:sov` | `did:web`, `did:ion` | `did:ion` | `did:ebsi` | nhiều method qua plugin | `did:fabric:trustid:*` (tự định nghĩa) |
| VC format | AnonCreds (CL signature) | AnonCreds | JWT VC + JSON-LD | JWT VC | JWT VC + JSON-LD | JWT VC + JSON-LD | JWT VC + SD-JWT |
| Revocation | CL accumulator (ZKP) | CL accumulator | Status List 2021 | (tuỳ implementation) | Status List 2021 | Status List 2021 | **Status List 2021 (bitstring 131072)** |
| Selective Disclosure | ZKP (BBS+, CL) | ZKP (BBS+, CL) | SD-JWT (preview) | (không native) | (đang khảo sát) | SD-JWT | **SD-JWT (IETF draft)** |
| Schema registry | On-ledger | On-ledger | Off-chain | Off-chain | On-ledger | Tuỳ plugin | Off-chain (MySQL + chaincode anchor) |
| Trust Registry | On-ledger | Sovrin Foundation | Microsoft graph | (không có) | ESSIF Trusted Issuers Registry | Tuỳ plugin | **On-chain (Fabric chaincode)** |
| OID4VP / OID4VCI | OID4VCI WG Indy profile | (qua Aries) | OID4VP đầy đủ | (không có) | OID4VCI đầy đủ | OID4VP + OID4VCI | OID4VP cơ bản (jwt_vp HMAC) |
| Ngôn ngữ chính | Python, Rust, Go | Rust | C#, TypeScript | Node.js | Node.js, Java | TypeScript | **Kotlin / Dart / TypeScript** |
| Governance | Linux Foundation | Sovrin Foundation | Microsoft | DIF | EU Commission | Veramo (Consensys Mesh) | (POC học thuật) |
| Production maturity | High (BC Gov, Trinsic, esatus) | High | High (Entra ID) | Beta | Pilot (Member States) | Library only | POC |

### 1.5.2. Phân tích positioning của TrustID

**So với Indy/Sovrin** — lựa chọn "chính thống" cho SSI nhưng có 3 nhược điểm với mục tiêu của đồ án:
- AnonCreds (CL signature) phụ thuộc nặng vào ZKP libraries (`libindy`/`anoncreds-rs`), khó debug và thiếu hỗ trợ mobile native (đặc biệt iOS).
- Sovrin Foundation governance phù hợp cho cross-organization scale, không phù hợp với một doanh nghiệp đơn lẻ làm Issuer minh hoạ.
- Indy ledger không hỗ trợ smart contract đa năng — chỉ định nghĩa sẵn các transaction cho DID, schema, credential definition; khó mở rộng cho HRMS use case (audit log, e-sign contract, payroll attestation).

→ TrustID chọn **Fabric chaincode Java** để tận dụng smart contract programmable, mở rộng dễ cho use case workplace credentials và e-sign hợp đồng.

**So với Microsoft Entra Verified ID** — production-ready với SDK đầy đủ, nhưng:
- Phụ thuộc Microsoft Azure (vendor lock-in), không phù hợp tự chủ.
- Closed source, không thể audit nội bộ thuật toán ký / quy trình revocation.
- Học thuật khó chứng minh đóng góp nếu chỉ wrap SDK.

→ TrustID self-hosted, mã nguồn mở (đồ án), audit được toàn bộ luồng từ chaincode đến Verifier portal.

**So với ION** — DPKI trên Bitcoin Sidetree tốt cho phi tập trung tối đa, nhưng:
- Tốc độ ghi rất chậm (~10 phút / block Bitcoin) → không phù hợp với HRMS yêu cầu cấp/thu hồi credential gần realtime.
- Phụ thuộc Bitcoin mainnet (transaction fee biến động) — không phù hợp cho doanh nghiệp khép kín.

**So với EBSI** — kiến trúc tương đồng nhất với TrustID (permissioned + JWT VC + Status List), nhưng EBSI là hạ tầng EU public sector, không thể tái sử dụng cho enterprise tự host.

**So với Veramo** — framework TypeScript linh hoạt, nhưng chỉ là library — không định nghĩa ledger và trust registry cụ thể. TrustID đóng vai trò "full-stack reference implementation" trên một ledger xác định.

### 1.5.3. Khoảng trống nghiên cứu (research gap) mà TrustID hướng tới

1. **Reference implementation đầy đủ cho SSI trên Hyperledger Fabric** — tài liệu và dự án mở hiện có rất ít so với hệ Indy/Aries (hầu hết Fabric tutorial chỉ dừng ở asset-transfer).
2. **Kết hợp hai chuẩn revocation + selective disclosure W3C/IETF**: Status List 2021 (W3C) + SD-JWT (IETF) — khác với cách Indy gói cả hai vào AnonCreds. Cách tiếp cận này dễ interop với các Verifier không phải-Indy hơn.
3. **HRMS như Issuer minh hoạ** — đa số nghiên cứu SSI dùng healthcare/education (vd. Dunphy & Petitcolas 2018, Mühle et al. 2018); workplace credentials kết hợp e-sign contract + payroll attestation là use case ít được khảo sát.
4. **Mobile-first wallet với biometric gate ECDSA** — nhiều SSI demo còn dùng web wallet hoặc browser extension; TrustID là Flutter native, private key sinh và lưu trong Secure Enclave / Keystore, biometric gate trước mỗi lần ký VP.

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
- **Framework**: Spring Boot 4.0.5 + Kotlin 2.2.21 (port 8080, base path `/api/v1`)
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
- **Framework**: Vite 6 + React 18 + TypeScript + Tailwind CSS 3
- **Đặc điểm**: SPA độc lập, KHÔNG cần đăng nhập
- **Chức năng**: Paste VC / SD-JWT → verify chữ ký + check Status List on-chain → hiển thị trusted issuers
- **Dev URL**: http://localhost:5173 (proxy `/api` và `/1.0` → backend)

### 4.4 Blockchain Layer (fabric-network)
- **Hyperledger Fabric 2.x**
- **Chaincode**: Java — class chính `IdentityLedger.java`
  - Tên chaincode khi deploy: **`identity-ledger`** (`fabric.chaincode-name` trong `application.yml`)
  - Thư mục mã nguồn vẫn là `chaincode/asset-transfer/` và package `org.hyperledger.fabric.samples` — di sản từ scaffold sample của Fabric, KHÔNG phải tên chaincode trên ledger
- **Network topology**:
  - 1 Orderer
  - 2 Peers (Org1)
  - 1 CA (Org1 CA)
  - Channel: `mychannel`
- **Triển khai**: backend kết nối peer qua Fabric Gateway; crypto material (cert/key của `User1@org1`) đặt trong WSL (xem section 16)

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

### Phase 3 — Interoperability, Notarization & Org Onboarding
| Tính năng | Chuẩn / Endpoint | Mô tả |
|---|---|---|
| **DIDComm Messaging** | DIDComm (PoC) — `/didcomm/{send,inbox/{did},inbox/{did}/mark-read}` | Kênh nhắn tin peer-to-peer giữa các DID (vd. gửi `credential-offer`). Message có `type` URI + `fromDid`/`toDid` + body JSON, lưu inbox theo DID. **PoC: lưu plaintext trong MySQL, chưa mã hoá** — production cần DIDComm v2 (ECDH-1PU authcrypt, JWM envelope) |
| **Document Notarization** | `/api/v1/notarization/{notarize,verify,{docId}}` | Upload file bất kỳ (multipart, ≤50MB) → SHA-256 → anchor lên Fabric qua `upsertNotarizationRecord` (recordType=DOCUMENT). Verify bằng cách gửi lại file (hoặc base64) → backend recompute hash, đối chiếu on-chain. GET `{docId}` public (không cần auth) |
| **Company (Issuer) Onboarding** | `/api/v1/company` (GET/POST/PUT) | Hồ sơ pháp lý của tổ chức Issuer: taxCode, companyName, người đại diện pháp luật (tên/chức danh/CCCD), địa chỉ, ngày đăng ký. Là **một company duy nhất** (nhất quán single-Org), không phải multi-tenant |
| **Employee Directory** | `/api/v1/directory` (list + `{id}`) | Danh bạ nhân sự trong tổ chức: tên, email, phòng ban, chức vụ, role, status |

> **Lưu ý positioning**: Notarization là use case blockchain **độc lập với HRMS** (anchor hash tài liệu bất kỳ) — minh hoạ giá trị Verifiable Data Registry rộng hơn workplace credentials. DIDComm là chuẩn SSI quan trọng cho luồng credential-offer/request giữa Issuer–Holder–Verifier.

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

## 12.5. Mô hình mối đe doạ (Threat Model — STRIDE + DREAD)

Phần này phân tích mối đe doạ theo phương pháp **STRIDE** (Microsoft) kết hợp đánh giá rủi ro **DREAD**, áp dụng riêng cho từng component của TrustID.

### 12.5.1. Phân tích STRIDE theo component

| Component | Spoofing | Tampering | Repudiation | Information Disclosure | DoS | Elevation of Privilege |
|---|---|---|---|---|---|---|
| **Issuer (Spring Backend)** | Giả mạo issuer key (HMAC secret) | Sửa VC trước khi ký | Issuer phủ nhận đã cấp VC | Lộ `vc.secret` / `sd-jwt.secret` | Flood `/auth/sign-in` | JWT token forgery do JWT secret yếu |
| **Holder (Flutter app)** | Cài app giả mạo (sideload) | Sửa VC trong Secure Storage | Holder phủ nhận đã present VP | Lộ private key ECDSA P-256 | Lock device qua biometric fail repeated | Bypass biometric gate (root/jailbreak) |
| **Verifier Portal** | Phishing portal giả `verify-michikuni.cloud` | Sửa kết quả verify hiển thị | — | Log toàn bộ VC nhận trong console | XSS / CSP bypass | — |
| **Fabric Ledger** | MSP cert giả của Org1 | Endorsement collusion (single Org) | — | Đọc trust registry không cần phép | Peer DoS (single peer) | Chaincode privilege escalation |
| **Network / Transport** | DNS poisoning trên michikuni.cloud | MITM TLS strip | — | Sniff JWT trong query string nếu lỡ log | TLS handshake flood | — |

### 12.5.2. Top 7 attack scenarios với DREAD score

**Thang điểm DREAD** (mỗi tiêu chí 1-10, 1 = thấp, 10 = cao). Tổng = trung bình cộng 5 tiêu chí.

| # | Attack scenario | D | R | E | A | D | **Score** | Mitigation hiện có |
|---|---|---|---|---|---|---|---|---|
| 1 | **VC/VP replay** — Verifier nhận lại VP cũ và verify thành công | 7 | 9 | 7 | 6 | 8 | **7.4** | OID4VP `nonce` + `state` per session (300s expiry); `VpSessionStore` check trước khi verify |
| 2 | **Issuer HMAC secret leak** — `vc.secret` bị lộ → forge mọi VC | 10 | 3 | 4 | 10 | 2 | **5.8** | Env var `${VC_SECRET}`, không commit; cần migrate sang Ed25519/ECDSA (hạn chế đã ghi nhận) |
| 3 | **Status List tampering** — sửa bitstring để un-revoke VC | 9 | 2 | 3 | 7 | 3 | **4.8** | Bất biến trên Fabric (endorsement policy Org1MSP); audit qua `GetRecordHistory` chaincode |
| 4 | **Holder device theft** — kẻ tấn công có thiết bị có VC + private key | 6 | 8 | 5 | 1 | 9 | **5.8** | Biometric gate `local_auth` + `flutter_secure_storage` (Keystore/Keychain); device binding + Sessions controller |
| 5 | **JWT token theft** (network sniff / log file) | 7 | 7 | 6 | 4 | 7 | **6.2** | HTTPS bắt buộc, JWT 24h expiry, Sessions controller cho phép logout per device |
| 6 | **SD-JWT salt prediction** — đoán salt để brute force claim ẩn | 8 | 2 | 2 | 5 | 4 | **4.2** | Salt ngẫu nhiên 128-bit per disclosure; `sd-jwt.secret` riêng biệt |
| 7 | **Brute force / credential stuffing** trên `/auth/sign-in` | 5 | 9 | 8 | 7 | 7 | **7.2** | Bucket4j rate limit 10 req/min + account lockout 5 lần / 15 phút + MFA TOTP (AAL2) |

**Top risk**: Threat 1 (VP replay, 7.4) và Threat 7 (brute force auth, 7.2) — cả hai đã có mitigation; còn Threat 2 (HMAC secret, 5.8) là hạn chế kiến trúc cần migrate dài hạn.

### 12.5.3. Threat 1 (VP Replay) — Sequence chi tiết

```
Holder (legit)        Verifier              Attacker (MITM)
     │                    │                       │
     │── VP token T1 ────►│                       │
     │                    │── verify OK ──────────│
     │                    │                       │
     │                    │ (attacker captures T1)│
     │                    │                       │
     │                    │◄─── replay T1 ────────│
     │                    │── nonce mismatch? ───→│
     │                    │    YES → reject       │
     │                    │    (session đã consumed hoặc expired 300s)
```

**Mitigation đã áp dụng trong code**:
1. `Oidc4VpController.createVpRequest()` sinh `state` + `nonce` ngẫu nhiên, lưu `VpSessionStore` (timeout 300s).
2. `vpService.verifyVpToken(vpToken, session)` đối chiếu `nonce` trong VP với `session.nonce`.
3. `sessionStore.submitVp` chỉ chấp nhận 1 lần; sau khi consume thì session marked.

→ Replay sau 5 phút HOẶC sang Verifier khác (nonce mismatch) → reject.

### 12.5.4. Threat 2 (HMAC Secret Leak) — Lý do và Mitigation roadmap

**Lý do POC chọn HMAC-SHA256**:
- Đơn giản hoá: không cần keypair management cho Issuer trong giai đoạn POC.
- Verify nhanh: 1 hàm hash đối xứng, không cần Universal Resolver lookup public key.
- Phù hợp single-Org demo (Org1 là Issuer duy nhất).

**Hệ luỵ bảo mật**:
- Một secret duy nhất → leak là forge được mọi VC quá khứ và tương lai (không revocable retroactively).
- Không phù hợp cross-organization (mọi Org phải biết secret = vi phạm nguyên tắc phi tập trung).
- Không đạt non-repudiation theo chuẩn W3C VC (W3C khuyến nghị ECDSA / Ed25519 / BBS+).

**Roadmap migrate sang asymmetric**:
1. Sinh keypair Ed25519 cho Issuer; public key publish lên DID Document trên Fabric (`did:fabric:trustid:org1`).
2. Đổi `VcIssuerService.sign()` từ HMAC sang Ed25519 (libsodium hoặc Bouncy Castle).
3. Update `VcVerifierService` đọc public key từ DID Document trước khi verify (gọi qua DIF Universal Resolver có sẵn).
4. Migration plan cho VC cũ: dual-sign HMAC + Ed25519 trong giai đoạn chuyển tiếp (3-6 tháng), sau đó deprecate HMAC.

### 12.5.5. Compliance mapping

| Yêu cầu | Threat liên quan | Mitigation trong TrustID |
|---|---|---|
| GDPR Art.17 (Right to Erasure) | Threat 4 (device theft → PII leak) | Xoá PII trong MySQL; hash trên Fabric không reverse được |
| GDPR Art.20 (Data Portability) | — | Endpoint `/api/v1/gdpr/export` trả JSON đầy đủ của user |
| OWASP A07 (Identification & Auth Failures) | Threat 5, 7 | MFA TOTP + Bucket4j rate limit + account lockout |
| OWASP A02 (Cryptographic Failures) | Threat 2, 6 | HMAC-SHA256 là điểm yếu cần migrate (đã ghi nhận) |
| OWASP A03 (Injection) | — | JPA prepared statements; không có raw SQL |
| NIST SP 800-63-3 AAL2 | Threat 5, 7 | MFA TOTP đạt AAL2 (multi-factor cryptographic software) |
| W3C VC Data Model — Integrity | Threat 1, 3 | Status List 2021 + chaincode `VerifyRecord` đối chiếu hash |

---

## 12.6. Đánh giá hiệu năng (Performance Evaluation)

Phần này định nghĩa **methodology** đo hiệu năng và **bảng template** để điền kết quả thực nghiệm. Số liệu thực tế phải được đo trên môi trường triển khai cụ thể và điền vào cột "Đo được"; **không bịa số**.

### 12.6.1. Methodology

**Môi trường đo** — cần ghi lại đầy đủ trong báo cáo:
- CPU: __ core / __ model / __ GHz
- RAM: __ GB
- Disk: SSD / HDD, __ GB free
- Network: localhost / LAN gigabit / WAN
- JVM: phiên bản, heap size, GC algorithm
- MySQL: phiên bản, innodb_buffer_pool_size
- Fabric: số peer / orderer / batch size / batch timeout

**Công cụ đề xuất**:

| Loại test | Tool | Lý do chọn |
|---|---|---|
| HTTP load (REST API) | **k6** (Grafana) hoặc Apache JMeter | Script TypeScript / JMX, output JSON metrics |
| Fabric chaincode | **Hyperledger Caliper** | Chuẩn benchmark Fabric, đo TPS endorsement + commit riêng biệt |
| Mobile e2e (Flutter) | Integration test + `Stopwatch` | Đo verify on-device (không qua network) |
| Network payload | Wireshark / Charles Proxy | Đo kích thước VC, VP, SD-JWT |

**Metrics**:
- **Latency**: p50 / p95 / p99 (ms)
- **Throughput**: requests per second (RPS) hoặc transactions per second (TPS)
- **Error rate**: % HTTP 5xx hoặc chaincode endorsement fail
- **Resource**: CPU %, memory MB, disk I/O MB/s
- **Payload size**: bytes của VC, VP, SD-JWT presentation

**Workload patterns**:
1. **Steady load**: N concurrent users gửi đều trong T phút.
2. **Spike test**: tăng đột ngột từ baseline lên N×5 trong 30s rồi giảm.
3. **Endurance**: load thấp ổn định trong 4–8 giờ để phát hiện memory leak và Outbox backpressure.

### 12.6.2. Bảng kết quả — REST API (k6 / JMeter)

> **Baseline expected** lấy từ Sukhwani et al. (2017), Thakkar et al. (2018) cho Fabric Gateway, kết hợp ước lượng dựa trên thuật toán (HMAC < 1ms, JWT decode < 5ms). Đây chỉ là tham chiếu lý thuyết, KHÔNG phải số TrustID thực tế — cột "Đo được" cần điền sau khi chạy benchmark.

| # | Test case | Endpoint | Load | Baseline p95 (expected) | **Đo được p95** |
|---|---|---|---|---|---|
| 1 | Auth sign-in | `POST /api/v1/auth/sign-in` | 100 RPS × 60s | < 200ms | ___ |
| 2 | MFA validate | `POST /api/v1/mfa/validate` | 50 RPS × 60s | < 250ms | ___ |
| 3 | VC issue (Employment) | `POST /api/v1/admin/employees/{id}/issue` | 20 RPS × 60s | < 800ms (Fabric submit) | ___ |
| 4 | VC verify (HMAC) | `POST /api/v1/identity/vc/verify` | 200 RPS × 60s | < 30ms | ___ |
| 5 | Status List fetch | `GET /api/v1/status-list/{id}` | 200 RPS × 60s | < 80ms (~16KB bitstring) | ___ |
| 6 | SD-JWT issue (Skill) | `POST /api/v1/sd-jwt/issue/skill/{id}` | 20 RPS × 60s | < 600ms | ___ |
| 7 | SD-JWT verify | `POST /api/v1/sd-jwt/verify` | 100 RPS × 60s | < 50ms | ___ |
| 8 | OID4VP request | `POST /api/v1/oidc/vp/request` | 50 RPS × 60s | < 100ms | ___ |
| 9 | OID4VP submit | `POST /api/v1/oidc/vp/submit` | 50 RPS × 60s | < 150ms | ___ |
| 10 | DIF Universal Resolver | `GET /1.0/identifiers/{did}` | 100 RPS × 60s | < 300ms (Fabric evaluate) | ___ |
| 11 | Trust Registry list | `GET /api/v1/trust-registry/issuers` | 100 RPS × 60s | < 200ms | ___ |
| 12 | Ledger record write | `POST /api/v1/ledger/records` | 10 RPS × 60s | 2000–5000ms (Fabric submit) | ___ |
| 13 | Ledger record read | `GET /api/v1/ledger/records/{id}/{type}` | 100 RPS × 60s | < 200ms (Fabric evaluate) | ___ |

### 12.6.3. Bảng kết quả — Fabric chaincode (Caliper)

| # | Transaction | Type | Send rate | Baseline (TPS / Latency) | **Đo được** |
|---|---|---|---|---|---|
| 1 | `RegisterDID` | SUBMIT | 10 TPS | 8–15 TPS / 2–4s | ___ |
| 2 | `UpsertRecord` | SUBMIT | 10 TPS | 8–15 TPS / 2–4s | ___ |
| 3 | `UpdateStatusListEntry` | SUBMIT | 5 TPS | 5–10 TPS / 2–5s | ___ |
| 4 | `GetRecord` | EVALUATE | 200 TPS | 150–300 TPS / <100ms | ___ |
| 5 | `GetRecordHistory` | EVALUATE | 50 TPS | 30–80 TPS / 100–500ms | ___ |
| 6 | `IsTrustedIssuer` | EVALUATE | 200 TPS | 200–400 TPS / <50ms | ___ |
| 7 | `RecordSignature` | SUBMIT | 5 TPS | 5–10 TPS / 2–5s | ___ |

### 12.6.4. Bảng kết quả — Mobile (Flutter)

| # | Operation | Thiết bị | Lần đo | Baseline expected | **Đo được (avg)** |
|---|---|---|---|---|---|
| 1 | App cold start → Wallet tab | Mid-range Android | 10 | < 3s | ___ |
| 2 | ECDSA P-256 keypair gen (pointycastle) | — | 10 | 50–200ms | ___ |
| 3 | Biometric unlock prompt | local_auth | 10 | 1–2s (user input dominant) | ___ |
| 4 | Build VP from VC (ECDSA sign) | — | 10 | < 100ms | ___ |
| 5 | SD-JWT selective disclosure (chọn 3/10 claims) | — | 10 | < 200ms | ___ |
| 6 | QR encode VP (qr_flutter) | — | 10 | < 100ms | ___ |
| 7 | QR scan + decode (mobile_scanner) | — | 10 | < 500ms | ___ |
| 8 | Offline VC load (Hive cache) | — | 10 | < 50ms | ___ |
| 9 | Online VC fetch (Dio + JWT) | LAN | 10 | < 500ms | ___ |

### 12.6.5. Bảng kết quả — Payload size

| Loại payload | Format | Baseline expected | **Đo được (bytes)** |
|---|---|---|---|
| EmploymentVC | JWT | 800–1500 | ___ |
| SkillVC SD-JWT (10 claims) | JWT + disclosures | 2000–4000 | ___ |
| VP token (W3C VP) | JWT VP | 1500–3000 | ___ |
| Status List 2021 response | JSON-LD + GZIP bitstring | ~17 KB | ___ |
| QR code chứa VP | PNG (ECC level M) | < 50 KB | ___ |
| DID Document | JSON-LD | 500–1200 | ___ |

### 12.6.6. Phân tích kết quả (template để discuss sau khi đo)

Sau khi điền số, phần Discussion cần trả lời:

1. **Bottleneck ở đâu?** — Fabric submit (block creation ~2s với BatchTimeout mặc định) hay HMAC verify (sub-ms)? Vẽ flame graph nếu có thể.
2. **Outbox retry có tạo backpressure không?** — đo TPS endpoint write với và không có Outbox enabled, so sánh.
3. **So sánh với Indy/Sovrin** (Bhattacharya et al. 2019): Indy AnonCreds verify ~200ms vs TrustID HMAC verify <30ms — đánh đổi với security level (HMAC < Ed25519 < BBS+ về non-repudiation).
4. **Scalability ngưỡng**: bao nhiêu employee trên Fabric trước khi Status List bitstring chạm 131072 entry? Cần shard ra sao?
5. **Mobile performance** trên thiết bị low-end (Android 8, 2GB RAM): có usable không?

### 12.6.7. References cho phần đánh giá hiệu năng

| # | Source |
|---|---|
| P1 | Sukhwani, H., Martínez, J. M., Chang, X., Trivedi, K. S., & Rindos, A. (2017). "Performance Modeling of Hyperledger Fabric (Permissioned Blockchain Network)". *IEEE 16th International Symposium on Network Computing and Applications (NCA)*. |
| P2 | Thakkar, P., Nathan, S., & Viswanathan, B. (2018). "Performance Benchmarking and Optimizing Hyperledger Fabric Blockchain Platform". *IEEE 26th International Symposium on Modeling, Analysis, and Simulation of Computer and Telecommunication Systems (MASCOTS)*. |
| P3 | Bhattacharya, M. P., Zavarsky, P., & Butakov, S. (2020). "Enhancing the Security and Privacy of Self-Sovereign Identities on Hyperledger Indy Blockchain". *International Symposium on Networks, Computers and Communications (ISNCC)*. |
| P4 | Hyperledger Caliper documentation — https://hyperledger.github.io/caliper/ |
| P5 | k6 documentation — https://k6.io/docs/ |
| P6 | Dunphy, P., & Petitcolas, F. A. (2018). "A First Look at Identity Management Schemes on the Blockchain". *IEEE Security & Privacy*. |
| P7 | Mühle, A., Grüner, A., Gayvoronskaya, T., & Meinel, C. (2018). "A Survey on Essential Components of a Self-Sovereign Identity". *Computer Science Review*. |

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
- **OID4VP** đã triển khai flow cơ bản (request/submit/result + discovery `/.well-known/openid-configuration`), nhưng chỉ hỗ trợ `jwt_vp` với HMAC-SHA256, chưa hỗ trợ `ldp_vp` / SD-JWT VC trong VP. **OID4VCI** (issuance) chưa triển khai.
- Status List size cố định 131072 → cần cơ chế shard cho scale lớn hơn.
- `spring.jpa.hibernate.ddl-auto=update` cho dev — production cần Flyway/Liquibase migration.
- Chưa có Zero-Knowledge Proof đầy đủ (BBS+ signatures, AnonCreds).
- Trust Registry chưa có UI cho Admin đăng ký/thu hồi issuer từ mobile.
- **DIDComm Messaging mới ở mức PoC**: message lưu plaintext trong MySQL, chưa mã hoá theo DIDComm v2 (ECDH-1PU authcrypt, JWM envelope); `fromDid` chưa verify chặt với JWT principal.

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
│           ├── company/               # Hồ sơ tổ chức Issuer
│           ├── directory/             # Danh bạ nhân sự
│           ├── workplace/             # Hub gom HRMS use-cases
│           ├── onboarding/ cccd/      # Onboarding + CCCD
│           ├── attendance/ requests/ payroll/ chief/ manager/
│           └── profile/               # GDPR Privacy
│   (controller backend tương ứng: 25 controller, gồm DIDComm + Notarization)
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

### DIDComm Messaging
| Method | Path | Mô tả |
|---|---|---|
| POST | `/didcomm/send` | Gửi message từ `fromDid` → `toDid` (type URI + body JSON) |
| GET | `/didcomm/inbox/{did}` | Inbox của DID (mặc định `unreadOnly=true`) |
| POST | `/didcomm/inbox/{did}/mark-read` | Đánh dấu đã đọc toàn bộ |

### Notarization
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/notarization/notarize` | Upload file → anchor SHA-256 lên Fabric (trả `docId`) |
| POST | `/api/v1/notarization/verify` | Đối chiếu hash file gửi lại với on-chain |
| GET | `/api/v1/notarization/{docId}` | Đọc metadata notarization (public) |

### Company & Directory
| Method | Path | Mô tả |
|---|---|---|
| GET / POST / PUT | `/api/v1/company` | Hồ sơ tổ chức Issuer |
| GET | `/api/v1/directory` / `/api/v1/directory/{id}` | Danh bạ nhân sự |

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
```

> Các khoá `vc.*`, `sd-jwt.*`, `jwt.*`, `spring.datasource.*` nằm trong `application.properties`. Riêng cấu hình Fabric nằm trong **`application.yml`** (kebab-case), bind qua `org.fabric.api.config.FabricProperties`:

```yaml
# application.yml — Fabric Gateway
fabric:
  msp-id: Org1MSP
  channel-name: mychannel
  chaincode-name: identity-ledger        # tên chaincode trên ledger (KHÔNG phải "asset-transfer")
  peer:
    endpoint: localhost:7051
    tls-cert-path: /home/<user>/identity-fabric/fabric-network/.../peer0.org1.example.com/tls/ca.crt
  gateway:
    cert-path: /home/<user>/identity-fabric/fabric-network/.../User1@org1.example.com/msp/signcerts/...-cert.pem
    key-path:  /home/<user>/identity-fabric/fabric-network/.../User1@org1.example.com/msp/keystore/
```

> **Lưu ý môi trường**: đường dẫn crypto material là tuyệt đối trong **WSL** (`/home/<user>/...`) vì Fabric chạy bằng Docker trên WSL, còn backend có thể chạy trên Windows host. Khi đổi máy phải sửa lại các path này.
>
> **Bảo mật**: `jwt.secret` hiện đang hard-code plaintext trong `application.properties` (không phải env var) — cần chuyển sang biến môi trường trước production (xem Threat Model 12.5, Threat 5).

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
| 10 | OpenID for Verifiable Credential Issuance (OID4VCI) — https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html |
| 11 | OpenID for Verifiable Presentations (OID4VP) — https://openid.net/specs/openid-4-verifiable-presentations-1_0.html |
| 12 | Hyperledger Indy / Aries Project — https://www.hyperledger.org/projects/aries |
| 13 | Sovrin Foundation Governance Framework — https://sovrin.org/library/sovrin-governance-framework/ |
| 14 | Microsoft Entra Verified ID — https://learn.microsoft.com/en-us/entra/verified-id/ |
| 15 | DIF ION (Sidetree on Bitcoin) — https://identity.foundation/ion/ |
| 16 | EBSI (European Blockchain Services Infrastructure) — https://ec.europa.eu/digital-building-blocks/sites/display/EBSI/ |
| 17 | Veramo Framework — https://veramo.io/ |
| 18 | Sukhwani, H. et al. (2017). "Performance Modeling of Hyperledger Fabric". *IEEE NCA*. |
| 19 | Thakkar, P. et al. (2018). "Performance Benchmarking and Optimizing Hyperledger Fabric". *IEEE MASCOTS*. |
| 20 | Bhattacharya, M. P. et al. (2020). "Enhancing the Security and Privacy of Self-Sovereign Identities on Hyperledger Indy". *ISNCC*. |
| 21 | Dunphy, P., & Petitcolas, F. A. (2018). "A First Look at Identity Management Schemes on the Blockchain". *IEEE Security & Privacy*. |
| 22 | Mühle, A. et al. (2018). "A Survey on Essential Components of a Self-Sovereign Identity". *Computer Science Review*. |
| 23 | Microsoft STRIDE Threat Model — https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats |
| 24 | OWASP Top 10 (2021) — https://owasp.org/Top10/ |

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

*File context này được cập nhật ngày 2026-05-30 (bổ sung Phase 3: DIDComm Messaging, Document Notarization, Company Onboarding, Directory; sửa tên chaincode `identity-ledger`, Vite 6, cấu hình Fabric trong application.yml/WSL). Lần cập nhật trước 2026-05-23 bổ sung Related Work, Threat Model STRIDE+DREAD, Performance Evaluation methodology. Khi dự án thay đổi đáng kể (thêm phase mới, đổi stack, …), cần cập nhật lại trước khi dùng cho prompt mới.*

**Lưu ý quan trọng cho phần Performance Evaluation (section 12.6)**: các bảng template hiện có cột "Đo được" để trống. Trước khi bảo vệ, **bắt buộc** chạy benchmark (k6 + Caliper) và điền số thật vào — không được để Claude bịa số liệu. Baseline expected chỉ là tham chiếu lý thuyết từ paper, không phải số TrustID.
