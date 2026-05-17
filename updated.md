# Backend đã sẵn sàng — Tài liệu tham chiếu để build UI

> Phạm vi: **Phase 0** (Restructure HRMS → SSI) + Phase 1 / 4.1 (W3C Status List 2021 — VC Revocation) + 4.2 (SD-JWT Selective Disclosure).
> Backend Kotlin + chaincode Java đã compile pass. **Phase 0 đã được áp dụng cho Flutter app (navigation, i18n, Issuer Console)**, file này tổng hợp những gì có sẵn để build các UI tiếp theo.

---

## 0. Phase 0 — Restructure HRMS → SSI (ĐÃ LÀM)

### 0.1. Bottom navigation — SSI-first
File sửa: [identity_frontend/lib/core/routes/app_router.dart](identity_frontend/lib/core/routes/app_router.dart)

Mọi role giờ mở app sẽ thấy ngay các tab SSI. HRMS features được gom vào tab `Workplace`.

| Role | Bottom Nav (mới) |
|---|---|
| EMPLOYEE | `Wallet` · `Verifier` · `Workplace` · `Home` · `Profile` |
| MANAGER | `Wallet` · `Verifier` · `Workplace` · `Home` · `Profile` |
| CHIEF | `Wallet` · `Verifier` · `Credential Subjects` · `Workplace` · `Profile` |
| ADMIN | `Issuer` · `Wallet` · `Verifier` · `Verifiable Records` · `Profile` |

**Lưu ý**: code HRMS cũ (Attendance/Requests/Directory/Company/Payroll/Contract/Manager screens) **chưa bị xóa** — vẫn truy cập được qua tab Workplace hoặc deeplink trực tiếp `/app/attendance`, `/app/requests`, v.v.

### 0.2. Workplace hub screen (mới)
File mới: [identity_frontend/lib/presentation/features/workplace/workplace_screen.dart](identity_frontend/lib/presentation/features/workplace/workplace_screen.dart)
Route mới: `/app/workplace` (thêm trong [app_router.dart](identity_frontend/lib/core/routes/app_router.dart))

Là 1 grid 2 cột chứa 6–8 ô (theo role): Attendance · Requests · Directory · Company · Payroll · Contract · (Manager) Approvals · (Manager) Timesheet. Mỗi ô push tới route HRMS tương ứng. Hội đồng vẫn xem được HRMS demo khi yêu cầu.

### 0.3. i18n — đổi label HRMS → SSI
File sửa:
- [identity_frontend/lib/l10n/app_vi.arb](identity_frontend/lib/l10n/app_vi.arb)
- [identity_frontend/lib/l10n/app_en.arb](identity_frontend/lib/l10n/app_en.arb)
- [identity_frontend/lib/l10n/app_localizations.dart](identity_frontend/lib/l10n/app_localizations.dart) (abstract getters)
- [identity_frontend/lib/l10n/app_localizations_en.dart](identity_frontend/lib/l10n/app_localizations_en.dart)
- [identity_frontend/lib/l10n/app_localizations_vi.dart](identity_frontend/lib/l10n/app_localizations_vi.dart)

Các key mới (KHÔNG động vào key cũ — caller cũ vẫn hoạt động):

| Key | EN | VI |
|---|---|---|
| `navCredentials` | Credentials | Chứng chỉ |
| `navIssuer` | Issuer | Issuer |
| `navWorkplace` | Workplace | Workplace |
| `workplaceTitle` | Workplace | Workplace |
| `workplaceSubtitle` | Use-cases illustrating the Issuer role (HRMS) | Các use case minh họa role Issuer (HRMS) |
| `workplaceAttendance` / `workplaceRequests` / `workplaceDirectory` / `workplaceCompany` / `workplacePayroll` / `workplaceContract` / `workplaceManagerRequests` / `workplaceManagerTimesheet` | (tile labels) | (tile labels) |
| `issuerConsoleTitle` | Issuer Console | Issuer Console |
| `issuerConsoleSubtitle` | Issue & manage Verifiable Credentials | Phát hành & quản lý Verifiable Credentials |
| `issuerStatsCredentialsIssued` | Credentials Issued | VC đã phát hành |
| `issuerStatsActiveDids` | Active DIDs | DID đang hoạt động |
| `issuerStatsRevokedMonth` | Revoked this month | VC bị thu hồi (tháng) |
| `issuerStatsTrustedIssuers` | Trusted Issuers | Issuer được tin tưởng |
| `issuerStatsSection` | SSI KPIs | KPI SSI |
| `issuerStatsHrSection` | Operations KPIs (HR) | KPI vận hành (HR) |
| `issuerActionEnroll` | Enroll & Issue Credential | Enroll & cấp VC |
| `issuerActionIssueSalary` | Issue SalaryRange VC | Phát hành SalaryRangeVC |
| `issuerActionIssueSkill` | Issue Skill VC (SD-JWT) | Phát hành SkillVC (SD-JWT) |
| `issuerActionIssueEducation` | Issue Education VC (SD-JWT) | Phát hành EducationVC (SD-JWT) |
| `issuerActionRevoke` | Revoke Credentials | Thu hồi VC |
| `issuerActionVerifier` | Open Verifier | Mở Verifier |
| `ssiCredentialWallet` | Credential Wallet | Ví Credential |
| `ssiCredentialSubjects` | Credential Subjects | Credential Subjects |
| `ssiPresentCredential` | Present Credential | Trình bày Credential |
| `ssiVerifiableRecords` | Verifiable Records | Sổ ghi Verifiable |
| `ssiIdentityAttributes` | Identity Attributes | Thuộc tính danh tính |

### 0.4. Admin Dashboard → Issuer Console
File sửa: [identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart](identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart)

- AppBar title đổi: `Control Panel` → `Issuer Console`
- Header card gradient hiển thị `issuerConsoleTitle` + `issuerConsoleSubtitle`
- Stats sắp xếp lại theo 2 section rõ ràng (mỗi section có label + icon):
  1. **SSI KPIs** (ưu tiên ở đầu): `Credentials Issued` · `Active DIDs` · `Revoked this month` · `Trusted Issuers`
  2. **Operations KPIs (HR)** (đẩy xuống dưới): `Total Staff` · `Active` · `Today Check-in` · `Pending` (giữ nguyên endpoint cũ `/admin/dashboard`)
- Quick actions xếp lại theo lăng kính Issuer: `Enroll & Issue Credential` · `Issue SalaryRange VC` · `Open Verifier` · `Credential Subjects` (`Manage Staff` cũ được rename về Credential Subjects, vẫn `/app/chief`).
- KPI SSI gọi endpoint MỚI `GET /api/v1/admin/issuer-stats` (chi tiết ở 0.5). Khi backend chưa có endpoint này, fetch issuer-stats fail-silent → các KPI SSI hiển thị `0` để tránh vỡ UI; KPI HR vẫn render bình thường từ `/admin/dashboard`.

### 0.5. Backend endpoint mới: `GET /api/v1/admin/issuer-stats`
File sửa: [fabric-spring-backend/.../AdminController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AdminController.kt)
Auth: `hasAnyRole('ADMIN','CHIEF')` (giống `/dashboard`).

**Response**:
```json
{
  "status": "200",
  "message": "OK",
  "data": {
    "credentialsIssued": 17,
    "employmentVcCount": 5,
    "salaryVcCount": 4,
    "promotionVcCount": 2,
    "skillSdJwtCount": 3,
    "educationSdJwtCount": 3,
    "activeDids": 5,
    "revokedThisMonth": 1,
    "trustedIssuers": 1,
    "pendingAccounts": 2
  }
}
```

**Cách tính** (đều aggregate trên `employeeJpaRepository.findAll()` để khỏi cần migration thêm bảng):
- `employmentVcCount` / `salaryVcCount` / `promotionVcCount` — đếm employee có VC field tương ứng non-null.
- `skillSdJwtCount` / `educationSdJwtCount` — đếm employee có cột `skillSdJwt` / `educationSdJwt` non-null (đọc qua reflection để khỏi crash nếu JPA migration chưa thêm cột).
- `credentialsIssued` = tổng 5 con số trên.
- `activeDids` = `isActive && did != null`.
- `revokedThisMonth` = employee có `terminationVc` non-null & `updatedAt` thuộc tháng hiện tại (proxy cho count revoke; Trust Registry 4.6 chưa làm).
- `trustedIssuers` = hằng số `1` (chỉ có org1 phát hành VC; khi 4.6 ship sẽ thay bằng `ListTrustedIssuers().size`).
- `pendingAccounts` — mirror từ `authRepository.findByStatus(PENDING).size` để Issuer Console hiển thị banner.

