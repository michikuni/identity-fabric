# Roadmap bổ sung tính năng TrustID — Định hướng SSI Platform

> Tài liệu lên ý tưởng bổ sung tính năng cho đồ án bảo vệ tốt nghiệp. Định hướng: **Self-Sovereign Identity platform** (không phải HRMS thuần).

---

## 1. Hiện trạng

TrustID đã có đầy đủ chức năng của một HRMS có blockchain audit:

**Backend** (Spring Boot + Kotlin):
- Auth (JWT, 4 role: EMPLOYEE / MANAGER / ADMIN / CHIEF)
- Employee / Profile / Contract / Payroll / Attendance / Request / Company CRUD
- Admin Dashboard, approve account, issue VC
- `IdentityLedgerBridge` — outbox pattern async write Fabric, retry exponential backoff
- DID register/revoke (`did:fabric:trustid:{empId}` + ECDSA P-256 JWK)
- VC Issuance: EmploymentVC, SalaryRangeVC, PromotionVC, TerminationVC (HMAC-SHA256 PoC)
- OID4VP flow

**Frontend** (Flutter):
- Sign-in/up + onboarding với CCCD scan (QR chip)
- Home / Profile / Contract / Payroll / Wallet / Attendance / Requests / Directory / Company
- LedgerScreen, VerifierScanScreen
- Wallet ECDSA P-256 (PointyCastle)
- i18n EN/VI, push notification

**Blockchain** (Hyperledger Fabric):
- Chaincode Java `identity-ledger`: UpsertRecord, GetRecord, GetRecordHistory, VerifyRecord, RegisterDID, RevokeDID, ResolveDID
- Privacy-first: SHA-256 hash + keyFields on-chain, full data off-chain

---

## 2. Gap phân tích — Vì sao cần bổ sung

Hệ thống hiện nghiêng nhiều về **HRMS có blockchain audit** hơn là **SSI platform** thực thụ. Để thuyết phục hội đồng đồ án về định hướng SSI, cần bổ sung các khối kỹ thuật cốt lõi:

- **VC Revocation** (Status List 2021) — hội đồng chắc chắn sẽ hỏi
- **Selective Disclosure** (SD-JWT) — kỹ thuật signature của SSI hiện đại
- **External Verifier** độc lập — chứng minh kiến trúc 3 bên (Issuer / Holder / Verifier)
- **Trust Registry** + **DID Resolver chuẩn DIF** — chứng minh tuân thủ chuẩn quốc tế
- **Biometric-bound private key** — kể câu chuyện "khóa nằm trong tay người dùng"
- **E-sign contract on-chain** — moment quyết định trong demo

---

## 3. Phase 0 — Restructure HRMS → SSI (làm TRƯỚC Phase 1)

> Chi phí thấp nhất, hiệu quả cao nhất. Đổi cách trình bày để app trông như **SSI platform** chứ không phải HRMS có blockchain. **Không xóa code**, chỉ ẩn / đổi tên / đổi vị trí.

### 3.1. Restructure bottom navigation — Đẩy SSI ra trước [Effort: S]

**Vấn đề**: Bottom nav hiện đặt HRMS feature ra trước → mở app thấy ngay HRMS, không thấy SSI.

**Mục tiêu**: Wallet / Credentials / Verifier / Ledger ra primary, HRMS chuyển vào tab phụ "Workplace".

**Trước**:
```
[Home] [Attendance] [Requests] [Directory] [Profile]
```

**Sau**:
```
[Wallet] [Credentials] [Verifier] [Ledger] [Profile]
                                              └─ Workplace submenu:
                                                 - Attendance
                                                 - Requests
                                                 - Directory
                                                 - Company Info
```

**Files**:
- Sửa: [identity_frontend/lib/routing/app_router.dart](identity_frontend/lib/routing/app_router.dart) — đổi thứ tự ShellRoute + thêm nhánh `/workplace/*` cho HRMS features
- Sửa file ShellRoute config / bottom nav widget — đổi item order theo bảng trên
- Sửa role-based nav: cả role EMPLOYEE / MANAGER / CHIEF / ADMIN đều phải đẩy SSI ra trước

### 3.2. Đổi tên khái niệm (i18n) [Effort: S]

**Vấn đề**: Label "Admin Dashboard", "Employee", "Approve account" nghe rất HR — hội đồng nhận diện app là HRMS ngay khi đọc menu.

**Mapping cần đổi trong arb**:

| Cũ (HRMS) | Mới (SSI) |
|---|---|
| Admin Dashboard | Issuer Console |
| Employee list | Credential Subjects |
| Approve account | Enroll & Issue Credential |
| Reject account | Reject Enrollment |
| Terminate | Revoke Credentials |
| Profile | Identity Attributes |
| Ledger | Verifiable Records |
| Wallet | Credential Wallet |
| Verifier Scan | Present Credential |

**Files**:
- Sửa: [identity_frontend/lib/l10n/app_vi.arb](identity_frontend/lib/l10n/app_vi.arb)
- Sửa: [identity_frontend/lib/l10n/app_en.arb](identity_frontend/lib/l10n/app_en.arb)

### 3.3. Polish Admin Dashboard → Issuer Console [Effort: S]

**Vấn đề**: Admin Dashboard hiện hiển thị KPI HR (total/active employees/attendance/pending) → trông giống HR dashboard.

**Mục tiêu**: KPI SSI lên đầu (chứng minh app phát hành credential), KPI HR đẩy xuống dưới hoặc ẩn.

**KPI mới hiển thị trên đầu Issuer Console**:
- **Total Credentials Issued** (Employment + Salary + Promotion + Skill + ...)
- **Active DIDs**
- **Revoked Credentials this month**
- **Trust Registry** — số issuer được tin tưởng
- **Verifier API calls / 24h** — bao nhiêu lần VC được verify bởi bên thứ ba

**Files**:
- Sửa: `identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart`
- Mới (backend endpoint): `GET /api/v1/admin/issuer-stats` ở [AdminController](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AdminController.kt)

### 3.4. Restructure demo flow narrative [Effort: S]

**Vấn đề**: Nếu mở demo bằng Attendance/Request, hội đồng định hình "app chấm công có blockchain".

