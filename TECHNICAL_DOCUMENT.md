# TÀI LIỆU KỸ THUẬT — TRUSTID IDENTITY FABRIC

> **Phiên bản:** 2.0  
> **Ngày:** 19/04/2026  
> **Công nghệ:** Hyperledger Fabric 2.5.4 · Spring Boot 3 (Kotlin) · Flutter 3

---

## MỤC LỤC

1. [Nền tảng kiến thức](#1-nền-tảng-kiến-thức)
2. [Phân tích thiết kế](#2-phân-tích-thiết-kế)
3. [Thực nghiệm](#3-thực-nghiệm)

---

## 1. Nền tảng kiến thức

### 1.1 Tổng quan hệ thống

**TrustID Identity Fabric** là nền tảng quản lý danh tính nhân viên kết hợp giữa cơ sở dữ liệu quan hệ truyền thống (MySQL) và công nghệ blockchain (Hyperledger Fabric). Hệ thống hướng đến:

- Lưu trữ dữ liệu nhạy cảm an toàn trên MySQL (source of truth).
- Tạo **audit trail bất biến** trên blockchain cho mọi thao tác ghi dữ liệu nhân sự.
- Cung cấp cơ chế **xác minh tính toàn vẹn dữ liệu** (data integrity) bằng hash SHA-256.
- Triển khai **DID (Decentralized Identifier)** theo chuẩn W3C cho từng nhân viên.
- Phát hành **Verifiable Credentials (VC)** — bằng chứng số có thể xác minh về tình trạng việc làm.
- Mobile-first cho nhân viên, web-like dashboard cho quản lý.

---

### 1.2 Lựa chọn công nghệ

#### 1.2.1 Hyperledger Fabric — Blockchain Layer

**Tại sao Hyperledger Fabric?**

| Tiêu chí | Hyperledger Fabric | Ethereum / Public Chain |
|---|---|---|
| Quyền truy cập | **Permissioned** — chỉ tổ chức được cấp phép | Public — ai cũng tham gia |
| Hiệu năng | ~3,000 TPS với cấu hình phù hợp | 15–30 TPS (Ethereum PoW) |
| Dữ liệu riêng tư | Hỗ trợ Private Data Collection | Mọi dữ liệu đều public |
| Chaincode | Java/Go/Node.js — quen thuộc với dev | Solidity — learning curve |
| Chi phí | Không có gas fee | Tốn gas mỗi transaction |
| Phù hợp doanh nghiệp | Đặc biệt thiết kế cho enterprise | Cần L2/custom để đạt enterprise-grade |

**Các khái niệm áp dụng trong dự án:**

- **Channel**: Kênh riêng tư giữa các tổ chức — kiểm soát ai được đọc ledger.
- **MSP (Membership Service Provider)**: Quản lý danh tính node bằng certificate X.509.
- **Endorsement Policy**: `MAJORITY Endorsement` — ít nhất 1 peer Org1 + 1 peer Org2 phải ký.
- **Orderer (etcdraft)**: Sắp xếp thứ tự transaction, đóng block (BatchTimeout: 2s).
- **Chaincode (Smart Contract)**: `IdentityLedger.java` — logic kiểm soát trạng thái ledger.
- **Gossip Protocol**: Đồng bộ ledger giữa các peer trong cùng org.

**Chiến lược Hybrid Storage:**

```
MySQL (off-chain)                  Hyperledger Fabric (on-chain)
─────────────────                  ─────────────────────────────
Toàn bộ dữ liệu gốc   ──────────► SHA-256(fullJson) = dataHash
Dữ liệu nhạy cảm                   keyFields (tóm tắt không nhạy cảm)
(CMND, lương, TK ngân hàng)        action, timestamp, updatedBy
                                   DID Document (public key, status)
                                   VC (Verifiable Credentials)
```

Blockchain **không** lưu dữ liệu thật — chỉ lưu bằng chứng toán học rằng dữ liệu tồn tại và chưa bị thay đổi, cùng với DID Document công khai.

**Chaincode `IdentityLedger` — các hàm:**

| Hàm | Loại | Mô tả |
|---|---|---|
| `UpsertRecord` | SUBMIT | Tạo/cập nhật audit record cho PROFILE/CONTRACT/PAYROLL |
| `DeleteRecord` | SUBMIT | Soft-delete record (giữ history) |
| `GetRecord` | EVALUATE | Lấy trạng thái hiện tại của record |
| `GetRecordHistory` | EVALUATE | Lấy toàn bộ lịch sử transaction của record |
| `GetAllRecordsByEmployee` | EVALUATE | Lấy tất cả record theo employeeId |
| `GetAllRecords` | EVALUATE | Lấy toàn bộ ledger (admin/audit) |
| `RecordExists` | EVALUATE | Kiểm tra record tồn tại |
| `VerifyRecord` | EVALUATE | So sánh hash để xác minh tính toàn vẹn |
| `RegisterDID` | SUBMIT | Đăng ký DID Document mới |
| `RevokeDID` | SUBMIT | Thu hồi DID khi chấm dứt hợp đồng |
| `ResolveDID` | EVALUATE | Tra cứu DID Document theo DID string |

**Key format trên ledger:**
- Record: `"{recordType}:{employeeId}"` — ví dụ: `"profile:42"`, `"contract:42"`
- DID Document: `"did:{did}"` — ví dụ: `"did:did:fabric:trustid:42"`

---

#### 1.2.2 DID & Verifiable Credentials Layer

**DID (Decentralized Identifier):**

Mỗi nhân viên được cấp một DID theo format:
```
did:fabric:trustid:<employeeId>
```

DID Document được lưu trên Fabric ledger, chứa:
- `publicKeyJwk` — ECDSA P-256 public key (do Flutter client sinh, gửi lên lúc onboarding)
- `controller` — `did:fabric:trustid:org1` (tổ chức phát hành)
- `status` — `ACTIVE` | `REVOKED`

**Luồng DID trong hệ thống:**

```
[Onboarding]        [Admin Approve]         [Terminate]
Flutter sinh        Backend gọi              Backend gọi
keypair ECDSA  ──►  FabricBridge            FabricBridge
Gửi publicKey       .registerDID()     ──►  .revokeDID()
lên /employee       → chaincode              → chaincode
                    RegisterDID              RevokeDID
                    → status=ACTIVE          → status=REVOKED
```

**Verifiable Credentials (VC):**

`VcIssuerService` phát hành 4 loại VC theo format W3C VC Data Model v1.1, ký bằng HMAC-SHA256:

| VC Type | Thời điểm cấp | Trường chính |
|---|---|---|
| `EmploymentCredential` | Khi Admin approve tài khoản | department, position, employmentStatus=ACTIVE, startDate |
| `SalaryRangeCredential` | Khi Admin gán payroll | salaryBand (ENTRY/MID/SENIOR/EXECUTIVE), currency |
| `PromotionCredential` | Khi Chief thay đổi role/position | oldPosition, newPosition, promotionDate, promotedBy |
| `TerminationCredential` | Khi Chief terminate nhân viên | employmentStatus=TERMINATED, terminationDate, reason |

**Salary Band:**
- ENTRY: < 10 triệu VND
- MID: 10–20 triệu
- SENIOR: 20–40 triệu
- EXECUTIVE: > 40 triệu

**VP (Verifiable Presentation) — Selective Disclosure:**

`VpService` triển khai OID4VP flow, cho phép nhân viên chỉ tiết lộ một số trường trong VC:

```
[Verifier gửi Auth Request]    [Holder trả VP Token]
nonce, state, requestedClaims  vpToken chứa VC (chỉ fields được chọn)
         ↓                              ↓
VpSessionStore lưu session      VpService.verifyVpToken()
(TTL: 5 phút)                   1. Check nonce khớp session
                                2. Verify proof của từng VC
                                3. Kiểm tra requestedClaims có đủ
```

---

#### 1.2.3 Spring Boot (Kotlin) — Backend Layer

**Tại sao Kotlin thay vì Java thuần?**

- **Null safety**: Compiler bắt lỗi NullPointerException tại compile time.
- **Data class**: Giảm boilerplate cho DTO/Entity (tự sinh equals, hashCode, copy, toString).
- **Coroutines**: Lập trình bất đồng bộ tự nhiên (dùng `@Async` cho Fabric bridge).
- **Extension functions**: Mapper functions (`AuthJpaEntity.toDomainEntity()`) tường minh hơn.
- **Tương thích JVM**: Chạy trên mọi JVM, dùng toàn bộ ecosystem Java.

**Kiến trúc Clean Architecture:**

```
Presentation (Controller/Request/Response)
        │
Application (UseCase/DTO/Command)
        │
Domain (Entity/Repository Interface)
        │
Infrastructure (JPA Entity/Repository Impl/Fabric Bridge/VC)
```

Mỗi layer chỉ phụ thuộc vào layer phía trong — domain không biết Spring, không biết JPA.

**Các pattern áp dụng:**

| Pattern | Áp dụng tại |
|---|---|
| Repository Pattern | `AuthRepository`, `EmployeeRepository`... |
| Use Case Pattern | `SignInUseCase`, `GetProfileUseCase`... |
| Outbox Pattern | `FabricOutboxService` + `FabricRetryScheduler` |
| Mapper Pattern | `AuthMapper.kt`, `EmployeeMapper.kt`... |
| Fire-and-Forget (Async) | `@Async` trên `FabricLedgerBridge` |
| Session Store | `VpSessionStore` cho OID4VP |

**Outbox Pattern — Đảm bảo Eventual Consistency:**

```
Transaction commit MySQL  ──► success → trả về response ngay
                          └─► @Async: gọi Fabric
                                    ├─ success → log INFO, kết thúc
                                    └─ fail → FabricOutboxService.enqueue()
                                              → lưu fabric_outbox_events (status=PENDING)
                                              → FabricRetryScheduler retry (mỗi 30s scan)
                                              → Exponential backoff: 30s, 60s, 120s, 240s, 480s
                                              → Sau 5 lần → DEAD_LETTER, cần xử lý thủ công
```

MySQL luôn là **source of truth**. Blockchain là lớp audit không blocking.

**`FabricLedgerBridge` — các hàm ghi ledger:**

| Hàm | Record Type | Khi nào gọi |
|---|---|---|
| `upsertProfileRecord` | PROFILE | Tạo/cập nhật profile |
| `deleteProfileRecord` | PROFILE | Xóa profile |
| `upsertContractRecord` | CONTRACT | Tạo/cập nhật hợp đồng |
| `deleteContractRecord` | CONTRACT | Xóa hợp đồng |
| `upsertPayrollRecord` | PAYROLL | Tạo/cập nhật bảng lương |
| `deletePayrollRecord` | PAYROLL | Xóa bảng lương |
| `logAttendance` | ATTENDANCE | Check-in / Check-out |
| `logRequest` | REQUEST | Tạo/duyệt/từ chối đơn từ |
| `logCompany` | COMPANY | Tạo/cập nhật thông tin công ty |
| `registerDID` | DID | Admin approve tài khoản |
| `revokeDID` | DID | Chief terminate nhân viên |

**keyFields — Partial Snapshot (không chứa PII):**

| Record Type | keyFields |
|---|---|
| PROFILE | name, gender, educationLevel, major, expYears, email |
| CONTRACT | typeContract, startDate, endDate, contractExpire |
| PAYROLL | salaryType, currency, totalIncome, payDay, bankName |

---

#### 1.2.4 Flutter — Mobile Frontend

**Tại sao Flutter?**

- **Cross-platform**: 1 codebase cho Android + iOS.
- **BLoC pattern**: Tách biệt rõ business logic khỏi UI — dễ test.
- **GoRouter**: Declarative routing với role-based guard.
- **Dio**: HTTP client mạnh, dễ add interceptor (attach JWT token).
- **Equatable**: So sánh state objects hiệu quả, tránh rebuild thừa.

**Các pattern áp dụng:**

| Pattern | Áp dụng tại |
|---|---|
| BLoC (Business Logic Component) | `AuthBloc`, `ProfileBloc`, `AttendanceBloc`... |
| Repository Pattern | `AuthRepository`, `EmployeeRepository`... |
| Dependency Injection | `GetIt` (`sl<UseCase>()`) |
| Clean Architecture | data / domain / presentation layers |

---

### 1.3 Luồng xác thực (JWT)

```
Client                Spring Boot              MySQL
  │                       │                     │
  ├─ POST /auth/sign-in ──►│                     │
  │                       ├─ findByUsername ────►│
  │                       │◄──── AuthEntity ─────┤
  │                       ├─ check password      │
  │                       ├─ check accountStatus │
  │                       ├─ generateToken(JWT)  │
  │◄── { token, role } ───┤                     │
  │                       │                     │
  ├─ GET /api/v1/profile ─►│                     │
  │  (Authorization: Bearer <token>)             │
  │                       ├─ JwtAuthFilter       │
  │                       ├─ verify & decode JWT │
  │                       ├─ set SecurityContext │
  │                       ├─ process request ───►│
  │◄── { profile data } ──┤                     │
```

---

## 2. Phân tích thiết kế

### 2.1 Use Case Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TrustID System                              │
│                                                                     │
│  ┌──────────┐    ┌─────────────────────────────────────────────┐   │
│  │          │    │                                             │   │
│  │ EMPLOYEE │───►│  Đăng ký tài khoản                         │   │
│  │          │───►│  Đăng nhập                                  │   │
│  │          │───►│  Xem hồ sơ cá nhân                         │   │
│  │          │───►│  Xem hợp đồng                              │   │
│  │          │───►│  Xem bảng lương                            │   │
│  │          │───►│  Chấm công (check-in / check-out)          │   │
│  │          │───►│  Xem lịch sử chấm công                     │   │
│  │          │───►│  Tạo đơn từ (nghỉ phép / WFH / công tác)  │   │
│  │          │───►│  Xem danh sách nhân viên                   │   │
│  │          │───►│  Xem thông tin công ty                     │   │
│  └──────────┘    └─────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────┐    ┌─────────────────────────────────────────────┐   │
│  │          │    │  (kế thừa tất cả quyền EMPLOYEE)            │   │
│  │ MANAGER  │───►│  Xem đơn từ cấp dưới                       │   │
│  │          │───►│  Duyệt / từ chối đơn từ                    │   │
│  └──────────┘    └─────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────┐    ┌─────────────────────────────────────────────┐   │
│  │          │    │  (kế thừa tất cả quyền MANAGER)             │   │
│  │  CHIEF   │───►│  Quản lý nhân viên (xem, tạo mới)          │   │
│  │          │───►│  Thay đổi role nhân viên → phát PromotionVC │   │
│  │          │───►│  Chấm dứt hợp đồng → RevokeDID + TerminVC  │   │
│  │          │───►│  Xem Blockchain Ledger (audit trail)        │   │
│  └──────────┘    └─────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────┐    ┌─────────────────────────────────────────────┐   │
│  │          │    │  (kế thừa tất cả quyền CHIEF)               │   │
│  │  ADMIN   │───►│  Xem dashboard thống kê                    │   │
│  │          │───►│  Duyệt tài khoản → RegisterDID + EmployVC  │   │
│  │          │───►│  Từ chối tài khoản (PENDING → REJECTED)    │   │
│  │          │───►│  Phát hành SalaryRangeVC                   │   │
│  └──────────┘    └─────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────┐    ┌─────────────────────────────────────────────┐   │
│  │  FABRIC  │    │                                             │   │
│  │ NETWORK  │───►│  Ghi audit record (UpsertRecord)            │   │
│  │          │───►│  Xóa mềm record (DeleteRecord)             │   │
│  │          │───►│  Xác minh hash toàn vẹn (VerifyRecord)     │   │
│  │          │───►│  Truy vấn lịch sử (GetRecordHistory)       │   │
│  │          │───►│  Đăng ký DID (RegisterDID)                 │   │
│  │          │───►│  Thu hồi DID (RevokeDID)                   │   │
│  │          │───►│  Tra cứu DID (ResolveDID)                  │   │
│  └──────────┘    └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 2.2 Kịch bản Use Case

#### UC-01: Đăng ký tài khoản

| Trường | Nội dung |
|---|---|
| **Actor** | Nhân viên mới |
| **Mục tiêu** | Tạo tài khoản và thiết lập hồ sơ công việc |
| **Tiền điều kiện** | Chưa có tài khoản trong hệ thống |
| **Hậu điều kiện** | Tài khoản ở trạng thái PENDING, chờ Admin duyệt |
| **Luồng chính** | 1. Nhân viên nhập email, số điện thoại, mật khẩu → 2. Hệ thống kiểm tra trùng lặp → 3. Tạo AuthEntity với `accountStatus=PENDING` → 4. Trả về JWT token → 5. Frontend chuyển đến màn hình Onboarding → 6. Nhân viên điền phòng ban, chức vụ, loại hình làm việc; Flutter sinh ECDSA keypair, gửi publicKey → 7. Tạo EmployeeEntity (lưu publicKey) → 8. Chờ Admin duyệt |
| **Luồng thay thế** | 2a. Email/phone đã tồn tại (và isActive=true) → báo lỗi "Tài khoản đã tồn tại" |

#### UC-02: Đăng nhập

| Trường | Nội dung |
|---|---|
| **Actor** | Nhân viên |
| **Mục tiêu** | Xác thực và nhận JWT token |
| **Tiền điều kiện** | Có tài khoản trong hệ thống |
| **Hậu điều kiện** | Nhận JWT token, điều hướng theo role |
| **Luồng chính** | 1. Nhập email/phone + mật khẩu → 2. Xác thực thông tin → 3. Kiểm tra accountStatus → 4. Tạo JWT(userId, role) → 5. Lưu token local → 6. Vào Home |
| **Luồng thay thế** | 3a. accountStatus=PENDING → lỗi 403 "Tài khoản chờ duyệt" / 3b. accountStatus=REJECTED → lỗi 403 "Tài khoản bị từ chối" |

#### UC-03: Chấm công Check-in

| Trường | Nội dung |
|---|---|
| **Actor** | Nhân viên |
| **Mục tiêu** | Ghi nhận giờ vào làm |
| **Tiền điều kiện** | Đã đăng nhập, chưa check-in hôm nay |
| **Hậu điều kiện** | Bản ghi attendance được lưu MySQL + async ghi Fabric |
| **Luồng chính** | 1. Nhấn Check-in → 2. Backend tạo AttendanceEntity(workDate=today, checkInTime=now) → 3. Commit MySQL → 4. Async: `FabricLedgerBridge.logAttendance(employeeId, "CHECK_IN")` → 5. Fabric ghi record ATTENDANCE |
| **Luồng thay thế** | 4a. Fabric lỗi → Outbox enqueue → retry với exponential backoff |

#### UC-04: Tạo đơn nghỉ phép

| Trường | Nội dung |
|---|---|
| **Actor** | Nhân viên |
| **Mục tiêu** | Gửi đơn xin nghỉ phép đến Manager |
| **Tiền điều kiện** | Đã đăng nhập |
| **Hậu điều kiện** | Đơn ở trạng thái PENDING, Manager nhận được thông báo |
| **Luồng chính** | 1. Chọn loại đơn LEAVE → 2. Chọn ngày bắt đầu/kết thúc → 3. Chọn buổi (cả ngày/sáng/chiều) → 4. Nhập lý do → 5. Submit → 6. LeaveRequestEntity(status=PENDING) lưu MySQL → 7. Async ghi Fabric REQUEST record |
| **Luồng thay thế** | Không có Manager → đơn vẫn tạo được, không có người duyệt |

#### UC-05: Admin duyệt tài khoản → cấp DID + EmploymentVC

| Trường | Nội dung |
|---|---|
| **Actor** | Admin |
| **Mục tiêu** | Phê duyệt đăng ký; cấp DID và EmploymentVC |
| **Tiền điều kiện** | Có tài khoản PENDING trong hệ thống |
| **Hậu điều kiện** | accountStatus=ACTIVE; DID on Fabric; EmploymentVC lưu DB |
| **Luồng chính** | 1. Admin vào Dashboard → thấy pendingAccounts → 2. PUT `/admin/accounts/{id}/approve` → 3. accountStatus=ACTIVE → 4. Backend lấy publicKey từ employee → 5. Async: `fabricBridge.registerDID()` → RegisterDID trên Fabric → 6. `vcIssuerService.issueEmploymentVC()` → lưu VC vào employee.employmentVc |
| **Luồng thay thế** | 4a. publicKey null → bỏ qua RegisterDID (không ghi DID lên Fabric) |

#### UC-06: Chief terminate nhân viên → RevokeDID + TerminationVC

| Trường | Nội dung |
|---|---|
| **Actor** | Chief / Admin |
| **Mục tiêu** | Chấm dứt hợp đồng và thu hồi danh tính số |
| **Tiền điều kiện** | Nhân viên đang ACTIVE |
| **Hậu điều kiện** | isActive=false, status=TERMINATED; DID REVOKED; TerminationVC lưu DB |
| **Luồng chính** | 1. PUT `/chief/employees/{id}/terminate` với reason → 2. employee.isActive=false, status=TERMINATED → 3. Async: `fabricBridge.revokeDID()` → RevokeDID chaincode → 4. `vcIssuerService.issueTerminationVC()` → lưu vào employee.terminationVc |

#### UC-07: Chief thay đổi role → PromotionVC

| Trường | Nội dung |
|---|---|
| **Actor** | Chief / Admin |
| **Mục tiêu** | Thăng chức/đổi vị trí và ghi nhận bằng VC |
| **Tiền điều kiện** | Nhân viên đang ACTIVE |
| **Hậu điều kiện** | Role/position cập nhật; PromotionVC lưu DB; ghi Fabric REQUEST |
| **Luồng chính** | 1. PUT `/chief/employees/{id}/role` với role, position → 2. Cập nhật auth.role + emp.position → 3. `vcIssuerService.issuePromotionVC(oldPosition, newPosition)` → lưu vào employee.promotionVc → 4. `fabricBridge.logRequest(id, "ROLE_CHANGE", role, actor)` |

#### UC-08: Verify tính toàn vẹn dữ liệu

| Trường | Nội dung |
|---|---|
| **Actor** | Chief / Admin |
| **Mục tiêu** | Xác minh dữ liệu MySQL chưa bị tamper |
| **Tiền điều kiện** | Record đã tồn tại trên Fabric |
| **Hậu điều kiện** | Nhận kết quả valid/invalid + lý do |
| **Luồng chính** | 1. Gọi `GET /ledger/records/{employeeId}/{recordType}/verify?hash={hash}` → 2. Backend tính SHA-256 của dữ liệu MySQL hiện tại → 3. Chaincode `VerifyRecord()` so sánh hash → 4. Trả về `{valid, reason, storedHash, providedHash}` |

---

### 2.3 Sequence Diagrams

#### SD-01: Tạo Profile và ghi lên Blockchain

```
Flutter          Spring Boot          MySQL           Fabric
  │                  │                  │               │
  ├──POST /profile──►│                  │               │
  │                  ├─ CreateProfile   │               │
  │                  │  UseCase         │               │
  │                  ├─ save(Profile)──►│               │
  │                  │◄─ ProfileEntity ─┤               │
  │                  ├─ COMMIT          │               │
  │◄── 201 Created ──┤                  │               │
  │                  │                  │               │
  │                  ├─── @Async ───────┼───────────────►
  │                  │  FabricLedger    │               │
  │                  │  Bridge          │               │
  │                  │  sha256(full) ──►│               │
  │                  │  upsertRecord────┼──────────────►│
  │                  │                  │  UpsertRecord │
  │                  │                  │  putState()   │
  │                  │◄── success ───── ┼───────────────┤
  │                  │  log INFO        │               │
```

#### SD-02: Fabric ghi lỗi → Outbox Exponential Backoff Retry

```
FabricLedgerBridge   FabricOutboxService   FabricRetryScheduler   Fabric
       │                    │                      │                 │
       ├─ upsertRecord ─────┼──────────────────────┼────────────────►│
       │◄── Exception ──────┼──────────────────────┼─────────────────┤
       │                    │                      │                 │
       ├─ enqueue() ───────►│                      │                 │
       │                    ├─ save(status=PENDING) │                 │
       │                    ├─ nextRetryAt=now+30s  │                 │
       │                    │                      │                 │
       │                    │◄── @Scheduled ───────┤                 │
       │                    │  processDueEvents()   │                 │
       │                    ├─ upsertRecord ────────┼────────────────►│
       │                    │◄── success ───────────┼─────────────────┤
       │                    ├─ status=COMPLETED     │                 │
       │                    │                      │                 │
       │                    │  [Nếu lỗi lần 2:]    │                 │
       │                    ├─ retryCount=2         │                 │
       │                    ├─ nextRetryAt=now+60s  │                 │
       │                    │  [Retry 3: now+120s]  │                 │
       │                    │  [Retry 4: now+240s]  │                 │
       │                    │  [Retry 5: now+480s]  │                 │
       │                    │  [Retry 6 → MAX:]     │                 │
       │                    ├─ status=DEAD_LETTER   │                 │
       │                    ├─ log ERROR            │                 │
```

#### SD-03: Đăng ký → Onboarding → Admin approve → DID + VC

```
Employee App        Backend          Admin App        Fabric
     │                 │                 │              │
     ├─ POST /signUp──►│                 │              │
     │                 ├─ create Auth    │              │
     │                 │  PENDING        │              │
     │◄─ JWT token ────┤                 │              │
     │                 │                 │              │
     ├─ POST /employee►│                 │              │
     │  (dept, pos,    │                 │              │
     │   publicKeyJwk) │                 │              │
     │                 ├─ create Emp     │              │
     │                 │  (lưu publicKey)│              │
     │◄─ EmployeeEntity┤                 │              │
     │                 │                 │              │
     │                 │          ┌─────►│              │
     │                 │          │  GET /admin/pending │
     │                 │          │◄─── [list PENDING]  │
     │                 │          │  PUT /approve/{id}  │
     │                 ├◄─────────┘  status=ACTIVE      │
     │                 ├─ @Async: registerDID ──────────►│
     │                 │  (employeeId, publicKeyJwk)     │
     │                 │                 │  RegisterDID  │
     │                 │                 │  putState()   │
     │                 ├─ issueEmploymentVC              │
     │                 │  lưu vào employee.employmentVc  │
     │                 │                 │              │
     ├─ POST /signIn──►│                 │              │
     │                 ├─ status=ACTIVE → OK             │
     │◄─ JWT(authorized│                 │              │
```

#### SD-04: Chief terminate → RevokeDID + TerminationVC

```
Chief App         Backend          Fabric
    │                │               │
    ├─ PUT /terminate►│               │
    │  {reason}      │               │
    │                ├─ emp.isActive=false     │
    │                ├─ emp.status=TERMINATED  │
    │                ├─ save(emp)    │         │
    │◄─ 200 OK ──────┤               │
    │                │               │
    │                ├─ @Async: revokeDID ────►│
    │                │               │  RevokeDID
    │                │               │  status=REVOKED
    │                ├─ issueTerminationVC     │
    │                │  lưu terminationVc      │
```

---

### 2.4 Bảng Database

#### Bảng `auth`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | UUID | PK, AUTO | Định danh tài khoản |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Email đăng nhập |
| `phone` | VARCHAR(20) | NOT NULL | Số điện thoại |
| `password` | VARCHAR(255) | NOT NULL | Mật khẩu (hash BCrypt) |
| `role` | ENUM | NOT NULL, DEFAULT 'EMPLOYEE' | EMPLOYEE / MANAGER / CHIEF / ADMIN |
| `account_status` | ENUM | NOT NULL, DEFAULT 'ACTIVE' | PENDING / ACTIVE / REJECTED |

#### Bảng `employee`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Định danh nhân viên |
| `auth_id` | UUID | FK → auth.id, UNIQUE | Liên kết tài khoản |
| `department` | VARCHAR(100) | NOT NULL | Phòng ban |
| `position` | VARCHAR(100) | NOT NULL | Chức vụ |
| `status` | VARCHAR(50) | NOT NULL | ACTIVE / TERMINATED |
| `working_type` | VARCHAR(50) | NOT NULL | FULL_TIME / PART_TIME |
| `is_active` | BOOLEAN | NOT NULL | Đang làm việc hay không |
| `manager_id` | BIGINT | FK → employee.id, NULL | Self-join quản lý |
| `public_key` | TEXT | NULL | ECDSA P-256 public key (JWK) từ Flutter |
| `did` | VARCHAR(255) | NULL | DID string sau khi được approve |
| `employment_vc` | TEXT | NULL | EmploymentVC JSON (W3C format) |
| `salary_range_vc` | TEXT | NULL | SalaryRangeVC JSON |
| `promotion_vc` | TEXT | NULL | PromotionVC JSON (lần thăng chức gần nhất) |
| `termination_vc` | TEXT | NULL | TerminationVC JSON |
| `created_at` | TIMESTAMP | NOT NULL | Ngày tạo |
| `updated_at` | TIMESTAMP | NOT NULL | Ngày cập nhật |
| `created_by` | VARCHAR(100) | NOT NULL | Người tạo |
| `note` | TEXT | NULL | Ghi chú |

#### Bảng `profile`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `employee_id` | BIGINT | FK, UNIQUE | 1-1 với employee |
| `name` | VARCHAR(150) | NOT NULL | Họ tên đầy đủ |
| `gender` | VARCHAR(10) | NOT NULL | MALE / FEMALE / OTHER |
| `identity_type` | VARCHAR(20) | NOT NULL | CCCD / CMND / Hộ chiếu |
| `identity_number` | VARCHAR(20) | UNIQUE, NOT NULL | Số CCCD/CMND |
| `identity_issue_date` | INT | NOT NULL | Năm cấp |
| `identity_issue_place` | VARCHAR(200) | NULL | Nơi cấp |
| `email` | VARCHAR(255) | NOT NULL | Email cá nhân |
| `phone` | VARCHAR(20) | NOT NULL | Điện thoại cá nhân |
| `emergency_name` | VARCHAR(150) | NULL | Người liên hệ khẩn cấp |
| `emergency_phone` | VARCHAR(20) | NULL | SĐT khẩn cấp |
| `emergency_relationship` | VARCHAR(50) | NULL | Quan hệ |
| `date_of_birth` | VARCHAR(10) | NULL | Ngày sinh (YYYY-MM-DD) |
| `health` | VARCHAR(50) | NULL | Nhóm máu, tình trạng |
| `married` | VARCHAR(20) | NULL | Tình trạng hôn nhân |
| `permanent_residence` | TEXT | NULL | Địa chỉ thường trú |
| `now_residence` | TEXT | NULL | Địa chỉ tạm trú |
| `avatar_url` | TEXT | NULL | URL ảnh đại diện |
| `education_level` | VARCHAR(50) | NULL | Trình độ học vấn |
| `major` | VARCHAR(150) | NULL | Chuyên ngành |
| `exp_years` | INT | NULL | Số năm kinh nghiệm |

**Bảng phụ** (ElementCollection):
- `employee_certificates(employee_id, certificate_name)`
- `employee_skills(employee_id, skill_name)`

#### Bảng `contract`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `employee_id` | BIGINT | FK, UNIQUE | 1-1 với employee |
| `type_contract` | VARCHAR(50) | NOT NULL | Loại hợp đồng |
| `start_date` | TIMESTAMP | NOT NULL | Ngày bắt đầu |
| `end_date` | TIMESTAMP | NULL | Ngày kết thúc (null = vô thời hạn) |
| `contract_expire` | TIMESTAMP | NULL | Ngày hết hiệu lực |
| `probation_start_date` | TIMESTAMP | NULL | Ngày bắt đầu thử việc |
| `probation_end_date` | TIMESTAMP | NULL | Ngày kết thúc thử việc |
| `tax_code` | VARCHAR(20) | UNIQUE, NOT NULL | Mã số thuế cá nhân |
| `social_insurance_number` | VARCHAR(20) | UNIQUE, NULL | Số BHXH |
| `health_insurance_number` | VARCHAR(20) | UNIQUE, NULL | Số BHYT |

#### Bảng `payroll`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `employee_id` | BIGINT | FK, UNIQUE | 1-1 với employee |
| `salary_type` | VARCHAR(50) | NOT NULL | GROSS / NET |
| `base_salary` | DOUBLE | NOT NULL | Lương cơ bản |
| `bonus_amount` | DOUBLE | NULL | Phụ cấp / thưởng |
| `over_time_rate` | DOUBLE | NULL | Hệ số OT |
| `total_income` | DOUBLE | NOT NULL | Tổng thu nhập |
| `currency` | VARCHAR(10) | NOT NULL | VND / USD |
| `payday` | TIMESTAMP | NOT NULL | Ngày thanh toán lương |
| `bank_account_number` | VARCHAR(30) | NOT NULL | Số tài khoản |
| `bank_account_name` | VARCHAR(150) | NOT NULL | Tên chủ TK |
| `bank_name` | VARCHAR(100) | NOT NULL | Tên ngân hàng |
| `bank_branch` | VARCHAR(100) | NULL | Chi nhánh |

#### Bảng `attendance`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `employee_id` | BIGINT | FK | |
| `work_date` | DATE | NOT NULL | Ngày làm việc |
| `check_in_time` | TIMESTAMP | NULL | Giờ check-in |
| `check_out_time` | TIMESTAMP | NULL | Giờ check-out |
| `check_in_location` | TEXT | NULL | Vị trí check-in |
| `check_out_location` | TEXT | NULL | Vị trí check-out |
| `status` | VARCHAR(20) | NOT NULL | PRESENT / LATE / HALF_DAY / ABSENT |
| `note` | TEXT | NULL | Ghi chú |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

> **UNIQUE(employee_id, work_date)**: mỗi nhân viên chỉ có 1 bản ghi mỗi ngày.

#### Bảng `leave_request`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `employee_id` | BIGINT | FK | Người gửi đơn |
| `request_type` | VARCHAR(50) | NOT NULL | LEAVE / WFH / BUSINESS_TRIP / ATTENDANCE_CORRECTION |
| `status` | VARCHAR(20) | NOT NULL | PENDING / APPROVED / REJECTED |
| `start_date` | DATE | NOT NULL | Ngày bắt đầu |
| `end_date` | DATE | NOT NULL | Ngày kết thúc |
| `session` | VARCHAR(20) | NULL | FULL_DAY / MORNING / AFTERNOON |
| `reason` | TEXT | NOT NULL | Lý do |
| `photo_url` | TEXT | NULL | Ảnh đính kèm |
| `approver_id` | BIGINT | FK → employee.id, NULL | Manager duyệt |
| `approved_at` | TIMESTAMP | NULL | Thời điểm duyệt |
| `rejected_reason` | TEXT | NULL | Lý do từ chối |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |

#### Bảng `company`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `tax_code` | VARCHAR(20) | UNIQUE, NOT NULL | MST công ty |
| `company_name` | VARCHAR(255) | NOT NULL | Tên công ty |
| `legal_rep_name` | VARCHAR(150) | NOT NULL | Tên người đại diện |
| `legal_rep_title` | VARCHAR(100) | NOT NULL | Chức danh đại diện |
| `legal_rep_id_number` | VARCHAR(20) | NOT NULL | CCCD người đại diện |
| `address` | TEXT | NOT NULL | Địa chỉ trụ sở |
| `phone` | VARCHAR(20) | NOT NULL | Điện thoại |
| `email` | VARCHAR(255) | NOT NULL | Email |
| `registered_at` | DATE | NOT NULL | Ngày đăng ký thành lập |
| `created_at` | TIMESTAMP | NOT NULL | |
| `updated_at` | TIMESTAMP | NOT NULL | |
| `created_by` | VARCHAR(100) | NOT NULL | |

#### Bảng `fabric_outbox_events`

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK | |
| `employee_id` | VARCHAR(50) | NOT NULL | ID nhân viên |
| `record_type` | VARCHAR(20) | NOT NULL | PROFILE / CONTRACT / PAYROLL / ATTENDANCE / REQUEST / DID |
| `record_status` | VARCHAR(20) | NOT NULL | ACTIVE / DELETED / REVOKED |
| `key_fields` | TEXT | NOT NULL | JSON summary (non-PII) |
| `data_hash` | VARCHAR(64) | NOT NULL | SHA-256 hex |
| `action` | VARCHAR(20) | NOT NULL | CREATE / UPDATE / DELETE |
| `updated_by` | VARCHAR(100) | NOT NULL | Actor |
| `event_status` | ENUM | NOT NULL | PENDING / RETRYING / COMPLETED / DEAD_LETTER |
| `retry_count` | INT | NOT NULL, DEFAULT 0 | Số lần retry (tối đa 5) |
| `created_at` | TIMESTAMP | NOT NULL | |
| `last_attempt_at` | TIMESTAMP | NULL | Lần thử gần nhất |
| `next_retry_at` | TIMESTAMP | NULL | Lần thử tiếp theo |
| `last_error` | TEXT | NULL | Thông báo lỗi cuối (tối đa 500 ký tự) |

> **INDEX(event_status, next_retry_at)**: tối ưu truy vấn của `FabricRetryScheduler`.

---

### 2.5 ERD Tóm tắt

```
auth ──(1:1)── employee ──(1:1)── profile
                   │
                   ├──(1:1)── contract
                   │
                   ├──(1:1)── payroll
                   │
                   ├──(1:N)── attendance
                   │
                   ├──(1:N)── leave_request
                   │
                   └──(N:1)── employee (self-join: manager)

company (độc lập)
fabric_outbox_events (độc lập — hàng đợi retry)

[Fabric Ledger]
  ├── "{recordType}:{employeeId}" → IdentityRecord (PROFILE/CONTRACT/PAYROLL/...)
  └── "did:{did}" → DIDDocument
```

---

## 3. Thực nghiệm

### 3.1 Luồng sử dụng đầy đủ

#### Luồng 1: Nhân viên mới đăng ký lần đầu

```
[Sign Up Screen]
    Nhập: email, phone, password, confirm password
         ↓ POST /auth/sign-up
[Onboarding Screen]
    Nhập: phòng ban, chức vụ, loại hình làm việc
    Flutter: sinh ECDSA P-256 keypair, lưu privateKey vào Secure Storage
         ↓ POST /employee (kèm publicKeyJwk)
[Home Screen — bị giới hạn đến khi Admin duyệt]
    Banner: "Tài khoản đang chờ Admin duyệt"
         ↓
[Admin: PUT /admin/accounts/{id}/approve]
    → accountStatus=ACTIVE
    → @Async: RegisterDID trên Fabric
    → issueEmploymentVC → lưu vào employee.employmentVc
         ↓
[Nhân viên login lại → vào đầy đủ tính năng]
```

#### Luồng 2: Nhân viên vào làm mỗi ngày

```
[Attendance Screen]
    Nhấn Check-in
         ↓ POST /attendance/check-in {location}
    Backend: tạo AttendanceEntity, async ghi Fabric ATTENDANCE
    Xem giờ check-in hiện tại
         ↓ (khi ra về)
    Nhấn Check-out
         ↓ POST /attendance/check-out
    Backend: cập nhật AttendanceEntity.checkOutTime
```

#### Luồng 3: Xin nghỉ phép

```
[Request List Screen]
    Nhấn (+) Tạo đơn mới
         ↓
[Create Request Screen]
    Chọn loại: LEAVE / WFH / BUSINESS_TRIP / ATTENDANCE_CORRECTION
    Chọn ngày bắt đầu, ngày kết thúc
    Chọn buổi: FULL_DAY / MORNING / AFTERNOON
    Nhập lý do (min 10 ký tự)
    (Tuỳ chọn) đính kèm ảnh
         ↓ POST /requests
[Request List Screen]
    Đơn hiện ở tab "Đang chờ" với status PENDING
         ↓
[Manager: PUT /manager/requests/{id}/approve]
    Status chuyển sang APPROVED
    Async ghi Fabric REQUEST record
```

#### Luồng 4: Admin phát hành SalaryRangeVC

```
[Admin Panel]
    PUT /admin/employees/{employeeId}/issue-salary-vc
         ↓
    Backend lấy payroll.baseSalary
    Tính salary band: ENTRY/MID/SENIOR/EXECUTIVE
    issuesSalaryRangeVC (W3C VC, ký HMAC-SHA256)
    Lưu vào employee.salaryRangeVc
         ↓
[Nhân viên dùng SalaryRangeVC để chứng minh mức lương
 với bên thứ ba qua VP flow — không lộ số lương thực]
```

#### Luồng 5: Terminate nhân viên

```
[Chief Panel]
    PUT /chief/employees/{id}/terminate {reason}
         ↓
    emp.isActive=false, status=TERMINATED → lưu MySQL
    @Async: fabricBridge.revokeDID(employeeId, revokedBy, reason)
        → RevokeDID chaincode → DIDDocument.status=REVOKED
    vcIssuerService.issueTerminationVC()
        → lưu employee.terminationVc
```

#### Luồng 6: Admin kiểm tra audit trail + verify integrity

```
[Admin Dashboard]
    Xem thống kê: totalEmployees, activeEmployees, todayAttendance, pendingRequests, pendingAccounts
         ↓
[Ledger Screen]
    GET /ledger/records → danh sách: recordType, action, timestamp, dataHash
         ↓ Nhấn vào 1 record
    GET /ledger/records/{employeeId}/{recordType}/history
    Xem lịch sử đầy đủ với txId, timestamp từng transaction
         ↓ Verify integrity
    GET /ledger/records/{employeeId}/{recordType}/verify?hash={hash}
    Backend tính lại SHA-256 của MySQL → so sánh với on-chain hash
    Kết quả: valid ✓ / invalid ✗ (dữ liệu bị tamper)
```

---

### 3.2 Model từng màn hình nhập dữ liệu

#### Màn hình Đăng ký (Sign Up)

| Field | Kiểu nhập | Validate | Ghi chú |
|---|---|---|---|
| Email | TextInput (email keyboard) | format email, required | Dùng để đăng nhập |
| Số điện thoại | TextInput (phone keyboard) | required | |
| Mật khẩu | TextInput (obscured) | required, min 6 ký tự | Hiện/ẩn toggle |
| Xác nhận mật khẩu | TextInput (obscured) | phải khớp password | |

**API call:** `POST /api/v1/auth/sign-up`
```json
{ "email": "nv@company.com", "phone": "0901234567", "password": "..." }
```

---

#### Màn hình Onboarding (Thiết lập công việc)

| Field | Kiểu nhập | Validate | Ghi chú |
|---|---|---|---|
| Phòng ban | TextInput | required | VD: Phòng Kỹ thuật |
| Chức vụ | TextInput | required | VD: Kỹ sư phần mềm |
| Loại hình làm việc | Dropdown | required | FULL_TIME / PART_TIME |
| Ghi chú | TextInput (multiline) | optional | |

**API call:** `POST /api/v1/employee`
```json
{
  "department": "Phòng Kỹ thuật",
  "position": "Kỹ sư phần mềm",
  "status": "ACTIVE",
  "workingType": "FULL_TIME",
  "isActive": true,
  "publicKeyJwk": "{ \"kty\":\"EC\", \"crv\":\"P-256\", ... }",
  "createdAt": "2026-04-19T08:00:00",
  "updatedAt": "2026-04-19T08:00:00",
  "createdBy": "nv@company.com",
  "note": null
}
```

---

#### Màn hình Đăng nhập (Sign In)

| Field | Kiểu nhập | Validate | Ghi chú |
|---|---|---|---|
| Email / Số điện thoại | TextInput | required | Hỗ trợ cả 2 |
| Mật khẩu | TextInput (obscured) | required | |

**API call:** `POST /api/v1/auth/sign-in`
```json
{ "username": "nv@company.com", "password": "..." }
```

**Xử lý lỗi:**

| HTTP Code | Message backend | Hiển thị trên app |
|---|---|---|
| 404 | User not found | Đăng nhập thất bại. Kiểm tra lại thông tin. |
| 401 | Password invalid | Đăng nhập thất bại. Kiểm tra lại thông tin. |
| 403 | Account is pending approval | Tài khoản đang chờ Admin duyệt. |
| 403 | Account has been rejected | Tài khoản đã bị từ chối. Vui lòng liên hệ Admin. |

---

#### Màn hình Tạo đơn từ (Create Request)

| Field | Kiểu nhập | Validate | Ghi chú |
|---|---|---|---|
| Loại đơn | Dropdown | required | LEAVE / WFH / BUSINESS_TRIP / ATTENDANCE_CORRECTION |
| Ngày bắt đầu | DatePicker | required, ≥ hôm nay | |
| Ngày kết thúc | DatePicker | required, ≥ ngày bắt đầu | |
| Buổi | Dropdown | required | FULL_DAY / MORNING / AFTERNOON |
| Lý do | TextInput (multiline) | required, min 10 ký tự | |
| Ảnh đính kèm | ImagePicker | optional | |

**API call:** `POST /api/v1/requests`
```json
{
  "requestType": "LEAVE",
  "startDate": "2026-04-20",
  "endDate": "2026-04-21",
  "session": "FULL_DAY",
  "reason": "Nghỉ phép theo kế hoạch",
  "photoUrl": null
}
```

---

#### Màn hình Chấm công (Attendance)

| Action | Input | API |
|---|---|---|
| Check-in | Tự động lấy vị trí GPS | `POST /api/v1/attendance/check-in` |
| Check-out | Tự động lấy vị trí GPS | `POST /api/v1/attendance/check-out` |

**Request body check-in:**
```json
{ "location": "21.028511, 105.834160", "note": null }
```

---

#### Màn hình Duyệt đơn (Manager)

| Action | Input | API |
|---|---|---|
| Duyệt đơn | Không cần input thêm | `PUT /api/v1/manager/requests/{id}/approve` |
| Từ chối đơn | Textarea "Lý do từ chối" (required) | `PUT /api/v1/manager/requests/{id}/reject` |

---

#### Màn hình Duyệt tài khoản (Admin — Pending Accounts)

| Action | Input | API |
|---|---|---|
| Duyệt tài khoản | Không cần input | `PUT /api/v1/admin/accounts/{id}/approve` |
| Từ chối tài khoản | Confirm dialog | `PUT /api/v1/admin/accounts/{id}/reject` |

**Hiệu ứng phụ khi duyệt:**
1. `accountStatus` → ACTIVE
2. `@Async`: `RegisterDID` lên Fabric (nếu publicKey có sẵn)
3. Issue `EmploymentVC`, lưu vào `employee.employmentVc`

---

#### Màn hình Quản lý nhân viên (Chief)

| Action | Input | API | Hiệu ứng phụ |
|---|---|---|---|
| Tạo nhân viên | email, phone, password, dept, pos | `POST /api/v1/chief/employees` | Ghi Fabric REQUEST |
| Đổi role/vị trí | role, position | `PUT /api/v1/chief/employees/{id}/role` | Issue PromotionVC |
| Terminate | reason | `PUT /api/v1/chief/employees/{id}/terminate` | RevokeDID + TerminationVC |

---

### 3.3 Phân quyền màn hình

| Màn hình | EMPLOYEE | MANAGER | CHIEF | ADMIN |
|---|:---:|:---:|:---:|:---:|
| Home | ✓ | ✓ | ✓ | ✓ |
| Profile | ✓ | ✓ | ✓ | ✓ |
| Contract | ✓ | ✓ | ✓ | ✓ |
| Payroll | ✓ | ✓ | ✓ | ✓ |
| Attendance | ✓ | ✓ | ✓ | ✓ |
| Requests | ✓ | ✓ | ✓ | ✓ |
| Directory | ✓ | ✓ | ✓ | ✓ |
| Company | ✓ | ✓ | ✓ | ✓ |
| Manager Requests | ✗ | ✓ | ✓ | ✓ |
| Chief Panel | ✗ | ✗ | ✓ | ✓ |
| Ledger (audit trail) | ✗ | ✗ | ✓ | ✓ |
| Admin Dashboard | ✗ | ✗ | ✗ | ✓ |
| Pending Accounts | ✗ | ✗ | ✗ | ✓ |
| Issue SalaryRangeVC | ✗ | ✗ | ✗ | ✓ |

---

### 3.4 Cấu trúc thư mục dự án

```
identity-fabric/
├── fabric-network/
│   ├── chaincode/
│   │   └── asset-transfer/
│   │       └── src/main/java/org/hyperledger/fabric/samples/
│   │           ├── IdentityLedger.java       ← Smart contract (audit + DID)
│   │           ├── IdentityRecord.java       ← Data model cho audit record
│   │           └── DIDDocument.java          ← Data model cho DID Document
│   ├── docker-compose.yaml                   ← 8 services
│   ├── network/configtx/configtx.yaml        ← Channel config
│   └── scripts/network.sh                    ← Setup script
│
├── fabric-spring-backend/
│   └── src/main/kotlin/
│       ├── org/fabric/api/
│       │   ├── config/FabricGatewayConfig.kt ← Kết nối Fabric Gateway
│       │   ├── model/IdentityModels.kt        ← Request models cho chaincode
│       │   └── service/IdentityLedgerService.kt ← Gọi chaincode functions
│       │
│       └── com/mpcorp/identity/
│           ├── application/
│           │   ├── dto/                       ← Command objects
│           │   └── usecase/                   ← Business logic
│           │       ├── auth/                  ← SignIn, SignUp
│           │       ├── employee/              ← CRUD employee
│           │       ├── profile/               ← CRUD profile
│           │       ├── contract/              ← CRUD contract
│           │       ├── payroll/               ← CRUD payroll
│           │       ├── attendance/            ← CheckIn, CheckOut
│           │       ├── request/               ← Create, Approve, Reject
│           │       └── company/               ← CRUD company
│           ├── domain/
│           │   ├── entity/                    ← Domain entities
│           │   └── repository/                ← Repository interfaces
│           ├── infrastructure/
│           │   ├── config/SecurityConfig.kt   ← Spring Security + RBAC
│           │   ├── fabric/
│           │   │   ├── FabricLedgerBridge.kt  ← Ghi ledger (@Async)
│           │   │   ├── FabricOutboxService.kt ← Retry queue
│           │   │   └── FabricRetryScheduler.kt← @Scheduled retry
│           │   ├── vc/
│           │   │   ├── VcIssuerService.kt     ← Phát hành 4 loại VC
│           │   │   ├── VpService.kt           ← OID4VP flow
│           │   │   └── VpSessionStore.kt      ← Lưu session VP request
│           │   └── persistence/
│           │       ├── jpa_entity/            ← JPA entities
│           │       ├── jpa_repository/        ← Spring Data repos
│           │       ├── mapper/                ← Domain ↔ JPA mappers
│           │       └── repository/            ← Repository impls
│           └── presentation/
│               ├── controller/
│               │   ├── AdminController.kt     ← /admin/* (ADMIN, CHIEF)
│               │   ├── ChiefController.kt     ← /chief/* (CHIEF, ADMIN)
│               │   ├── EmployeeController.kt  ← /employee/*
│               │   └── ...
│               ├── request/                   ← Request DTOs
│               └── response/                  ← Response DTOs
│
└── identity_frontend/
    └── lib/
        ├── core/
        │   ├── di/injection.dart              ← GetIt DI setup
        │   ├── network/api_client.dart         ← Dio + interceptors
        │   ├── network/api_constants.dart      ← All API endpoints
        │   ├── routes/app_router.dart          ← GoRouter + guards
        │   ├── storage/secure_storage.dart     ← JWT + privateKey storage
        │   └── themes/app_colors.dart          ← Design tokens
        ├── data/
        │   ├── datasources/remote/            ← HTTP calls
        │   ├── models/                         ← JSON models
        │   └── repositories/                   ← Repository impls
        ├── domain/
        │   ├── entities/                       ← Domain entities
        │   ├── repositories/                   ← Interfaces
        │   └── usecases/                       ← Use case wrappers
        └── presentation/
            ├── features/
            │   ├── auth/                       ← SignIn, SignUp + BLoC
            │   ├── onboarding/                 ← Onboarding + BLoC (sinh keypair)
            │   ├── home/                       ← HomeScreen
            │   ├── profile/                    ← ProfileScreen + BLoC
            │   ├── contract/                   ← ContractScreen + BLoC
            │   ├── payroll/                    ← PayrollScreen + BLoC
            │   ├── attendance/                 ← Attendance + BLoC
            │   ├── requests/                   ← Requests + BLoC
            │   ├── directory/                  ← DirectoryScreen + Cubit
            │   ├── company/                    ← CompanyScreen + Cubit
            │   ├── ledger/                     ← LedgerScreen + BLoC
            │   ├── admin/                      ← Admin + Pending Accounts
            │   ├── chief/                      ← ChiefScreen
            │   └── manager/                    ← ManagerRequestsScreen
            └── widgets/                        ← AppInput, AppCard, ...
```

---

*Tài liệu được tổng hợp từ source code thực tế của dự án TrustID Identity Fabric v2.0.*