**ApiConstants frontend**: thêm `static const String adminIssuerStats = '/admin/issuer-stats';` ([api_constants.dart](identity_frontend/lib/core/network/api_constants.dart#L45)).

### 0.6. Demo flow narrative (không cần code)
Slide thuyết minh đổi định vị: *"TrustID là nền tảng SSI cho workplace credentials. HRMS chỉ là use case minh họa role Issuer."* Demo MỞ ĐẦU bằng Wallet → Credentials → Verifier scan từ phút đầu; HRMS chỉ xuất hiện khi hội đồng yêu cầu (qua tab Workplace).

### 0.7. Bảng tham chiếu file Phase 0

| File | Loại | Vai trò |
|---|---|---|
| [identity_frontend/lib/core/routes/app_router.dart](identity_frontend/lib/core/routes/app_router.dart) | sửa | SSI-first bottom nav, thêm route `/app/workplace` |
| [identity_frontend/lib/presentation/features/workplace/workplace_screen.dart](identity_frontend/lib/presentation/features/workplace/workplace_screen.dart) | mới | Workplace hub gom HRMS use-cases |
| [identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart](identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart) | sửa | Issuer Console: SSI KPIs section + HR KPIs section + quick actions |
| [identity_frontend/lib/core/network/api_constants.dart](identity_frontend/lib/core/network/api_constants.dart) | sửa | thêm `adminIssuerStats` |
| [identity_frontend/lib/l10n/app_vi.arb](identity_frontend/lib/l10n/app_vi.arb) · [app_en.arb](identity_frontend/lib/l10n/app_en.arb) · [app_localizations.dart](identity_frontend/lib/l10n/app_localizations.dart) · [app_localizations_en.dart](identity_frontend/lib/l10n/app_localizations_en.dart) · [app_localizations_vi.dart](identity_frontend/lib/l10n/app_localizations_vi.dart) | sửa | 28 key SSI/Workplace/Issuer/Stats mới |
| [fabric-spring-backend/.../AdminController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AdminController.kt) | sửa | endpoint `GET /admin/issuer-stats` |

### 0.8. Việc Phase 0 còn lại (có thể làm sau, không block Phase 1)
- [ ] Đổi label trên các screen lẻ tẻ vẫn dùng từ "Employee/Admin Dashboard" trong AppBar (ví dụ Home greeting, Chief screen title) — hiện đang dùng key cũ, không ảnh hưởng tới định vị bottom nav nhưng nên đồng bộ nếu hội đồng vào sâu.
- [ ] Khi Skill / Education SD-JWT UI ra đời (Phase 1 / 4.2 ở phần dưới), cắm trực tiếp 2 KPI `skillSdJwtCount` / `educationSdJwtCount` vào Issuer Console (đã có sẵn trong response).
- [ ] Khi Trust Registry (Phase 1 / 4.6) ship, thay `trustedIssuers = 1` bằng count từ chaincode `ListTrustedIssuers()`.

---

---

## 1. Tóm tắt thay đổi backend

### 1.1. Hyperledger Fabric chaincode (Java)
- [`IdentityLedger.java`](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) — thêm 2 transaction:
  - `UpdateStatusListEntry(listId, encodedList, size, updatedIndex, revoked, timestamp, updatedBy)` — SUBMIT
  - `GetStatusList(listId)` — EVALUATE
  - Key format on-chain: `statuslist:{listId}`
- [`StatusListRecord.java`](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/StatusListRecord.java) — DataType mới.

### 1.2. Spring Boot (Kotlin)
- **Service mới**: [`StatusListService.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/StatusListService.kt), [`SdJwtIssuer.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtIssuer.kt), [`SdJwtVerifier.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtVerifier.kt).
- **Controller mới**: [`StatusListController.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/StatusListController.kt), [`SdJwtController.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/SdJwtController.kt).
- **Sửa**:
  - [`VcIssuerService.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt) — mọi VC giờ chứa `credentialStatus` + `verifyVC()` check Status List; thêm `issueSkillSdJwt()` / `issueEducationSdJwt()`.
  - [`AdminController.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AdminController.kt) — `approve` gọi `statusListService.activate(...)` trước khi issue VC.
  - [`ChiefController.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/ChiefController.kt) — `terminate` gọi `statusListService.revoke(...)`.
  - [`EmployeeJpaEntity.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/persistence/jpa_entity/EmployeeJpaEntity.kt) — thêm cột `skill_sd_jwt`, `education_sd_jwt` (LONGTEXT).
  - [`IdentityLedgerBridge.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/fabric/IdentityLedgerBridge.kt) — bridge `updateStatusListEntry()` / `getStatusList()`.
  - [`IdentityLedgerService.kt`](fabric-spring-backend/src/main/kotlin/org/fabric/api/service/IdentityLedgerService.kt) — `updateStatusListEntry()` / `getStatusList()`.
  - [`SecurityConfig.kt`](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/config/SecurityConfig.kt) — mở public cho `/api/v1/status-list/**` và SD-JWT public endpoints.
  - [`application.properties`](fabric-spring-backend/src/main/resources/application.properties) — thêm `vc.issuer-did`, `vc.status-list.*`, `sd-jwt.secret`.

---

## 2. API tổng hợp cho UI

### 2.1. Status List 2021 — public (không cần JWT)

| Method | Path | Mục đích | Caller |
|---|---|---|---|
| GET | `/api/v1/status-list/{listId}` | Lấy signed `StatusList2021Credential` VC (chứa bitstring đã gzip + base64url) | Verifier portal, Mobile verifier |
| GET | `/api/v1/status-list/{listId}/entry?index={i}` | Convenience: trả `{revoked: true/false}` cho 1 entry | Mobile wallet / verifier UI khi không muốn decode bitstring tự |

**Default listId**: `employment-status-list-1` (cấu hình `vc.status-list.id`, capacity 131,072 bits).

#### Response `GET /api/v1/status-list/{listId}`
```json
{
  "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://w3id.org/vc/status-list/2021/v1"
  ],
  "id": "http://localhost:8080/api/v1/status-list/employment-status-list-1",
  "type": ["VerifiableCredential", "StatusList2021Credential"],
  "issuer": "did:fabric:trustid:org1",
  "issuanceDate": "2026-05-16T10:30:00Z",
  "credentialSubject": {
    "id": ".../employment-status-list-1#list",
    "type": "StatusList2021",
    "statusPurpose": "revocation",
    "encodedList": "<base64url(gzip(bitstring))>"
  },
  "proof": {
    "type": "HMAC-SHA256",
    "created": "2026-05-16T10:30:00Z",
    "verificationMethod": "did:fabric:trustid:org1#key-1",
    "proofValue": "<hex>"
  }
}
```

#### Response `GET /api/v1/status-list/{listId}/entry?index=42`
```json
{
  "listId": "employment-status-list-1",
  "statusListIndex": 42,
  "revoked": false,
  "status": "ACTIVE"
}
```

### 2.2. SD-JWT — kết hợp public + Admin

| Method | Path | Auth | Mục đích |
|---|---|---|---|
| POST | `/api/v1/sd-jwt/issue/skill/{employeeId}` | ADMIN/CHIEF | Phát SkillCredential SD-JWT |
| POST | `/api/v1/sd-jwt/issue/education/{employeeId}` | ADMIN/CHIEF | Phát EducationCredential SD-JWT |
| POST | `/api/v1/sd-jwt/issue` | ADMIN/CHIEF | Generic issuer (tự định nghĩa vct + claims) |
| GET | `/api/v1/sd-jwt/{employeeId}/skill` | public | Mobile wallet sync SkillVC |
| GET | `/api/v1/sd-jwt/{employeeId}/education` | public | Mobile wallet sync EducationVC |
| POST | `/api/v1/sd-jwt/present` | public | Helper build presentation (demo / portal) |
| POST | `/api/v1/sd-jwt/verify` | public | Verifier check presentation |

#### Request `POST /api/v1/sd-jwt/issue/skill/{employeeId}`
```json
{
  "skills": {
    "Kotlin": "ADVANCED",
    "Spring Boot": "ADVANCED",
    "React": "INTERMEDIATE",
    "Docker": "INTERMEDIATE",
    "PostgreSQL": "INTERMEDIATE",
    "Hyperledger Fabric": "BEGINNER",
    "AWS": "BEGINNER",
    "Flutter": "INTERMEDIATE",
    "TypeScript": "ADVANCED",
    "GraphQL": "BEGINNER"
  }
}
```

#### Response (Admin nhận được sau khi issue — toàn bộ disclosure):
```json
{
  "status": "200",
  "message": "SkillCredential SD-JWT issued",
  "data": {
    "employeeId": 42,
    "sdJwt": "eyJhbGc...~WyJhYmMi...~WyJkZWYi...~",
    "disclosures": ["WyJhYmMi...", "WyJkZWYi...", "..."],
    "claims": [
      {"name": "Kotlin", "value": "ADVANCED"},
      {"name": "Spring Boot", "value": "ADVANCED"}
    ]
  }
}
```

#### Request `POST /api/v1/sd-jwt/present` (Holder build presentation)
```json
{
  "sdJwt": "eyJhbGc...~WyJhYmMi...~WyJkZWYi...~",
  "reveal": ["Kotlin", "Spring Boot", "Hyperledger Fabric"]
}
```

#### Response:
```json
{
  "data": {
    "presentation": "eyJhbGc...~<disclosure Kotlin>~<disclosure Spring>~<disclosure HF>~",
    "revealed": ["Kotlin", "Spring Boot", "Hyperledger Fabric"],
    "hidden": ["React", "Docker", "PostgreSQL", "AWS", "Flutter", "TypeScript", "GraphQL"]
  }
}
```

#### Request `POST /api/v1/sd-jwt/verify` (Verifier)
```json
{
  "presentation": "eyJhbGc...~<d1>~<d2>~<d3>~",
  "requireClaims": ["Kotlin", "Spring Boot"]
}
```

#### Response:
```json
{
  "data": {
    "valid": true,
    "reason": "Valid",
    "issuer": "did:fabric:trustid:org1",
    "subject": "did:fabric:trustid:42",
    "vct": "SkillCredential",
    "disclosedClaims": {
      "Kotlin": "ADVANCED",
      "Spring Boot": "ADVANCED",
      "Hyperledger Fabric": "BEGINNER"
    },
    "alwaysVisible": {
      "iss": "did:fabric:trustid:org1",
      "vct": "SkillCredential",
      "sub": "did:fabric:trustid:42",
      "iat": 1747387800,
      "exp": 1778923800,
      "department": "Engineering",
      "position": "Senior Developer",
      "issuedBy": "did:fabric:trustid:org1",
      "status": "StatusList2021Entry block"
    }
  }
}
```

### 2.3. VC verify đã được nâng cấp

`POST /api/v1/identity/vc/verify` (đã có) + `GET /api/v1/identity/vc/verify-by-id?vcId=...` — giờ **tự động check Status List 2021** trong khi verify. Nếu VC có `credentialStatus.type = "StatusList2021Entry"` và bit = 1 → trả `{valid: false, reason: "VC revoked (status list ... index ...)"}`.

UI verifier scan không cần thay gì — chỉ cần hiển thị reason mới khi `valid = false`.

---

## 3. Cấu trúc VC sau khi đổi

### 3.1. EmploymentVC / SalaryRangeVC / PromotionVC / TerminationVC

Mỗi VC giờ có thêm block `credentialStatus`:

```json
{
  "@context": ["https://www.w3.org/2018/credentials/v1"],
  "type": ["VerifiableCredential", "EmploymentCredential"],
  "id": "vc:trustid:employment:42:1747387800",
  "issuer": "did:fabric:trustid:org1",
  "issuanceDate": "2026-05-16T10:30:00Z",
  "expirationDate": "2027-05-16T10:30:00Z",
  "credentialSubject": "subject fields as before",
  "credentialStatus": {
    "id": "http://localhost:8080/api/v1/status-list/employment-status-list-1#42",
    "type": "StatusList2021Entry",
    "statusPurpose": "revocation",
    "statusListIndex": "42",
    "statusListCredential": "http://localhost:8080/api/v1/status-list/employment-status-list-1"
  },
  "proof": "HMAC proof as before"
}
```

**Quy ước index**: `statusListIndex = employee.id` (Long → String). Index không bị đổi trong toàn bộ vòng đời nhân viên.

### 3.2. SkillCredential / EducationCredential (SD-JWT format)

Wire format: `<header>.<payload>.<signature>~<disclosure_1>~<disclosure_2>~...~`

**Header**:
```json
{"alg": "HS256", "typ": "sd+jwt"}
```

**Payload** (clear claims + machinery):
```json
{
  "iss": "did:fabric:trustid:org1",
  "vct": "SkillCredential",
  "sub": "did:fabric:trustid:42",
  "iat": 1747387800,
  "exp": 1778923800,
  "_sd_alg": "sha-256",
  "_sd": ["<digest1>", "<digest2>"],
  "status": "StatusList2021Entry block",
  "department": "Engineering",
  "position": "Senior Developer",
  "issuedBy": "did:fabric:trustid:org1"
}
```

**Disclosure** = `base64url(JSON([salt, claim_name, claim_value]))`. Ví dụ disclosure đã decode:
```json
["x9PnvW0salt", "Kotlin", "ADVANCED"]
```

**Presentation = JWT + chỉ những disclosure holder muốn lộ + trailing `~`**. Khi gửi cho Verifier, các disclosure bị bỏ thì Verifier không biết tên claim đó tồn tại (digest đã được shuffle khi issue).

---

## 4. Hướng dẫn build UI — Flutter (TrustID mobile app)

### 4.1. Wallet — hiển thị badge ACTIVE / REVOKED cho từng VC

**File**: [`identity_frontend/lib/presentation/features/wallet/wallet_screen.dart`](identity_frontend/lib/presentation/features/wallet/wallet_screen.dart)

**Flow**:
1. Khi render mỗi VC card, parse `credentialStatus` block từ VC JSON.
2. Gọi `GET /api/v1/status-list/{listId}/entry?index={statusListIndex}` (endpoint convenience).
3. Hiển thị badge:
   - `revoked: false` → green chip "ACTIVE"
   - `revoked: true` → red chip "REVOKED"
   - Network error → grey chip "UNKNOWN" + retry button

**Suggested caching**: lưu kết quả last-check + timestamp trong Hive (nếu Phase 3 / 6.4 đã làm) hoặc in-memory cache 60s để không spam endpoint.

**Pseudocode**:
```dart
Future<VcStatus> fetchStatus(String vcJson) async {
  final vc = jsonDecode(vcJson) as Map<String, dynamic>;
  final cs = vc['credentialStatus'] as Map<String, dynamic>?;
  if (cs == null || cs['type'] != 'StatusList2021Entry') {
    return VcStatus.unknown;
  }
  final listId = (cs['statusListCredential'] as String).split('/').last;
  final index  = cs['statusListIndex'] as String;
  final resp = await dio.get(
    '/api/v1/status-list/$listId/entry',
    queryParameters: {'index': index},
  );
  return resp.data['revoked'] == true ? VcStatus.revoked : VcStatus.active;
}
```

### 4.2. Verifier Scan — hiển thị reason mới

**File**: [`identity_frontend/lib/presentation/features/verifier/verifier_scan_screen.dart`](identity_frontend/lib/presentation/features/verifier/verifier_scan_screen.dart)

Endpoint `POST /api/v1/identity/vc/verify` giờ trả thêm reason "VC revoked (status list ... index ...)" khi VC bị revoke. Chỉ cần:
- Khi `valid = false` và reason chứa "revoked" → hiển thị màn hình lỗi nền đỏ + icon huỷ + dòng "Credential đã bị thu hồi"
- Khi `valid = false` và reason khác (expired / tampered / mismatch) → giữ UI hiện tại

### 4.3. Disclosure Picker — màn hình mới cho SD-JWT

**File mới đề xuất**: `identity_frontend/lib/presentation/features/wallet/disclosure_picker_screen.dart`

**Flow** (chạy khi Holder scan QR Verifier request cho SkillCredential / EducationCredential):
1. Verifier gửi request kèm danh sách claim mong muốn (ví dụ `["Kotlin", "Spring Boot", "Docker"]`).
2. Mobile load SD-JWT từ local store (đã sync trước đó qua `GET /api/v1/sd-jwt/{employeeId}/skill`).
3. Parse SD-JWT để lấy danh sách disclosures (tách theo `~`, decode mỗi disclosure base64url → JSON array `[salt, name, value]`).
4. Hiển thị danh sách checkbox: claims Verifier yêu cầu mặc định tick, claims khác mặc định untick. Holder có thể tick thêm hoặc bỏ tick claim Verifier yêu cầu (Verifier sẽ thấy missing claim trong response).
5. Khi tap "Sign with Biometric" (kết hợp với 4.5 Biometric khi làm sau):
   - Option A (demo nhanh): gọi `POST /api/v1/sd-jwt/present` với `{sdJwt, reveal: [...]}` để backend build presentation.
   - Option B (đúng SSI): build presentation locally — cắt chuỗi disclosure tương ứng và ghép lại.
6. Gửi `presentation` lên Verifier theo flow OID4VP đã có.

**Suggested model**:
```dart
class SdJwtDisclosure {
  final String raw;
  final String claimName;
  final dynamic claimValue;
  bool selected;
}

class SdJwtCredential {
  final String jwtPart;
  final List<SdJwtDisclosure> disclosures;

  static SdJwtCredential parse(String sdJwt) {
    // split on '~'
  }

  String buildPresentation() =>
    '$jwtPart~${disclosures.where((d) => d.selected).map((d) => d.raw).join('~')}~';
}
```

### 4.4. Issuer Console (Admin) — thêm card "Issue Skill / Education"

**File**: [`identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart`](identity_frontend/lib/presentation/features/admin/admin_dashboard_screen.dart) hoặc tạo screen mới `issue_sd_jwt_screen.dart`.

**Form**:
- Search nhân viên → chọn employeeId
- Tab "Skill" / "Education":
  - Skill: list rỗng có nút "+ Add skill" → mỗi row có `name` (text) + `level` (dropdown ADVANCED/INTERMEDIATE/BEGINNER)
  - Education: form cố định fields `degree`, `major`, `university`, `graduationYear`, `gpa`, `honors`
- Submit → `POST /api/v1/sd-jwt/issue/skill/{employeeId}` (kèm JWT Admin)
- Response trả `sdJwt` → có thể show QR cho holder scan, hoặc gửi push notification.

### 4.5. KPI dashboard cập nhật (Phase 0 / 3.3 nếu chưa)

Khi làm phần Issuer Console, có thể thêm KPI:
- **Revoked Credentials this month** — bên backend tạo endpoint `/api/v1/admin/issuer-stats` (chưa làm) đếm số lần `statusListService.revoke()` trong tháng. Tạm thời FE có thể đếm từ `terminationVc` đã issue.

---

## 5. Hướng dẫn build UI — Verifier Portal (Vite + React) — chưa tạo

Theo update.md mục 4.3, portal sẽ tách riêng (`verifier-portal/`). Hai feature mới này giúp portal:

### 5.1. Trang Verify Result (`VerifyResult.tsx`)

Khi nhận VP token / SD-JWT presentation từ Holder:
- Gọi `POST /api/v1/identity/vc/verify` cho VC JSON-LD truyền thống → check `valid`. Nếu `valid = false` và reason chứa "revoked" → badge đỏ "REVOKED", reason khác → badge "INVALID".
- Gọi `POST /api/v1/sd-jwt/verify` cho SD-JWT presentation → hiển thị `disclosedClaims` (tick xanh từng claim), `vct`, `issuer`, `subject`.

**UI components đề xuất**:
- `StatusBadge` (3 states: VALID green / INVALID grey / REVOKED red)
- `DisclosedClaimsTable` (key-value table với icon eye / eye-off)
- `IssuerInfo` (resolve DID issuer + check Trust Registry — Trust Registry chưa làm ở phase này)

### 5.2. Trang Request VP (`Home.tsx`)

Cho phép Verifier chọn:
- "Request Skill Proof" → tạo QR `?type=SkillCredential&required=Kotlin,Spring,Docker`
- "Request Education Proof" → QR `?type=EducationCredential&required=degree,university,graduationYear`

Holder mobile scan QR → mở Disclosure Picker (4.3) → gửi presentation → portal poll result.

---

## 6. Cấu hình cần biết

`application.properties` mới:
```properties
# Issuer DID — dùng trong VC issuer + verificationMethod fields
vc.issuer-did=did:fabric:trustid:org1

# W3C Status List 2021
vc.status-list.id=employment-status-list-1
vc.status-list.size=131072
vc.status-list.base-url=http://localhost:8080/api/v1/status-list

# SD-JWT signing secret (separate from vc.secret để rotate độc lập)
sd-jwt.secret=${SD_JWT_SECRET:sd-jwt-secret-trustid-org1-2026}
```

**Lưu ý production**: `vc.status-list.base-url` phải đổi sang public domain khi deploy, vì URL này được nhúng vào mọi VC issued ra và Verifier phải resolve được.

---

## 7. Database migration

JPA đang `spring.jpa.hibernate.ddl-auto=update` → 2 cột mới sẽ tự thêm khi backend start:
- `employee.skill_sd_jwt` (LONGTEXT)
- `employee.education_sd_jwt` (LONGTEXT)

Nếu production dùng Flyway/Liquibase: cần migration script tương ứng.

---

## 8. Test plan (manual qua Postman / curl)

### 8.1. Status List end-to-end

```bash
# 1. Sign up + admin approve employeeId=42 → EmploymentVC có credentialStatus.statusListIndex="42"
curl http://localhost:8080/api/v1/identity/vc/employment/42

# 2. Lấy Status List VC
curl http://localhost:8080/api/v1/status-list/employment-status-list-1

# 3. Check entry hiện tại = ACTIVE
curl "http://localhost:8080/api/v1/status-list/employment-status-list-1/entry?index=42"
# Kết quả: {"revoked": false, "status": "ACTIVE"}

# 4. Chief terminate (cần JWT Chief)
curl -X PUT -H "Authorization: Bearer $CHIEF_JWT" -d '{"reason":"resigned"}' \
  http://localhost:8080/api/v1/chief/employees/42/terminate

# 5. Check entry → REVOKED
curl "http://localhost:8080/api/v1/status-list/employment-status-list-1/entry?index=42"
# Kết quả: {"revoked": true, "status": "REVOKED"}

# 6. Verify VC cũ → invalid với reason revoked
curl -X POST -H "Content-Type: application/json" \
  -d '{"vc":"<paste EmploymentVC JSON>"}' \
  http://localhost:8080/api/v1/identity/vc/verify
# Kết quả: {"valid": false, "reason": "VC revoked (status list employment-status-list-1 index 42)"}
```

### 8.2. SD-JWT end-to-end

```bash
# 1. Admin issue 10 skills (cần JWT Admin)
curl -X POST -H "Authorization: Bearer $ADMIN_JWT" -H "Content-Type: application/json" \
  -d '{"skills":{"Kotlin":"ADVANCED","Spring":"ADVANCED","React":"INTERMEDIATE","Docker":"INTERMEDIATE","PostgreSQL":"INTERMEDIATE","Fabric":"BEGINNER","AWS":"BEGINNER","Flutter":"INTERMEDIATE","TS":"ADVANCED","GraphQL":"BEGINNER"}}' \
  http://localhost:8080/api/v1/sd-jwt/issue/skill/42

# Lưu sdJwt từ response → $SD_JWT

# 2. Holder lấy lại (public)
curl http://localhost:8080/api/v1/sd-jwt/42/skill

# 3. Build presentation: chỉ lộ 3/10 skill
curl -X POST -H "Content-Type: application/json" \
  -d "{\"sdJwt\":\"$SD_JWT\",\"reveal\":[\"Kotlin\",\"Spring\",\"Fabric\"]}" \
  http://localhost:8080/api/v1/sd-jwt/present
# Response: { presentation, hidden: 7 claims }

# Lưu presentation → $PRESENTATION

# 4. Verifier verify
curl -X POST -H "Content-Type: application/json" \
  -d "{\"presentation\":\"$PRESENTATION\",\"requireClaims\":[\"Kotlin\"]}" \
  http://localhost:8080/api/v1/sd-jwt/verify
# Response: valid: true, disclosedClaims chỉ chứa 3 skill, KHÔNG có 7 skill ẩn

# 5. Edge case: revoke holder → verify presentation lại
curl -X PUT -H "Authorization: Bearer $CHIEF_JWT" -d '{"reason":"test"}' \
  http://localhost:8080/api/v1/chief/employees/42/terminate

curl -X POST -H "Content-Type: application/json" \
  -d "{\"presentation\":\"$PRESENTATION\"}" \
  http://localhost:8080/api/v1/sd-jwt/verify
# Response: valid: false, reason: "VC revoked (status list ... index 42)"
```

---

## 9. Mockup UI gợi ý

### 9.1. Wallet card với badge revocation

```
+-------------------------------------+
|  Employment Credential        [OK]  |
|  -------------------------------    |
|  Department: Engineering            |
|  Position:   Senior Developer       |
|                                     |
|  Issued: 2026-05-16                 |
|  Expires: 2027-05-16                |
|                                     |
|  [   ACTIVE   ]   <- StatusList     |
+-------------------------------------+

(Sau revoke:)

+-------------------------------------+
|  Employment Credential       [X]    |
|  -------------------------------    |
|  Department: Engineering            |
|  ...                                |
|                                     |
|  [   REVOKED   ]   <- red           |
|                                     |
|  Revoked at: 2026-05-20             |
|  Reason: CONTRACT_TERMINATED        |
+-------------------------------------+
```

### 9.2. Disclosure Picker

```
+-------------------------------------+
|  Share Skill Credential             |
|  Bank XYZ is requesting             |
|  -------------------------------    |
|                                     |
|  Skills the Verifier wants to see:  |
|  [x] Kotlin              [ADVANCED] |
|  [x] Spring Boot         [ADVANCED] |
|  [x] Hyperledger Fabric  [BEGINNER] |
|                                     |
|  Optional - others you can share:   |
|  [ ] React               [INTERMED] |
|  [ ] Docker              [INTERMED] |
|  [ ] PostgreSQL          [INTERMED] |
|  [ ] AWS                 [BEGINNER] |
|  [ ] Flutter             [INTERMED] |
|  [ ] TypeScript          [ADVANCED] |
|  [ ] GraphQL             [BEGINNER] |
|                                     |
|  ! Verifier will NOT see unchecked  |
|    skills - signature still valid   |
|                                     |
|  [    Sign with Biometric    ]      |
+-------------------------------------+
```

---

## 10. Việc còn lại (chưa làm, để Phase tiếp theo)

- [ ] **Flutter wallet_screen.dart**: render badge ACTIVE/REVOKED theo `credentialStatus` + toggle biometric lock (dùng `BiometricService`)
- [ ] **Flutter verifier_scan_screen.dart**: hiển thị reason mới khi verify trả "revoked", check Trust Registry
- [ ] **Flutter disclosure_picker_screen.dart**: màn hình mới chọn claim để lộ (mockup đã có ở mục 9.2)
- [ ] **Flutter sd_jwt_holder.dart**: model local parse + build presentation
- [ ] **Flutter admin issuance UI**: form issue Skill / Education
- [ ] **Wallet sync**: khi onboard / pull-to-refresh, fetch các SD-JWT mới qua `GET /api/v1/sd-jwt/{employeeId}/{skill,education}` và lưu vào SecureStorage / Hive
- [ ] **ContractSignScreen tích hợp router**: thêm route `/app/contract/:id/sign` trong `app_router.dart`
- [ ] **Production hardening**: HMAC → Ed25519/ECDSA, public domain cho `vc.status-list.base-url`, rotate `sd-jwt.secret`

---

## 11. Reference paths nhanh

| Khái niệm | File backend |
|---|---|
| Status List bitmap + sign | [StatusListService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/StatusListService.kt) |
| Status List API | [StatusListController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/StatusListController.kt) |
| Chaincode Status List | [IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) |
| SD-JWT build | [SdJwtIssuer.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtIssuer.kt) |
| SD-JWT verify | [SdJwtVerifier.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/SdJwtVerifier.kt) |
| SD-JWT API | [SdJwtController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/SdJwtController.kt) |
| VC + credentialStatus + verify upgrade | [VcIssuerService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt) |
| Wire approve | [AdminController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AdminController.kt) |
| Wire terminate | [ChiefController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/ChiefController.kt) |
| New JPA columns | [EmployeeJpaEntity.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/persistence/jpa_entity/EmployeeJpaEntity.kt) |
| Public endpoints config | [SecurityConfig.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/config/SecurityConfig.kt) |
| Properties | [application.properties](fabric-spring-backend/src/main/resources/application.properties) |

---

---

## 12. Phase 1 / 4.3–4.7 — ĐÃ LÀM (2026-05-16)

> Phần này bổ sung vào `updated.md` để UI builder có đủ context triển khai toàn bộ Phase 1.

---

### 12.1. Verifier Portal (4.3) — `verifier-portal/` — ĐÃ TẠO

SPA độc lập Vite 5 + React 18 + TypeScript + Tailwind CSS. **Không cần tài khoản TrustID.**

**Cách chạy:**
```powershell
cd verifier-portal
npm install
npm run dev    # http://localhost:5173
npm run build  # → dist/ (nginx hoặc copy vào resources/static/verifier/)
```

**Dev proxy** (vite.config.ts): `/api` và `/1.0` → `http://localhost:8080` — chạy backend song song là xài được ngay, không cần CORS config thêm.

**Tổng quan các trang:**

| Trang | Route | Mô tả |
|---|---|---|
| Home | `/` | Paste VC/SD-JWT *hoặc* hiển thị QR VP Request cho holder scan |
| Verify Result | `/verify` | Badge VALID / INVALID / REVOKED / ERROR, disclosed claims, DID Document resolution, issuer Trust Registry status |
| Trust Registry | `/trust-registry` | Bảng danh sách trusted issuers từ on-chain, 4 KPI cards, flow explanation |

**API client** ([verifier-portal/src/lib/trustid-client.ts](verifier-portal/src/lib/trustid-client.ts)):

| Function | Backend endpoint | Ghi chú |
|---|---|---|
| `verifyVC(vcJson)` | `POST /api/v1/identity/vc/verify` | W3C VC JSON |
| `verifySdJwt(presentation)` | `POST /api/v1/sd-jwt/verify` | Compact SD-JWT |
| `smartVerify(payload)` | auto-detect | Detect `~` → SD-JWT, ngược lại → VC JSON |
| `resolveDID(did)` | `GET /1.0/identifiers/{did}` | DIF Universal Resolver |
| `listTrustedIssuers()` | `GET /api/v1/trust-registry/issuers` | Public |
| `getStatusList(listId)` | `GET /api/v1/status-list/{listId}` | Public |

**Props VerifyResult state** (truyền qua react-router `navigate('/verify', { state: { result, raw } })`):
```typescript
interface VcVerifyResult {
  valid: boolean
  status: 'VALID' | 'INVALID' | 'REVOKED' | 'EXPIRED' | 'ERROR'
  reason: string
  issuerDid?: string
  subjectDid?: string
  vcType?: string[]
  issuanceDate?: string
  expirationDate?: string
  disclosedClaims?: Record<string, unknown>  // chỉ có khi SD-JWT
  issuerTrusted?: boolean
}
```

---

### 12.2. DID Resolver chuẩn DIF (4.4) — ĐÃ TẠO

**File mới:** [UniversalResolverController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/UniversalResolverController.kt)

**Endpoint:** `GET /1.0/identifiers/{did}` — không cần auth, public.

**Ví dụ:**
```
GET /1.0/identifiers/did:fabric:trustid:42
```

**Response (DIF DID Resolution Result v1):**
```json
{
  "@context": "https://w3id.org/did-resolution/v1",
  "didDocument": {
    "@context": [
      "https://www.w3.org/ns/did/v1",
      "https://w3id.org/security/suites/jws-2020/v1"
    ],
    "id": "did:fabric:trustid:42",
    "controller": "did:fabric:trustid:org1",
    "verificationMethod": [{
      "id": "did:fabric:trustid:42#key-1",
      "type": "JsonWebKey2020",
      "controller": "did:fabric:trustid:42",
      "publicKeyJwk": { "kty": "EC", "crv": "P-256", "x": "...", "y": "..." }
    }],
    "authentication": ["did:fabric:trustid:42#key-1"],
    "assertionMethod": ["did:fabric:trustid:42#key-1"],
    "service": [{
      "id": "did:fabric:trustid:42#trustid-service",
      "type": "TrustIDIdentityService",
      "serviceEndpoint": "https://trustid.example.com/api/v1"
    }]
  },
  "didDocumentMetadata": {
    "deactivated": false,
    "created": "2026-05-16T10:00:00Z",
    "updated": "2026-05-16T10:00:00Z"
  },
  "didResolutionMetadata": {
    "contentType": "application/did+ld+json",
    "retrieved": "2026-05-16T12:00:00Z"
  }
}
```

Nếu DID bị revoke (`status = REVOKED`): `"deactivated": true` + `"deactivatedAt"` + `"deactivationReason"` trong metadata.

Nếu không tìm thấy: HTTP 404.

**Bridge helper thêm vào** [IdentityLedgerBridge.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/fabric/IdentityLedgerBridge.kt):
```kotlin
fun resolveDid(did: String): DIDDocument = ledgerService.resolveDID(did)
```

---

### 12.3. Biometric Unlock + Sign-on-touch (4.5) — ĐÃ TẠO

#### pubspec.yaml
Thêm: `local_auth: ^2.3.0`

#### BiometricService ([lib/core/security/biometric_service.dart](identity_frontend/lib/core/security/biometric_service.dart))

| Method | Mô tả |
|---|---|
| `isAvailable()` | Kiểm tra thiết bị hỗ trợ biometric |
| `availableTypes()` | Danh sách loại biometric (fingerprint/face/iris) |
| `isBiometricLockEnabled()` | Đọc SharedPreferences — toggle từ Wallet screen |
| `setBiometricLockEnabled(bool)` | Lưu SharedPreferences |
| `authenticate({reason})` | Prompt nếu lock enabled; skip nếu tắt hoặc không có biometric |
| `authenticateNow({reason})` | Luôn prompt bất kể lock setting — dùng cho "Sign" button |

#### WalletService ([lib/core/wallet/wallet_service.dart](identity_frontend/lib/core/wallet/wallet_service.dart)) — Phương thức mới

```dart
// Ký payload bytes với ECDSA P-256. Prompt biometric trước khi đọc private key.
static Future<String> sign(
  Uint8List payload, {
  String biometricReason = 'Touch to sign with your identity key',
})
// → Base64(DER signature)
// Throws WalletBiometricException nếu user cancel
// Throws WalletNotInitializedException nếu chưa generate keypair

// Convenience: SHA-256(doc) rồi sign. Dùng cho contract.
static Future<({String signatureBase64, String docHash})> signDocument(
  Uint8List documentBytes, {
  String biometricReason = 'Touch to sign contract',
})
// → record { signatureBase64: Base64(DER), docHash: hex SHA-256 }
```

**Exception classes** (trong cùng file wallet_service.dart):
```dart
class WalletBiometricException implements Exception  // biometric cancel/fail
class WalletNotInitializedException implements Exception  // keypair chưa tạo
```

**Tích hợp toggle biometric vào Wallet screen:**
```dart
final enabled = await BiometricService.isBiometricLockEnabled();
Switch(
  value: enabled,
  onChanged: (v) async {
    if (v) {
      // Verify once before enabling
      final ok = await BiometricService.authenticateNow(reason: 'Confirm to enable App Lock');
      if (!ok) return;
    }
    await BiometricService.setBiometricLockEnabled(v);
    setState(() {});
  },
)
```

---

### 12.4. Trust Registry on-chain (4.6) — ĐÃ TẠO

#### Chaincode methods mới ([IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java))

| Method | Type | Key format | Mô tả |
|---|---|---|---|
| `RegisterIssuer(did, name, role, scope, registeredBy, timestamp)` | SUBMIT | `trustregistry:{did}` | Đăng ký issuer |
| `RevokeIssuer(did, revokedBy, timestamp)` | SUBMIT | `trustregistry:{did}` | Set `active=false` |
| `IsTrustedIssuer(did)` | EVALUATE | — | Trả `true` nếu `active=true` |
| `ListTrustedIssuers()` | EVALUATE | range scan `trustregistry:` | Trả JSON array |

**Issuer record on-chain:**
```json
{
  "did": "did:fabric:trustid:org1",
  "name": "MpCorp HR Issuer",
  "role": "ISSUER",
  "scope": "EmploymentCredential",
  "active": true,
  "registeredBy": "admin@mpcorp.com",
  "registeredAt": "2026-05-16T10:00:00Z",
  "revokedAt": null
}
```

#### REST API ([TrustRegistryController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/TrustRegistryController.kt))

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/trust-registry/issuers` | **Public** | Danh sách tất cả issuers |
| GET | `/api/v1/trust-registry/issuers/{did}/trusted` | **Public** | Check đơn lẻ → `{"did":"...","trusted":true}` |
| POST | `/api/v1/trust-registry/issuers` | CHIEF/ADMIN | Đăng ký issuer mới |
| DELETE | `/api/v1/trust-registry/issuers/{did}` | CHIEF/ADMIN | Revoke issuer |

**Request body POST:**
```json
{
  "did": "did:fabric:trustid:org1",
  "name": "MpCorp HR Issuer",
  "role": "ISSUER",
  "scope": "EmploymentCredential"
}
```

**Response GET /issuers (array):**
```json
[
  {
    "did": "did:fabric:trustid:org1",
    "name": "MpCorp HR Issuer",
    "role": "ISSUER",
    "scope": "EmploymentCredential",
    "active": true,
    "registeredBy": "admin@mpcorp.com",
    "registeredAt": "2026-05-16T10:00:00Z",
    "revokedAt": null
  }
]
```

**Lưu ý cho UI:**
- Verifier Portal đã tích hợp `GET /api/v1/trust-registry/issuers` ở trang Trust Registry.
- Sau khi Trust Registry ship, cập nhật `issuerStats` trong AdminController: thay `trustedIssuers = 1` bằng `fabricBridge.listTrustedIssuers().count { it["active"] == true }`.
- Khi Admin approve account, nên tự động `RegisterIssuer("did:fabric:trustid:org1", ...)` một lần nếu chưa có — hoặc pre-seed khi deploy.

---

### 12.5. E-sign Contract bằng wallet ECDSA (4.7) — ĐÃ TẠO

#### Chaincode methods mới ([IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java))

| Method | Type | Key format | Mô tả |
|---|---|---|---|
| `RecordSignature(contractId, signerDid, signatureBase64, docHash, timestamp, updatedBy)` | SUBMIT | `signature:{contractId}:{signerDid}` | Anchor chữ ký |
| `GetSignatures(contractId)` | EVALUATE | range scan `signature:{contractId}:` | Trả JSON array tất cả chữ ký của contract |

**Signature record on-chain:**
```json
{
  "contractId": "5",
  "signerDid": "did:fabric:trustid:42",
  "signatureBase64": "<Base64 DER ECDSA P-256>",
  "docHash": "<hex SHA-256 of document bytes>",
  "signedAt": "2026-05-16T15:30:00Z",
  "updatedBy": "employee@mpcorp.com"
}
```

#### REST API ([ContractSignatureController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/ContractSignatureController.kt))

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/contracts/{contractId}/sign` | Authenticated | Anchor chữ ký lên Fabric |
| GET | `/api/v1/contracts/{contractId}/signatures` | Authenticated | Lấy danh sách chữ ký |
| POST | `/api/v1/contracts/hash` | Public | Helper compute SHA-256 từ Base64 doc bytes |

**Request body POST sign:**
```json
{
  "signatureBase64": "<Base64 DER ECDSA P-256 signature>",
  "docHash": "<hex SHA-256 of document bytes>",
  "signerDid": "did:fabric:trustid:42"
}
```

**Response POST sign:**
```json
{
  "status": "200",
  "message": "Contract signed and anchored on Fabric",
  "data": {
    "contractId": 5,
    "signerDid": "did:fabric:trustid:42",
    "docHash": "a3f5b2...",
    "anchoredBy": "employee@mpcorp.com"
  }
}
```

**Request body POST hash (helper):**
```json
{ "documentBase64": "<Base64 encoded PDF bytes>" }
```

**Response:**
```json
{ "data": { "docHash": "a3f5b2...", "sizeBytes": 48291 } }
```

#### Flutter ContractSignScreen ([lib/presentation/features/contract/contract_sign_screen.dart](identity_frontend/lib/presentation/features/contract/contract_sign_screen.dart))

**Constructor:**
```dart
ContractSignScreen(
  contractId: 5,               // required — Long contract ID
  contractType: 'FULL_TIME',   // optional display
  startDate: '2026-01-01',     // optional display
  endDate: null,               // null = open-ended
)
```

**UI flow khi tap "Sign with Biometric":**
1. State `authenticating` → OS biometric dialog
2. State `signing` → `WalletService.signDocument(docBytes)` — DER encode P-256 sig
3. State `anchoring` → `POST /api/v1/contracts/{id}/sign`
4. State `done` → success banner + reload signature list

**Demo note**: `docBytes` được build từ `jsonEncode({contractId, type, startDate, endDate, platform})` để demo không cần PDF upload thật. Production: fetch `/contracts/{id}/document` để lấy PDF bytes.

**Tích hợp vào router** (cần làm thêm):
```dart
// Trong app_router.dart, thêm route:
GoRoute(
  path: '/app/contract/:id/sign',
  builder: (context, state) {
    final contractId = int.parse(state.pathParameters['id']!);
    final extra = state.extra as Map<String, dynamic>?;
    return ContractSignScreen(
      contractId: contractId,
      contractType: extra?['contractType'],
      startDate: extra?['startDate'],
      endDate: extra?['endDate'],
    );
  },
)
```

**Navigate từ ContractDetailScreen:**
```dart
context.push(
  '/app/contract/${contract.id}/sign',
  extra: {
    'contractType': contract.typeContract,
    'startDate': contract.startDate,
    'endDate': contract.endDate,
  },
)
```

---

### 12.6. SecurityConfig — public endpoints cần mở thêm

Đảm bảo [SecurityConfig.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/config/SecurityConfig.kt) cho phép unauthenticated với:

```kotlin
.requestMatchers(
    "/api/v1/status-list/**",       // Status List (4.1) — đã có
    "/api/v1/sd-jwt/verify",        // SD-JWT verify (4.2) — đã có
    "/api/v1/sd-jwt/present",       // SD-JWT present (4.2) — đã có
    "/api/v1/sd-jwt/*/skill",       // SD-JWT sync mobile (4.2) — đã có
    "/api/v1/sd-jwt/*/education",   // SD-JWT sync mobile (4.2) — đã có
    "/1.0/identifiers/**",          // DIF Universal Resolver (4.4) — MỚI
    "/api/v1/trust-registry/issuers",          // Trust Registry public list (4.6) — MỚI
    "/api/v1/trust-registry/issuers/*/trusted" // Trust Registry check (4.6) — MỚI
).permitAll()
```

---

### 12.7. Reference paths Phase 1 (4.3–4.7)

| Khái niệm | File |
|---|---|
| Verifier Portal entry | [verifier-portal/src/main.tsx](verifier-portal/src/main.tsx) |
| Verifier Portal API client | [verifier-portal/src/lib/trustid-client.ts](verifier-portal/src/lib/trustid-client.ts) |
| Verifier Portal Home (paste/QR) | [verifier-portal/src/pages/Home.tsx](verifier-portal/src/pages/Home.tsx) |
| Verifier Portal Result | [verifier-portal/src/pages/VerifyResult.tsx](verifier-portal/src/pages/VerifyResult.tsx) |
| Verifier Portal Trust Registry | [verifier-portal/src/pages/TrustRegistry.tsx](verifier-portal/src/pages/TrustRegistry.tsx) |
| DIF Universal Resolver endpoint | [UniversalResolverController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/UniversalResolverController.kt) |
| Biometric service Flutter | [biometric_service.dart](identity_frontend/lib/core/security/biometric_service.dart) |
| Wallet signing + biometric gate | [wallet_service.dart](identity_frontend/lib/core/wallet/wallet_service.dart) |
| Trust Registry REST | [TrustRegistryController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/TrustRegistryController.kt) |
| Contract E-sign REST | [ContractSignatureController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/ContractSignatureController.kt) |
| Contract Sign Screen Flutter | [contract_sign_screen.dart](identity_frontend/lib/presentation/features/contract/contract_sign_screen.dart) |
| Chaincode (Trust Registry + E-sign) | [IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java) |
| Bridge helpers (all 4.4–4.7) | [IdentityLedgerBridge.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/fabric/IdentityLedgerBridge.kt) |
| Service helpers (all 4.4–4.7) | [IdentityLedgerService.kt](fabric-spring-backend/src/main/kotlin/org/fabric/api/service/IdentityLedgerService.kt) |

---

---

## 13. Phase 2 — SHOULD HAVE (Chiều sâu Q&A hội đồng) — ĐÃ LÀM (2026-05-16)

> Toàn bộ Phase 2 đã implement backend. **Không cần thêm migration script** — JPA `ddl-auto=update` tự thêm các cột mới khi backend restart.

---

### 13.1. TOTP 2FA — Admin/Chief (5.1) — ĐÃ TẠO

**Files mới:**
| File | Vai trò |
|---|---|
| [MfaService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/security/MfaService.kt) | Tạo secret, build QR data URI, verify TOTP code, generate backup codes |
| [MfaController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/MfaController.kt) | REST API setup/verify/disable/validate |

**Dependency thêm vào build.gradle:**
```
dev.samstevens.totp:totp:1.7.1
```

**DB columns mới trong `auth` table** (tự migrate qua ddl-auto=update):
- `mfa_secret` — TOTP secret (Base32)
- `mfa_enabled` — Boolean
- `mfa_backup_codes` — JSON array of 8 backup codes

**Endpoints:**

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/mfa/setup` | ADMIN/CHIEF | Generate secret + QR code data URI |
| POST | `/api/v1/mfa/verify-setup` | ADMIN/CHIEF | Confirm 6-digit code → enable MFA, trả backup codes |
| POST | `/api/v1/mfa/disable` | ADMIN/CHIEF | Disable MFA (cần valid TOTP code) |
| POST | `/api/v1/mfa/validate` | **public** | Second-factor login validation (gọi trước khi issue JWT) |
| GET  | `/api/v1/mfa/status/{userId}` | ADMIN/CHIEF | Check MFA status |

**Flow setup MFA từ Flutter:**
```
1. POST /api/v1/mfa/setup {userId} → nhận qrDataUri + secret
2. Hiện qrDataUri (image/png;base64) qua Image.memory — user scan vào Authenticator app
3. User nhập 6 chữ số → POST /api/v1/mfa/verify-setup {userId, code}
4. Response trả backupCodes (8 codes) — hiển thị một lần, lưu cẩn thận
5. Mỗi lần login sau: POST /api/v1/mfa/validate {userId, code} trước khi dùng JWT
```

**Request/Response verify-setup:**
```json
// Request
{ "userId": "uuid-of-admin", "code": "123456" }

// Response 200
{
  "status": "200",
  "message": "MFA enabled",
  "data": {
    "userId": "...",
    "backupCodes": ["ABCD1234", "EFGH5678", "..."],
    "warning": "Store these backup codes safely — they will not be shown again"
  }
}
```

**Backup codes:** 8 codes, 8 ký tự mỗi code. Khi dùng hết → disable và re-setup. Backup codes bị consume khi dùng (xóa khỏi mfa_backup_codes sau verify-setup).

---

### 13.2. Audit Log Viewer on-chain (5.2) — ĐÃ TẠO

**File mới:** [AuditLogController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AuditLogController.kt)

Tận dụng `GetRecordHistory` chaincode đã có — không cần chaincode mới.

**Endpoints:**

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/audit/employees/{employeeId}/history/{type}` | ADMIN/CHIEF | Lịch sử một loại record |
| GET | `/api/v1/audit/employees/{employeeId}/history` | ADMIN/CHIEF | Tất cả record của employee |
| GET | `/api/v1/audit/records` | ADMIN/CHIEF | Full ledger snapshot |
| GET | `/api/v1/audit/records/{type}/{employeeId}` | ADMIN/CHIEF | Record hiện tại của một employee+type |

**Valid record types:** `PROFILE`, `CONTRACT`, `PAYROLL`, `ATTENDANCE`, `REQUEST`, `DID`, `STATUS_LIST`, `CONTRACT_SIGNATURE`, `COMPANY`

**Response mẫu `/history/PROFILE`:**
```json
{
  "status": "200",
  "message": "Record history (3 entries)",
  "data": [
    {
      "recordId": "PROFILE:42",
      "employeeId": "42",
      "recordType": "PROFILE",
      "status": "ACTIVE",
      "keyFields": "{\"name\":\"Nguyen Van A\",\"gender\":\"MALE\",...}",
      "dataHash": "a3f5b2...",
      "action": "CREATE",
      "timestamp": "2026-05-16T10:00:00Z",
      "updatedBy": "admin@mpcorp.com"
    },
    ...
  ]
}
```

**UI Flutter đề xuất** (`audit_log_screen.dart`):
- Timeline vertical (flutter_timeline hoặc custom) — mỗi entry là 1 card
- Card hiển thị: action badge (CREATE/UPDATE/DELETE/REVOKE) màu khác nhau, timestamp, updatedBy, keyFields preview
- Tap vào card → xem full JSON

---

### 13.3. New VC types — TrainingVC + NDA-AcceptedVC (5.3) — ĐÃ TẠO

**File sửa:** [VcIssuerService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt) — thêm 2 methods:
- `issueTrainingVC(employee, trainingName, provider, completedDate, score?)`
- `issueNdaAcceptedVC(employee, ndaTitle, docHash, signedDate)`

**File sửa:** [AdminController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AdminController.kt) — thêm 2 endpoints:

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/admin/employees/{employeeId}/issue-training-vc` | ADMIN/CHIEF | Phát TrainingVC |
| POST | `/api/v1/admin/employees/{employeeId}/issue-nda-vc` | ADMIN/CHIEF | Phát NDA-AcceptedVC |

**Request body issue-training-vc:**
```json
{
  "trainingName": "AWS Solutions Architect",
  "provider": "AWS Training",
  "completedDate": "2026-05-16",
  "score": "92/100"
}
```

**Request body issue-nda-vc:**
```json
{
  "ndaTitle": "General Employee NDA 2026",
  "docHash": "<hex SHA-256 của PDF NDA>",
  "signedDate": "2026-05-16"
}
```

**TrainingVC format:**
```json
{
  "@context": ["https://www.w3.org/2018/credentials/v1"],
  "type": ["VerifiableCredential", "TrainingCredential"],
  "id": "vc:trustid:training:42:...",
  "issuer": "did:fabric:trustid:org1",
  "issuanceDate": "...",
  "expirationDate": "...",
  "credentialSubject": {
    "id": "did:fabric:trustid:42",
    "department": "Engineering",
    "position": "Senior Developer",
    "trainingName": "AWS Solutions Architect",
    "provider": "AWS Training",
    "completedDate": "2026-05-16",
    "status": "COMPLETED",
    "score": "92/100"
  },
  "credentialStatus": { "...Status List block..." },
  "proof": { "...HMAC-SHA256..." }
}
```

---

### 13.4. Account Lockout + Rate Limiting (5.4) — ĐÃ TẠO

**File mới:** [RateLimitFilter.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/security/RateLimitFilter.kt)

**Dependency thêm vào build.gradle:**
```
com.github.bucket4j:bucket4j-core:8.10.1
```

**Rate Limiting policy:**
- Áp dụng cho tất cả `/api/v1/auth/**` endpoints
- 10 requests / minute / IP (token bucket, refill greedy)
- Vượt quá → HTTP 429 với body JSON chuẩn

**Account Lockout** (trong [SignInUseCase.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/application/usecase/auth/SignInUseCase.kt)):
- 5 lần sai mật khẩu liên tiếp → lock 15 phút
- Reset counter khi login thành công
- Thông báo: `"Account locked. Try again in ~X minutes."`

**DB columns mới trong `auth` table** (tự migrate):
- `failed_login_attempts` INT (default 0)
- `locked_until` TIMESTAMP (nullable)

**HTTP responses:**
```json
// Rate limit hit
{"status":"429","message":"Too many requests — please wait before retrying","data":null}

// Account locked
{"status":"429","message":"Account locked due to too many failed attempts. Try again in ~14 minutes.","data":null}
```

---

### 13.5. GDPR Data Export + Right to be Forgotten (5.5) — ĐÃ TẠO

**File mới:** [GdprController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/GdprController.kt)

**Endpoints:**

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| GET | `/api/v1/me/export-data` | Authenticated (any role) | GDPR Art.20 — export all personal data as JSON |
| DELETE | `/api/v1/me/data` | Authenticated (any role) | GDPR Art.17 — soft-delete + revoke VC + RevokeDID |

**Export data response structure:**
```json
{
  "data": {
    "exportedAt": "2026-05-16T12:00:00Z",
    "exportVersion": "1.0",
    "account": { "id", "email", "phone", "role", "mfaEnabled" },
    "identity": { "employeeId", "department", "position", "did", "isActive" },
    "credentials": { "employmentVc", "salaryRangeVc", "promotionVc", "skillSdJwt", "educationSdJwt" },
    "profile": { "name", "gender", "birthDate", "educationLevel", "major", "expYears" },
    "contract": { "type", "startDate", "endDate" },
    "payroll": { "salaryBand", "currency" },
    "attendance": [...],
    "leaveRequests": [...]
  }
}
```

**Delete data flow:**
1. Revoke Status List bit (tất cả VC invalidated)
2. RevokeDID trên Fabric (async, lý do "GDPR_ERASURE")
3. Nullify tất cả VC fields + DID + publicKey trong MySQL
4. Nullify PII trong Profile (name/email/phone/identityNumber/addresses → "[DELETED]")
5. Nullify auth credentials (password/mfa_secret/email obfuscated)
6. On-chain audit hashes KHÔNG bị xóa (blockchain bất biến; chỉ hash on-chain, không có PII)

**Lưu ý GDPR compliance:** Chỉ hashes + keyFields non-sensitive được lưu on-chain — đúng theo yêu cầu GDPR về không lưu PII immutably.

---

### 13.6. Device Binding & Session List (5.6) — ĐÃ TẠO

**Files mới:**
| File | Vai trò |
|---|---|
| [UserSessionJpaEntity.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/persistence/jpa_entity/UserSessionJpaEntity.kt) | Table `user_session` — track active devices |
| [UserSessionJpaRepository.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/persistence/jpa_repository/UserSessionJpaRepository.kt) | JPA queries |
| [SessionsController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/SessionsController.kt) | REST API quản lý session |

**Files sửa:**
- [SignInRequest.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/request/auth/SignInRequest.kt) — thêm `deviceId`, `deviceName`, `devicePlatform` (optional)
- [SignInCommand.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/application/dto/auth/SignInCommand.kt) — thêm device fields
- [SignInUseCase.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/application/usecase/auth/SignInUseCase.kt) — embed `deviceId` vào JWT + auto-create session record
- [JwtUtils.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/common/utils/JwtUtils.kt) — thêm `deviceId` claim + `extractDeviceId()`
- [AuthController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AuthController.kt) — pass device fields từ request → command

**DB table mới** `user_session` (tự migrate qua ddl-auto=update):
```sql
user_session(
  id UUID PK,
  user_id UUID NOT NULL,
  device_id VARCHAR(128) NOT NULL,
  device_name VARCHAR(255),
  device_platform VARCHAR(32),    -- "android" | "ios" | "web"
  last_seen TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  token_hash VARCHAR(64),         -- sha256(jwt) để quick-revoke nếu cần
  UNIQUE (user_id, device_id)
)
```

**Session endpoints:**

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/v1/sessions/register` | Authenticated | Đăng ký / cập nhật session sau khi login |
| GET  | `/api/v1/sessions` | Authenticated | Danh sách active sessions |
| DELETE | `/api/v1/sessions/{deviceId}` | Authenticated | Logout device cụ thể |
| DELETE | `/api/v1/sessions?keepCurrent=true` | Authenticated | Logout tất cả device (trừ current) |

**Sign-in với device binding (Flutter):**
```json
// POST /api/v1/auth/sign-in
{
  "username": "user@example.com",
  "password": "...",
  "deviceId": "flutter-uuid-from-secure-storage",
  "deviceName": "Samsung Galaxy S24",
  "devicePlatform": "android"
}
// Response giống cũ + JWT đã embed deviceId claim
```

**Response GET /sessions:**
```json
{
  "data": [
    {
      "sessionId": "uuid",
      "deviceId": "flutter-uuid",
      "deviceName": "Samsung Galaxy S24",
      "devicePlatform": "android",
      "lastSeen": "2026-05-16T12:30:00Z",
      "createdAt": "2026-05-16T10:00:00Z"
    }
  ]
}
```

**Flutter sessions_screen.dart** (cần tạo):
```dart
// Screen hiển thị danh sách sessions
// Mỗi row: deviceName + platform icon + lastSeen relative time + "Logout" button
// "Logout all other devices" button ở dưới
```

---

### 13.7. SecurityConfig — public endpoints đã cập nhật

[SecurityConfig.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/config/SecurityConfig.kt) — đã mở public thêm:
```kotlin
"/api/v1/mfa/validate",      // MFA second-factor validation
"/1.0/identifiers/**",       // DIF Universal Resolver
"/api/v1/trust-registry/issuers", // Trust Registry public list
```

---

### 13.8. build.gradle.kts — dependencies mới

```kotlin
// TOTP 2FA
implementation("dev.samstevens.totp:totp:1.7.1")
// Rate Limiting
implementation("com.github.bucket4j:bucket4j-core:8.10.1")
```

---

### 13.9. Database schema changes (Phase 2)

JPA `ddl-auto=update` tự thêm khi backend restart. Không cần manual migration.

**Bảng `auth` — cột mới:**
```
mfa_secret             VARCHAR(255)  NULLABLE
mfa_enabled            BOOLEAN       DEFAULT FALSE
mfa_backup_codes       TEXT          NULLABLE
failed_login_attempts  INT           DEFAULT 0
locked_until           TIMESTAMP     NULLABLE
```

**Bảng `user_session` — tạo mới:**
```
id               UUID        PK
user_id          UUID        NOT NULL
device_id        VARCHAR(128) NOT NULL
device_name      VARCHAR(255) NULLABLE
device_platform  VARCHAR(32)  NULLABLE
last_seen        TIMESTAMP   NOT NULL
created_at       TIMESTAMP   NOT NULL
is_active        BOOLEAN     DEFAULT TRUE
token_hash       VARCHAR(64) NULLABLE
UNIQUE(user_id, device_id)
```

---

### 13.10. Việc còn lại (Phase 2 — Flutter UI)

- [ ] **`mfa_setup_screen.dart`**: hiện QR code (decode base64 data URI → Image.memory), form nhập 6 chữ số, show backup codes
- [ ] **`sessions_screen.dart`**: danh sách devices, logout buttons, relative timestamps
- [ ] **`audit_log_screen.dart`**: timeline UI — action badges màu (CREATE=blue, UPDATE=orange, DELETE=red, REVOKE=darkred)
- [ ] **Wallet sync TrainingVC / NDA-AcceptedVC**: thêm card loại mới trong wallet_screen.dart
- [ ] **Admin issue Training/NDA form**: thêm vào Issuer Console action list
- [ ] **GDPR screen**: Settings → Privacy → "Export My Data" button + "Delete My Data" (with confirmation dialog)
- [ ] **Issuer Console stats**: `trustedIssuers` giờ dynamic từ `listTrustedIssuers()` chaincode thay vì hardcoded 1

---

### 13.11. Reference paths Phase 2

| Khái niệm | File |
|---|---|
| TOTP service | [MfaService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/security/MfaService.kt) |
| TOTP API | [MfaController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/MfaController.kt) |
| Audit Log API | [AuditLogController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AuditLogController.kt) |
| TrainingVC + NDA VC | [VcIssuerService.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/vc/VcIssuerService.kt) |
| Rate Limit Filter (Bucket4j) | [RateLimitFilter.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/security/RateLimitFilter.kt) |
| Account Lockout | [SignInUseCase.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/application/usecase/auth/SignInUseCase.kt) |
| GDPR Export + Forget | [GdprController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/GdprController.kt) |
| Session entity | [UserSessionJpaEntity.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/persistence/jpa_entity/UserSessionJpaEntity.kt) |
| Session API | [SessionsController.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/SessionsController.kt) |
| JWT + deviceId claim | [JwtUtils.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/common/utils/JwtUtils.kt) |
| Sign-in + lockout + session | [SignInUseCase.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/application/usecase/auth/SignInUseCase.kt) |
| Auth entity new fields | [AuthJpaEntity.kt](fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/infrastructures/persistence/jpa_entity/AuthJpaEntity.kt) |