**Mục tiêu**: Demo MỞ ĐẦU bằng Wallet → Credentials → Verifier scan ngay từ phút đầu. HRMS (Attendance/Request) chỉ xuất hiện như "use case minh họa cho Issuer".

**Bài thuyết minh đổi định vị**:
- ❌ Trước: *"TrustID là HRMS có blockchain audit"*
- ✅ Sau: *"TrustID là nền tảng SSI cho workplace credentials. HRMS chỉ là use case minh họa role Issuer."*

**Không cần code** — chỉ là kịch bản trình bày + slide. Đã phản ánh trong Demo Flow ở section 7.

### 3.5. Phân loại chức năng đã có (tham khảo) [Effort: 0]

**🟢 Giữ và làm nổi bật (SSI core)**:
- Auth + DID + Wallet
- VC Issuance (qua Approve)
- Contract + E-sign (sẽ thêm ở 4.7)
- Ledger view
- Verifier Scan

**🟡 Giữ nhưng giảm vai trò (HRMS support SSI)**:
- Employee CRUD — cần làm subject cho VC, nhưng đổi label
- Admin Dashboard → Issuer Console (đã đổi ở 3.3)
- Chief Terminate — vì gắn với revoke flow

**🔴 Ẩn khỏi nav chính, chuyển vào Workplace submenu**:
- Attendance check-in/out + history + team timesheet
- Leave / Advance Request workflow
- Manager approve requests
- Payroll detail (lương, OT, thuế, bank info)
- Directory employee list
- Company info screen

**Quan trọng**: chỉ **ẩn khỏi navigation chính**, **không xóa code**. Hội đồng có thể yêu cầu vào để xem, vẫn truy cập được qua Workplace submenu.

---

## 4. Phase 1 — MUST HAVE (Core SSI features cho demo)

### 4.1. W3C Status List 2021 — VC Revocation [Effort: L]

**Lý do**: Không có cơ chế revoke là điểm yếu chí mạng của SSI. Status List 2021 là chuẩn W3C, dùng bitstring nén để cập nhật trạng thái VC mà không lộ holder.

**Demo storyline**: *"Nhân viên nghỉ việc → Admin click Revoke → Verifier scan VC cũ lập tức thấy REVOKED dù VC chưa hết hạn."*

**Files**:
- Mới: [fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/StatusListService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/StatusListService.kt) — build bitstring, gzip, base64url
- Mới: [fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/StatusListController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/StatusListController.kt) — `GET /api/v1/status-list/{listId}`
- Sửa: [fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) — thêm `UpdateStatusListEntry(listId, index, revoked)`, `GetStatusList(listId)`
- Sửa: [fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt) — mỗi VC được phát thêm field `credentialStatus.statusListIndex`
- Sửa: [identity_frontend/lib/presentation/features/verifier/verifier_scan_screen.dart](identity_frontend/lib/presentation/features/verifier/verifier_scan_screen.dart) — hiển thị badge REVOKED

### 4.2. SD-JWT Selective Disclosure (SkillVC + EducationVC) [Effort: L]

**Lý do**: Là kỹ thuật cốt lõi của SSI hiện đại (IETF draft). Holder chỉ lộ field tối thiểu, vẫn verify được chữ ký issuer. Implement song song với VC JSON-LD hiện tại — không phá code cũ.

**Demo storyline**: *"Xin việc bên ngân hàng → Employee tick chọn 3/10 skill → SD-JWT giấu 7 skill còn lại, Verifier vẫn verify được chữ ký."*

**Files**:
- Mới: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtIssuer.kt` — format `header.payload.signature~disclosure1~disclosure2~...`
- Mới: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtVerifier.kt`
- Mới: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/SdJwtController.kt` — `POST /api/v1/sd-jwt/issue`, `POST /api/v1/sd-jwt/verify`
- Mới: `identity_frontend/lib/core/wallet/sd_jwt_holder.dart` — Holder pick subset disclosure
- Mới: `identity_frontend/lib/presentation/features/wallet/disclosure_picker_screen.dart` — UI tick chọn field

### 4.3. External Verifier Portal (Vite + React) [Effort: L]

**Lý do**: Tách Verifier ra khỏi TrustID app = chứng minh kiến trúc SSI thực thụ (3 bên độc lập). Demo 2 màn hình song song (mobile Holder + web Verifier) tạo hiệu ứng rất mạnh. Chọn **Vite + React** thay vì Next.js vì verifier là SPA thuần — không cần SSR / API routes / server functions của Next.js, bundle nhỏ hơn 30-50% và HMR nhanh hơn.

**Demo storyline**: *"Ngân hàng XYZ mở web verifier riêng, không cần tài khoản trong TrustID, vẫn verify được VC qua QR."*

**Stack**: Vite 5 + React 18 + TypeScript + Tailwind CSS + react-router-dom + @tanstack/react-query + qrcode.react

**Files** (tạo module mới `verifier-portal/`):
- `verifier-portal/src/pages/Home.tsx` — landing + tạo VP Request với QR
- `verifier-portal/src/pages/VerifyResult.tsx` — kết quả verify (VALID / INVALID / REVOKED)
- `verifier-portal/src/pages/TrustRegistry.tsx` — danh sách issuer được tin tưởng
- `verifier-portal/src/lib/trustid-client.ts` — gọi `/api/v1/identity/vc/verify`, `/status-list/*`, `/1.0/identifiers/*`, `/trust-registry/issuers`
- `verifier-portal/src/App.tsx` — react-router-dom config
- `verifier-portal/src/main.tsx` — entry
- `verifier-portal/index.html`
- `verifier-portal/vite.config.ts`
- `verifier-portal/package.json` — Vite 5 + React 18 + TypeScript + Tailwind

**Cấu trúc thư mục**:
```
verifier-portal/
├── src/
│   ├── pages/
│   │   ├── Home.tsx              # Request VP với QR
│   │   ├── VerifyResult.tsx      # Kết quả VALID/INVALID/REVOKED
│   │   └── TrustRegistry.tsx     # Danh sách issuer
│   ├── lib/
│   │   └── trustid-client.ts     # API client gọi backend
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── vite.config.ts
└── package.json
```

**Deploy**: `npm run build` → ra static `dist/` → serve qua nginx, hoặc bỏ vào `fabric-spring-backend/src/main/resources/static/verifier/` nếu muốn dùng chung domain.

### 4.4. DID Resolver chuẩn DIF (Universal Resolver compatible) [Effort: M]

**Lý do**: Chứng minh dự án theo chuẩn quốc tế (DIF spec). Bất kỳ Universal Resolver toàn cầu nào cũng gọi được endpoint này.

**Demo storyline**: *"Đối tác nước ngoài resolve DID của nhân viên qua chuẩn DIF không cần biết internal API của TrustID."*

**Files**:
- Mới: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/UniversalResolverController.kt` — `GET /1.0/identifiers/{did}` trả DID Document JSON-LD với `@context`, `verificationMethod[]`, `service[]`, `didDocumentMetadata.deactivated`, `didResolutionMetadata.contentType`
- Sửa: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/fabric/IdentityLedgerBridge.kt` — bổ sung helper `resolveDid(did)` gọi chaincode `ResolveDID`

### 4.5. Biometric Unlock + Sign-on-touch [Effort: M]

**Lý do**: SSI = "khóa riêng trong tay người dùng". Sinh trắc bảo vệ private key là điểm bán hàng UX cốt lõi, demo trên điện thoại thật cực kỳ thuyết phục.

**Demo storyline**: *"Mỗi lần ký Verifiable Presentation, nhân viên chạm vân tay — chứng minh khóa không thể tách khỏi người dùng."*

**Files**:
- Mới: [identity_frontend/lib/core/security/biometric_service.dart](identity_frontend/lib/core/security/biometric_service.dart) — wrap package `local_auth`
- Sửa: `identity_frontend/lib/core/wallet/wallet_service.dart` — method `sign()` yêu cầu biometric trước khi đọc private key từ `flutter_secure_storage`
- Sửa: `identity_frontend/lib/presentation/features/wallet/wallet_screen.dart` — toggle App Lock + indicator "Protected by Biometric"
- Sửa: [identity_frontend/pubspec.yaml](identity_frontend/pubspec.yaml) — thêm `local_auth: ^2.3.0`

### 4.6. Trust Registry on-chain [Effort: M]

**Lý do**: Verifier cần biết issuer nào trustable. Trust Registry on-chain là pattern chuẩn (eIDAS, EBSI). Governance qua role CHIEF.

**Demo storyline**: *"Verifier check VC → resolve issuer DID → tra Trust Registry → chỉ ACCEPT nếu issuer được liệt kê."*

**Files**:
- Sửa: [fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) — `RegisterIssuer(did, name, role, scope)`, `RevokeIssuer(did)`, `IsTrustedIssuer(did)`, `ListTrustedIssuers()`
- Mới: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/TrustRegistryController.kt` — admin endpoints + public `GET /api/v1/trust-registry/issuers`
- Mới: `verifier-portal/src/pages/TrustRegistry.tsx` — hiển thị danh sách issuer được tin tưởng

### 4.7. E-sign Contract bằng wallet ECDSA [Effort: M]

**Lý do**: Moment quyết định trong demo — nhân viên ký hợp đồng bằng khóa của chính mình (không cần OTP), hash PDF lên Fabric, bất biến.

**Demo storyline**: *"Manager gửi contract PDF → Employee tap Sign (biometric) → P-256 signature + SHA-256 PDF hash lên Fabric → manager thấy SIGNED, timestamp on-chain."*

**Files**:
- Mới: `identity_frontend/lib/presentation/features/contract/contract_sign_screen.dart`
- Mới: `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/ContractSignatureController.kt` — `POST /api/v1/contracts/{id}/sign` nhận signatureBase64 + docHash + signerDid
- Sửa: [fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) — `RecordSignature(contractId, signerDid, signatureBase64, docHash, timestamp)`, `GetSignatures(contractId)`

---

## 5. Phase 2 — SHOULD HAVE (Chiều sâu cho Q&A hội đồng)

### 5.1. TOTP 2FA cho Admin/Chief [Effort: S]
RFC 6238 (Google Authenticator). Files: `infrastructures/security/MfaService.kt`, `presentation/controller/MfaController.kt`, `identity_frontend/lib/presentation/features/auth/mfa_setup_screen.dart`. Lib: `aerogear-otp-java` hoặc `dev.samstevens.totp:totp`.

### 5.2. Audit Log Viewer UI on-chain [Effort: S]
Tận dụng `GetRecordHistory` đã có. File: `identity_frontend/lib/presentation/features/admin/audit_log_screen.dart` — timeline UI hiển thị mọi hành động on-chain (admin xem ai làm gì lúc nào).

### 5.3. Thêm VC types: TrainingVC, NDA-AcceptedVC, SkillVC, EducationVC [Effort: S]
Bổ sung methods vào [VcIssuerService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt). Kết hợp với Status List từ 4.1 và SD-JWT từ 4.2.

### 5.4. Account Lockout + Rate Limiting [Effort: S]
Filter Bucket4j cho `/auth/*` (10 req/min/IP), khóa account sau 5 lần fail. File: `infrastructures/security/RateLimitFilter.kt`.

### 5.5. GDPR Data Export + Right to be Forgotten [Effort: M]
- `GET /api/v1/me/export-data` → trả ZIP JSON tổng hợp toàn bộ data của user
- `DELETE /api/v1/me/data` → soft-delete + revoke tất cả VC + RevokeDID + ghi audit on-chain

### 5.6. Device Binding & Session List [Effort: S]
JWT thêm claim `deviceId`. Lưu `UserSessionJpaEntity(userId, deviceId, deviceName, lastSeen, createdAt)`. Screen `identity_frontend/lib/presentation/features/security/sessions_screen.dart` cho phép logout từng device hoặc all.

---

## 6. Phase 3 — NICE TO HAVE (nếu còn thời gian)

### 6.1. Dark Mode [Effort: S]
Bổ sung `ThemeMode.system` vào app theme. Setting screen toggle Light / Dark / System.

### 6.2. DIDComm Messaging [Effort: L]
Demo peer-DID encrypted channel giữa Issuer ↔ Holder ngoài OID4VP. Không cần full spec, chỉ 2 endpoints `POST /didcomm/send`, `GET /didcomm/inbox/{did}`.

### 6.3. Notarization Service [Effort: S]
Upload bất kỳ file → SHA-256 → `UpsertRecord(recordType=DOCUMENT)` on-chain. File: `presentation/controller/NotarizationController.kt`.

### 6.4. Offline Cache (Hive) + Crashlytics [Effort: S]
Cache VC trong Hive để mở wallet không cần network. Bật `firebase_crashlytics` (đã có Firebase setup từ FCM).

---

## 7. Demo Flow End-to-End (8 bước, ~12 phút cho buổi bảo vệ)

| # | Bước | Thời lượng | Tính năng dùng |
|---|---|---|---|
| 1 | **Onboarding** — Employee scan CCCD QR → Flutter sinh ECDSA P-256 trong SecureStorage → publicKeyJwk gửi backend. Biometric enrollment ngay sau. | 2 phút | 4.5 |
| 2 | **Admin Approve** — Admin (Issuer Console) click Approve → backend gọi chaincode `RegisterDID` + `RegisterIssuer` (org1) + phát hành 3 VC (Employment + SalaryRange + Skill SD-JWT) với `credentialStatus.statusListIndex`. | 1 phút | 4.1, 4.2, 4.6 |
| 3 | **Holder review wallet** — Employee mở app, vân tay mở khóa → thấy 3 VC, badge "Active" lấy từ Status List. | 1 phút | 4.1, 4.5 |
| 4 | **Selective Disclosure** — Verifier Portal (Vite + React) mở web → click "Request Skill Proof" → QR. Employee scan → Disclosure Picker tick 3/10 skill → tap Sign with Biometric → SD-JWT presentation gửi đi. | 2 phút | 4.2, 4.3, 4.5 |
| 5 | **Verifier Verify** — Portal nhận SD-JWT → gọi `/api/v1/identity/vc/verify` → resolve issuer DID qua `/1.0/identifiers/...` → check Trust Registry → check Status List → hiện VALID + 3 skill được lộ. | 1 phút | 4.1, 4.3, 4.4, 4.6 |
| 6 | **E-sign Contract** — Manager push contract PDF → Employee tap Sign (biometric) → signature + hash lên chaincode `RecordSignature` → manager thấy SIGNED, timestamp on-chain. | 1.5 phút | 4.5, 4.7 |
| 7 | **Tamper Test** — Mở MySQL, sửa salary trực tiếp → gọi `/api/v1/ledger/records/.../verify` → backend recompute hash, so on-chain → trả INVALID. | 1 phút | (Đã có) |
| 8 | **Revocation** — Chief click Terminate → chaincode `RevokeDID` + `UpdateStatusListEntry(revoked=true)` + phát hành TerminationVC → Verifier Portal scan lại VP cũ → REVOKED badge đỏ. | 1.5 phút | 4.1, (Đã có) |

---

## 8. Kiến trúc sau khi bổ sung

```
+---------------------------+         +-----------------------------+
|   HOLDER (Flutter App)    |         |   VERIFIER PORTAL           |
|                           |         |   (Vite + React + TS)       |
|---------------------------|         |-----------------------------|
| - Biometric Service [4.5] |         | - QR Request Generator      |
| - Wallet (ECDSA P-256)    |         | - SD-JWT Verifier client    |
| - SD-JWT Holder      [4.2]|         | - Trust Registry lookup     |
| - Disclosure Picker UI    |         | - Status List checker       |
| - Session List       [5.6]|         | (verifier-portal/, SPA)     |
| - Hive Offline Cache [6.4]|         +--------------+--------------+
+-------------+-------------+                        |
              |                                       |
              |  OID4VP / SD-JWT Presentation         |  POST /vc/verify
              +-------+-------------------------------+
                      |
                      v
       +--------------+----------------------+
       |   SPRING BOOT BACKEND (Kotlin)      |
       |-------------------------------------|
       | Presentation:                       |
       |   AuthApi + TOTP 2FA           [5.1]|
       |   IdentityController                |
       |   UniversalResolverController  [4.4]|
       |   StatusListController         [4.1]|
       |   TrustRegistryController      [4.6]|
       |   SdJwtController              [4.2]|
       |   ContractSignatureController  [4.7]|
       |   AuditLogController           [5.2]|
       |   MfaController                [5.1]|
       |-------------------------------------|
       | Infrastructure:                     |
       |   VcIssuerService (existing)        |
       |   SdJwtIssuer / SdJwtVerifier  [4.2]|
       |   StatusListService            [4.1]|
       |   MfaService (TOTP RFC6238)    [5.1]|
       |   RateLimitFilter (Bucket4j)   [5.4]|
       |   IdentityLedgerBridge (outbox)     |
       +-------+------------------------+----+
               |                        |
               | gRPC                   | JDBC
               v                        v
   +-----------+-----------+    +-------+-----------+
   |  HYPERLEDGER FABRIC   |    |  MySQL (PII off-  |
   |  chaincode IdentityLedger  |  chain): Employee |
   |-----------------------|    |  Profile/Contract |
   | UpsertRecord (existing)    |  Payroll/Request  |
   | GetRecordHistory      |    |  Outbox/Audit/    |
   | VerifyRecord (hash)   |    |  UserSession/MFA  |
   | RegisterDID/Resolve   |    +-------------------+
   | RevokeDID             |
   | RegisterIssuer        |<-- Trust Registry [4.6]
   | IsTrustedIssuer       |
   | UpdateStatusListEntry |<-- Status List 2021 [4.1]
   | GetStatusList         |
   | RecordSignature       |<-- E-sign [4.7]
   | GetSignatures         |
   +-----------------------+
```

---

## 11.5. Phase 1 Implementation Status (updated 2026-05-16)

> Ghi lại chính xác những gì **đã được implement** để team frontend có thể dựa vào build UI.

### ✅ 4.1 — W3C Status List 2021 (đã xong trước session này)

| File | Trạng thái |
|---|---|
| `StatusListService.kt` | ✅ Hoàn chỉnh — gzip+base64url bitmap, revoke/activate/isRevoked |
| `StatusListController.kt` | ✅ `GET /api/v1/status-list/{listId}` |
| `IdentityLedger.java` | ✅ `UpdateStatusListEntry`, `GetStatusList` |
| `VcIssuerService.kt` | ✅ `credentialStatus.statusListIndex` trong mọi VC |
| `IdentityLedgerBridge.kt` | ✅ `updateStatusListEntry`, `getStatusList` |

### ✅ 4.2 — SD-JWT Selective Disclosure (đã xong trước session này)

| File | Trạng thái |
|---|---|
| `SdJwtIssuer.kt` | ✅ `issue()`, `present()`, `parseDisclosure()` |
| `SdJwtVerifier.kt` | ✅ verify signature + disclosed claims |
| `SdJwtController.kt` | ✅ `POST /api/v1/sd-jwt/issue`, `POST /api/v1/sd-jwt/verify` |
| `VcIssuerService.kt` | ✅ `issueSkillSdJwt()`, `issueEducationSdJwt()` |

### ✅ 4.3 — External Verifier Portal (Vite + React)

Tạo mới toàn bộ module `verifier-portal/` — SPA độc lập, không cần tài khoản TrustID.

| File | Vai trò |
|---|---|
| `verifier-portal/package.json` | Vite 5, React 18, TypeScript, Tailwind CSS 3, react-router-dom v6, @tanstack/react-query v5, qrcode |
| `verifier-portal/vite.config.ts` | Dev proxy `/api` và `/1.0` → `localhost:8080` |
| `verifier-portal/src/lib/trustid-client.ts` | API client: `verifyVC`, `verifySdJwt`, `smartVerify`, `resolveDID`, `listTrustedIssuers`, `getStatusList` |
| `verifier-portal/src/pages/Home.tsx` | Landing — paste VC/SD-JWT hoặc hiện QR VP Request |
| `verifier-portal/src/pages/VerifyResult.tsx` | Kết quả VALID/INVALID/REVOKED/ERROR — badge màu, disclosed claims, DID resolution, issuer info |
| `verifier-portal/src/pages/TrustRegistry.tsx` | Danh sách trusted issuers từ on-chain registry |
| `verifier-portal/src/App.tsx` | react-router-dom layout + nav |

**Endpoints sử dụng:**
- `POST /api/v1/identity/vc/verify` — W3C VC verify
- `POST /api/v1/sd-jwt/verify` — SD-JWT verify + disclosed claims
- `GET /1.0/identifiers/{did}` — DIF Universal Resolver
- `GET /api/v1/trust-registry/issuers` — Trust Registry public list
- `GET /api/v1/status-list/{listId}` — Status List 2021

**Cách chạy:**
```powershell
cd verifier-portal
npm install
npm run dev   # http://localhost:5173
npm run build # → dist/ (static, deploy qua nginx hoặc /resources/static/verifier/)
```

### ✅ 4.4 — DID Resolver chuẩn DIF (Universal Resolver compatible)

| File | Trạng thái |
|---|---|
| `UniversalResolverController.kt` | ✅ `GET /1.0/identifiers/{did}` |
| `IdentityLedgerBridge.kt` | ✅ `resolveDid(did)` helper |

**Response format** (DIF DID Resolution v1):
```json
{
  "@context": "https://w3id.org/did-resolution/v1",
  "didDocument": {
    "@context": ["https://www.w3.org/ns/did/v1", "https://w3id.org/security/suites/jws-2020/v1"],
    "id": "did:fabric:trustid:42",
    "verificationMethod": [{ "id": "..#key-1", "type": "JsonWebKey2020", "publicKeyJwk": {...} }],
    "authentication": ["..#key-1"],
    "assertionMethod": ["..#key-1"],
    "service": [{ "type": "TrustIDIdentityService", ... }]
  },
  "didDocumentMetadata": { "deactivated": false, "created": "...", "updated": "..." },
  "didResolutionMetadata": { "contentType": "application/did+ld+json", "retrieved": "..." }
}
```

### ✅ 4.5 — Biometric Unlock + Sign-on-touch

| File | Trạng thái |
|---|---|
| `identity_frontend/pubspec.yaml` | ✅ thêm `local_auth: ^2.3.0` |
| `lib/core/security/biometric_service.dart` | ✅ Mới — `isAvailable`, `authenticate`, `authenticateNow`, `isBiometricLockEnabled`, `setBiometricLockEnabled` |
| `lib/core/wallet/wallet_service.dart` | ✅ Thêm `sign(Uint8List, biometricReason)`, `signDocument(Uint8List)`, exception classes |

**API mới trong WalletService:**
```dart
// Ký payload bytes — gọi biometric trước, throw WalletBiometricException nếu cancel
static Future<String> sign(Uint8List payload, {String biometricReason})

// Convenience: SHA-256(doc) + sign — dùng cho contract signing
static Future<({String signatureBase64, String docHash})> signDocument(Uint8List documentBytes)
```

**Exception classes** (trong cùng file):
- `WalletBiometricException` — biometric cancel/fail
- `WalletNotInitializedException` — chưa generate keypair

**Toggle biometric lock từ Wallet screen:**
```dart
await BiometricService.setBiometricLockEnabled(true);  // bật
await BiometricService.isBiometricLockEnabled();        // đọc
```

### ✅ 4.6 — Trust Registry on-chain

| File | Trạng thái |
|---|---|
| `IdentityLedger.java` | ✅ `RegisterIssuer`, `RevokeIssuer`, `IsTrustedIssuer`, `ListTrustedIssuers` |
| `TrustRegistryController.kt` | ✅ CRUD endpoints |
| `IdentityLedgerBridge.kt` | ✅ `registerIssuer`, `revokeIssuer`, `listTrustedIssuers`, `isTrustedIssuer` |
| `IdentityLedgerService.kt` | ✅ 4 methods mới gọi chaincode |

**Endpoints:**
```
GET    /api/v1/trust-registry/issuers          — public, không cần auth
GET    /api/v1/trust-registry/issuers/{did}/trusted — check đơn lẻ
POST   /api/v1/trust-registry/issuers          — CHIEF/ADMIN, đăng ký issuer mới
DELETE /api/v1/trust-registry/issuers/{did}    — CHIEF/ADMIN, revoke issuer
```

**Request body (POST):**
```json
{ "did": "did:fabric:trustid:org1", "name": "MpCorp HR Issuer", "role": "ISSUER", "scope": "EmploymentCredential" }
```

**Chaincode key pattern:** `trustregistry:{did}`

### ✅ 4.7 — E-sign Contract bằng wallet ECDSA

| File | Trạng thái |
|---|---|
| `IdentityLedger.java` | ✅ `RecordSignature`, `GetSignatures` |
| `ContractSignatureController.kt` | ✅ sign + get + hash helper |
| `IdentityLedgerBridge.kt` | ✅ `recordSignature`, `getSignatures` |
| `IdentityLedgerService.kt` | ✅ 2 methods mới |
| `lib/presentation/features/contract/contract_sign_screen.dart` | ✅ Flutter UI — biometric prompt → sign → anchor |

**Endpoints:**
```
POST /api/v1/contracts/{contractId}/sign       — authenticated, ghi signature on-chain
GET  /api/v1/contracts/{contractId}/signatures — lấy danh sách signatures
POST /api/v1/contracts/hash                    — helper: Base64(docBytes) → docHash hex
```

**Request body (POST sign):**
```json
{
  "signatureBase64": "<base64 DER ECDSA P-256 signature>",
  "docHash": "<hex SHA-256 of document bytes>",
  "signerDid": "did:fabric:trustid:42"
}
```

**Chaincode key pattern:** `signature:{contractId}:{signerDid}`

**Flutter ContractSignScreen props:**
```dart
ContractSignScreen(
  contractId: 5,             // required
  contractType: 'FULL_TIME', // optional display
  startDate: '2026-01-01',   // optional display
  endDate: null,             // null = open-ended
)
```

---

## 12. Frontend integration guide (cho UI builder)

### Backend base URL
```
http://10.0.2.2:8080/api/v1    (Android emulator)
http://localhost:8080/api/v1   (web / desktop)
```

### Auth header
Tất cả endpoint trừ public (status-list, trust-registry GET, 1.0/identifiers) đều cần:
```
Authorization: Bearer <jwt_token>
```

### VC field names trong EmployeeJpaEntity
| Field | Ý nghĩa |
|---|---|
| `employmentVc` | JSON string EmploymentVC (W3C) |
| `salaryRangeVc` | JSON string SalaryRangeVC (W3C) |
| `promotionVc` | JSON string PromotionVC (W3C) |
| `terminationVc` | JSON string TerminationVC (W3C) |
| `skillSdJwt` | Compact SD-JWT SkillCredential |
| `educationSdJwt` | Compact SD-JWT EducationCredential |
| `did` | `did:fabric:trustid:{id}` |
| `publicKey` | JWK JSON string |

### Hướng dẫn tích hợp ContractSignScreen
1. Sau khi Manager tạo contract và assign cho Employee, Employee mở ContractSignScreen với `contractId`.
2. Screen tự fetch signatures hiện tại (nếu đã ký trước đó).
3. Employee tap "Sign with Biometric" → `WalletService.signDocument` → `POST /contracts/{id}/sign`.
4. Manager có thể gọi `GET /contracts/{id}/signatures` để confirm đã ký.

### Hướng dẫn tích hợp Biometric toggle (Wallet screen)
```dart
// Đọc trạng thái hiện tại
final enabled = await BiometricService.isBiometricLockEnabled();

// Toggle
await BiometricService.setBiometricLockEnabled(!enabled);

// Hiển thị indicator
Text(enabled ? '🔒 Protected by Biometric' : '🔓 Biometric off')
```

### Verifier Portal (web)
- Dev: `cd verifier-portal && npm install && npm run dev`
- Production build: `npm run build` → `dist/` → nginx hoặc copy vào `resources/static/verifier/`
- Không cần tài khoản TrustID để sử dụng — đây là điểm quan trọng cho demo 3-party

---

## 9. Critical files reference

| File | Vai trò | Liên quan phase |
|---|---|---|
| [fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) | Chaincode chính, cần thêm 7 methods | 4.1, 4.6, 4.7 |
| [fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt) | Đã có VC issuance, cần thêm `credentialStatus` | 4.1, 5.3 |
| [fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/fabric/IdentityLedgerBridge.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/fabric/IdentityLedgerBridge.kt) | Outbox bridge, cần thêm methods gọi chaincode mới | 4.1, 4.6, 4.7 |
| [identity_frontend/lib/core/wallet/wallet_service.dart](identity_frontend/lib/core/wallet/wallet_service.dart) | ECDSA P-256 wallet, cần wrap biometric trước sign | 4.5, 4.7 |
| [identity_frontend/lib/presentation/features/verifier/verifier_scan_screen.dart](identity_frontend/lib/presentation/features/verifier/verifier_scan_screen.dart) | Verifier scan, cần hiển thị badge REVOKED + Trust check | 4.1, 4.6 |
| [identity_frontend/lib/routing/app_router.dart](identity_frontend/lib/routing/app_router.dart) | Bottom nav config, cần đẩy SSI ra trước | 3.1 |
| [identity_frontend/lib/l10n/app_vi.arb](identity_frontend/lib/l10n/app_vi.arb) + [app_en.arb](identity_frontend/lib/l10n/app_en.arb) | Đổi label HRMS → SSI | 3.2 |
| [identity_frontend/pubspec.yaml](identity_frontend/pubspec.yaml) | Cần thêm `local_auth`, `hive`, `flutter_pdfview` | 4.5, 4.7, 6.4 |
| [verifier-portal/](verifier-portal/) (mới) | Vite + React verifier portal độc lập | 4.3 |

---

## 10. Cách kiểm thử end-to-end

### 10.1. Backend & Chaincode

```powershell
# Khởi động Fabric network
cd fabric-network
./scripts/network.sh up
./scripts/network.sh createChannel
./scripts/network.sh deployCC

# Khởi động backend
cd ../fabric-spring-backend
./gradlew bootRun
```

**Test cases**:
- **Status List**:
  - `POST /api/v1/auth/sign-up` → tạo user
  - Admin `PUT /admin/accounts/{id}/approve` → check `credentialStatus.statusListIndex` trong VC
  - `GET /api/v1/status-list/employment-status-list-1` → verify bitstring hợp lệ
  - Chief `PUT /admin/accounts/{id}/revoke` → fetch lại Status List → bit tại index = 1
- **DID Resolver**: `GET /1.0/identifiers/did:fabric:trustid:emp-abc` → verify JSON-LD chuẩn DIF
- **SD-JWT**: `POST /api/v1/sd-jwt/issue` với 10 claims → response có 10 disclosure → `/verify` với 3/10 disclosure → vẫn VALID

### 10.2. Flutter App
- `flutter run` trên emulator Android (API trỏ `http://10.0.2.2:8080/api/v1`)
- **Bottom nav** (sau Phase 0): mở app → tab đầu tiên là Wallet (không phải Home/Attendance)
- **i18n** (sau Phase 0): menu hiển thị "Credential Wallet", "Verifiable Records", "Issuer Console"
- **Biometric**: Wallet → toggle App Lock → restart app → phải yêu cầu vân tay/PIN
- **Disclosure Picker**: scan VP request QR → tick chọn skill → submit → backend log chỉ 3 skill được disclose

### 10.3. Verifier Portal (Vite + React)
```powershell
cd verifier-portal
npm install
npm run dev
# Mở http://localhost:5173 → click Request Skill Proof → QR hiện ra
```

Build production: `npm run build` → static `dist/` deploy được qua nginx.

### 10.4. Demo dry-run trước bảo vệ
- Chạy đủ 8 bước demo flow, đo thời gian — đảm bảo ≤12 phút
- Mỗi bước screenshot/record → đề phòng sự cố mạng trong buổi bảo vệ, có thể play video backup
- Chuẩn bị câu trả lời cho các câu hỏi hội đồng:
  - **"Tại sao Hyperledger Fabric mà không phải Ethereum?"** → permissioned, performance cao, không cần token
  - **"SHA-256 có an toàn không?"** → đủ cho audit trail, không phải mã hóa
  - **"Nếu mất điện thoại?"** → đã có session list (5.6), TOTP backup codes (5.1), revoke DID (đã có)
  - **"Privacy?"** → PII off-chain, chỉ hash + keyFields + bitstring on-chain
  - **"Production-ready chưa?"** → HMAC PoC chỉ cho demo, production sẽ Ed25519 / ECDSA P-256 với HSM
  - **"Tại sao Vite + React mà không Next.js?"** → verifier là SPA thuần, không cần SSR / API routes của Next.js; bundle nhỏ hơn, dev HMR nhanh hơn

---

## 11. Ghi chú thực thi

- **Thứ tự thực thi**:
  1. **Phase 0** (Restructure) — làm TRƯỚC, ~2-3 ngày. Không động chạm business logic, chỉ navigation + i18n + KPI dashboard.
  2. **Phase 1** theo thứ tự: 4.1 (Status List) → 4.2 (SD-JWT) → 4.4 (DID Resolver) → 4.6 (Trust Registry) → 4.3 (Verifier Portal) → 4.5 (Biometric) → 4.7 (E-sign). Lý do: backend foundation trước, frontend/portal sau khi API đã ổn định.
  3. **Phase 2** chen vào khi cần demo Q&A (TOTP, audit log, GDPR là điểm cộng cho compliance).
  4. **Phase 3** chỉ làm nếu dư thời gian.
- **Không cần production-grade**: HMAC-SHA256 hiện tại cho VC signing chấp nhận được cho demo; slide thuyết minh ghi rõ "production sẽ dùng Ed25519 / ECDSA P-256 + HSM" để hội đồng yên tâm.
- **Reuse hết sức có thể**:
  - `IdentityLedgerBridge` outbox pattern đã có → mọi feature mới gọi qua bridge để tránh viết lại retry logic
  - `VcIssuerService` đã có pattern phát VC → SD-JWT chỉ cần file mới song song
  - `WalletService` (Flutter) đã có ECDSA P-256 → biometric chỉ wrap thêm trước khi đọc private key
- **Effort estimate tổng**:
  - Phase 0 (5 task): ~2-3 ngày
  - Phase 1 (7 tính năng): ~6-8 tuần
  - Phase 2 (6 tính năng): ~2-3 tuần
  - Phase 3 (4 tính năng): ~1-2 tuần

---

## 13. Phase 3 Implementation Status (updated 2026-05-16)

> Ghi lại chính xác những gì đã implement để UI builder có thể tích hợp.

### ✅ 6.1 — Dark Mode

| File | Trạng thái |
|---|---|
| `lib/core/themes/theme_cubit.dart` | ✅ Mới — `ThemeCubit` persist ThemeMode vào SharedPreferences |
| `lib/core/themes/app_theme.dart` | ✅ Thêm `darkTheme` — màu nền `#12121F`, surface `#1E1E2E`, text trắng |
| `lib/main.dart` | ✅ `BlocProvider<ThemeCubit>` + `darkTheme: AppTheme.darkTheme` + `themeMode: themeMode` |
| `lib/presentation/features/settings/theme_setting_screen.dart` | ✅ Mới — UI 3 option: Light / Dark / System default |

**Cách tích hợp vào Settings/Profile screen:**
```dart
// Navigate đến ThemeSettingScreen
context.push('/settings/theme');

// Hoặc toggle nhanh ngay trong screen
context.read<ThemeCubit>().setMode(ThemeMode.dark);
context.read<ThemeCubit>().setMode(ThemeMode.light);
context.read<ThemeCubit>().setMode(ThemeMode.system);

// Đọc current mode
final mode = context.watch<ThemeCubit>().state; // ThemeMode
```

**ThemeSettingScreen** (`/presentation/features/settings/theme_setting_screen.dart`) tự render 3 option có border highlight khi selected. Chỉ cần `push('/settings/theme')` hoặc navigate trực tiếp.

---

### ✅ 6.2 — DIDComm Messaging

| File | Trạng thái |
|---|---|
| `DIDCommMessageJpaEntity.kt` | ✅ Mới — lưu DIDComm message vào MySQL |
| `DIDCommMessageJpaRepository.kt` | ✅ Mới — findByToDid, markAllRead |
| `DIDCommController.kt` | ✅ Mới — 3 endpoints (xem bên dưới) |
| `SecurityConfig.kt` | ✅ `/didcomm/**` added to permitAll |

**Endpoints:**
```
POST /didcomm/send                        — gửi message DID → DID
GET  /didcomm/inbox/{did}?unreadOnly=true — lấy inbox (unread default)
GET  /didcomm/inbox/{did}?unreadOnly=false — lấy tất cả messages
GET  /didcomm/inbox/{did}?markRead=true   — fetch + auto mark as read
POST /didcomm/inbox/{did}/mark-read       — đánh dấu tất cả đã đọc
```

**Request body (POST /didcomm/send):**
```json
{
  "type": "https://trustid.io/didcomm/1.0/credential-offer",
  "fromDid": "did:fabric:trustid:org1",
  "toDid": "did:fabric:trustid:42",
  "body": "{\"vcType\":\"EmploymentVC\",\"message\":\"Your credential is ready\"}"
}
```

**Response (message object):**
```json
{
  "id": "uuid",
  "type": "https://...",
  "fromDid": "did:...",
  "toDid": "did:...",
  "body": "...",
  "createdAt": "2026-05-16T...",
  "isRead": false
}
```

**Demo storyline**: Issuer gửi credential offer → Holder mở inbox → thấy notification → tap Accept.

**Cách build UI (Flutter):**
- `GET /didcomm/inbox/{myDid}?unreadOnly=true` để hiển thị badge số tin nhắn chưa đọc.
- `GET /didcomm/inbox/{myDid}?unreadOnly=false&markRead=true` khi mở màn hình inbox.
- `POST /didcomm/send` khi Issuer muốn push notification qua DIDComm.

---

### ✅ 6.3 — Notarization Service

| File | Trạng thái |
|---|---|
| `NotarizationController.kt` | ✅ Mới — 3 endpoints (xem bên dưới) |
| `IdentityLedgerBridge.kt` | ✅ Thêm `upsertNotarizationRecord`, `getNotarizationRecord` |
| `SecurityConfig.kt` | ✅ `/api/v1/notarization/**` added to permitAll (verify là public) |

**Endpoints:**
```
POST /api/v1/notarization/notarize   — upload file (multipart, max 50MB) → anchor SHA-256 on-chain
POST /api/v1/notarization/verify     — verify file integrity: { docId, fileBase64 }
GET  /api/v1/notarization/{docId}    — lấy metadata on-chain (public, không cần auth)
```

**Upload request (multipart/form-data):**
```
file     : <binary>
label    : "NDA Contract 2026"   (optional)
signerDid: "did:fabric:trustid:42" (optional)
```

**Verify request body:**
```json
{
  "docId": "uuid-của-document",
  "fileBase64": "<base64 encoded original file bytes>"
}
```

**Verify response:**
```json
{
  "docId": "...",
  "verified": true,
  "computedHash": "sha256hex...",
  "onChainHash": "sha256hex...",
  "record": { "recordId": "...", "dataHash": "...", "keyFields": "...", ... }
}
```

**Chaincode key pattern:** `document:{docId}` với `recordType=DOCUMENT` — tái dùng `UpsertRecord` có sẵn, không cần chaincode mới.

**Cách build UI (Flutter):**
- Dùng `file_picker` package để chọn file.
- Gửi multipart POST tới `/api/v1/notarization/notarize` → nhận `docId`.
- Lưu `docId` vào local storage để verify sau.
- Verify: đọc lại file → base64 → `POST /api/v1/notarization/verify`.

---

### ✅ 6.4 — Offline Cache (Hive) + Crashlytics

**Crashlytics:** Đã có từ Phase 1 (`firebase_crashlytics: ^5.2.0` + `CrashlyticsService`). Không cần thêm.

**Hive offline VC cache:**

| File | Trạng thái |
|---|---|
| `pubspec.yaml` | ✅ Thêm `hive_flutter: ^1.1.0` |
| `lib/core/cache/vc_cache_service.dart` | ✅ Mới — static service, box `vc_cache` |
| `lib/main.dart` | ✅ `VcCacheService.init()` trước `configureDependencies()` |

**API:**
```dart
// Init (đã gọi trong main.dart)
await VcCacheService.init();

// Cache VCs sau khi fetch từ backend
await VcCacheService.cacheEmploymentVc(vcJsonString);
await VcCacheService.cacheSalaryRangeVc(vcJsonString);
await VcCacheService.cacheSkillSdJwt(sdJwtCompact);

// Đọc cache (offline fallback)
final vc = VcCacheService.getEmploymentVc();  // String? JSON
final sdJwt = VcCacheService.getSkillSdJwt(); // String? compact

// Generic put/get
await VcCacheService.put('custom_key', value);
final val = VcCacheService.get('custom_key');

// Xóa khi logout
await VcCacheService.clear();

// Watch reactive
VcCacheService.watch('employment_vc').listen((event) { ... });
```

**Tích hợp vào Wallet screen:**
```dart
// Trong WalletBloc hoặc screen initState:
// 1. Hiện cached VCs ngay (offline-first)
final cachedVc = VcCacheService.getEmploymentVc();
if (cachedVc != null) emit(WalletLoaded(vcJson: cachedVc, fromCache: true));

// 2. Fetch mới từ backend
try {
  final fresh = await profileRepo.getMyVcs();
  await VcCacheService.cacheEmploymentVc(fresh.employmentVc);
  emit(WalletLoaded(vcJson: fresh.employmentVc, fromCache: false));
} catch (_) {
  // giữ cached state — user thấy last-known-good credential
}
```

**Keys có sẵn:**
| Constant | Key | Loại |
|---|---|---|
| `VcCacheService.keyEmployment` | `employment_vc` | W3C VC JSON String |
| `VcCacheService.keySalaryRange` | `salary_range_vc` | W3C VC JSON String |
| `VcCacheService.keyPromotion` | `promotion_vc` | W3C VC JSON String |
| `VcCacheService.keyTermination` | `termination_vc` | W3C VC JSON String |
| `VcCacheService.keySkillSdJwt` | `skill_sd_jwt` | SD-JWT compact String |
| `VcCacheService.keyEducationSdJwt` | `education_sd_jwt` | SD-JWT compact String |