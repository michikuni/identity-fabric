# Blockchain — Hyperledger Fabric trong Identity Fabric

## Mục lục
1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [fabric-network — Hạ tầng mạng](#2-fabric-network--hạ-tầng-mạng)
   - [Cấu trúc thư mục](#21-cấu-trúc-thư-mục)
   - [Docker Compose — các node trong mạng](#22-docker-compose--các-node-trong-mạng)
   - [configtx.yaml — cấu hình channel](#23-configtxYaml--cấu-hình-channel)
   - [network.sh — vòng đời mạng](#24-networksh--vòng-đời-mạng)
3. [Chaincode — Smart Contract](#3-chaincode--smart-contract)
   - [IdentityRecord — mô hình dữ liệu trên ledger](#31-identityrecord--mô-hình-dữ-liệu-trên-ledger)
   - [DIDDocument — tài liệu DID](#32-diddocument--tài-liệu-did)
   - [IdentityLedger — hàm smart contract](#33-identityledger--hàm-smart-contract)
4. [fabric-spring-backend — Kết nối Backend → Fabric](#4-fabric-spring-backend--kết-nối-backend--fabric)
   - [FabricProperties — cấu hình](#41-fabricproperties--cấu-hình)
   - [FabricGatewayConfig — kết nối gRPC](#42-fabricgatewayconfig--kết-nối-grpc)
   - [IdentityLedgerBridge — điểm giao tiếp chính](#43-identityledgerbridge--điểm-giao-tiếp-chính)
   - [FabricOutboxService — outbox pattern](#44-fabricoutboxservice--outbox-pattern)
   - [FabricRetryScheduler — retry tự động](#45-fabricretryscheduler--retry-tự-động)
5. [Luồng dữ liệu end-to-end](#5-luồng-dữ-liệu-end-to-end)
6. [Chiến lược độ tin cậy](#6-chiến-lược-độ-tin-cậy)

---

## 1. Tổng quan kiến trúc

Dự án dùng **Hyperledger Fabric 2.5** — một blockchain *private/permissioned*, nghĩa là chỉ các node được cấp phép mới tham gia mạng. Không phải public blockchain như Ethereum.

```
┌─────────────────────────────────────────────────────────┐
│                   Flutter Mobile App                    │
└─────────────────────────┬───────────────────────────────┘
                          │ REST API
┌─────────────────────────▼───────────────────────────────┐
│              Spring Boot Backend (fabric-spring-backend) │
│                                                          │
│  UseCase → IdentityLedgerBridge (@Async)                 │
│                    │                                     │
│          ┌─────────▼──────────┐                          │
│          │  FabricGateway     │  ←── TLS gRPC            │
│          │  (Fabric SDK)      │                          │
│          └─────────┬──────────┘                          │
│                    │  on failure                         │
│          ┌─────────▼──────────┐                          │
│          │  FabricOutbox      │  ←── MySQL retry queue   │
│          │  + RetryScheduler  │                          │
│          └────────────────────┘                          │
└──────────────────────────────────────────────────────────┘
                          │ gRPC :7051
┌─────────────────────────▼───────────────────────────────┐
│              Hyperledger Fabric Network                  │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ peer0    │  │ peer1    │  │ orderer  │               │
│  │ Org1     │  │ Org2     │  │ etcdraft │               │
│  └────┬─────┘  └──────────┘  └──────────┘               │
│       │                                                  │
│  ┌────▼──────────────────────────────────┐               │
│  │  Chaincode: identity-ledger           │               │
│  │  (Java — IdentityLedger.java)         │               │
│  └───────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────┘
```

**Triết lý thiết kế:** MySQL là nguồn dữ liệu chính (source of truth cho app), Fabric là lớp **audit trail bất biến** — mọi thay đổi quan trọng đều được ghi lên blockchain dưới dạng hash + metadata. Không lưu dữ liệu nhạy cảm (lương, CMND) lên chain.

---

## 2. fabric-network — Hạ tầng mạng

### 2.1 Cấu trúc thư mục

```
fabric-network/
├── docker-compose.yaml              # Định nghĩa tất cả container Fabric
├── scripts/
│   └── network.sh                   # Script điều khiển vòng đời mạng
├── network/
│   ├── configtx/
│   │   └── configtx.yaml            # Cấu hình channel, policy, tổ chức
│   └── crypto-config/
│       └── crypto-config.yaml       # Định nghĩa tổ chức để sinh cert
├── chaincode/
│   └── asset-transfer/              # Java chaincode (Maven project)
│       ├── pom.xml
│       └── src/main/java/org/hyperledger/fabric/samples/
│           ├── IdentityLedger.java  # Smart contract chính
│           ├── IdentityRecord.java  # Model bản ghi trên ledger
│           └── DIDDocument.java     # Model tài liệu DID
└── application/                     # Demo client Java (Fabric Gateway SDK)
    └── src/main/java/.../App.java
```

### 2.2 Docker Compose — các node trong mạng

File `docker-compose.yaml` định nghĩa **8 container** tạo thành mạng Fabric:

| Container | Image | Vai trò |
|---|---|---|
| `ca_org1` | fabric-ca:1.5.7 | Certificate Authority cho Org1 — cấp và quản lý chứng chỉ X.509 |
| `ca_org2` | fabric-ca:1.5.7 | Certificate Authority cho Org2 |
| `orderer.example.com` | fabric-orderer:2.5.4 | Sắp xếp và đóng gói transaction vào block (consensus etcdraft) |
| `peer0.org1.example.com` | fabric-peer:2.5.4 | Peer chính Org1 — lưu ledger, thực thi chaincode |
| `peer1.org1.example.com` | fabric-peer:2.5.4 | Peer dự phòng Org1 |
| `peer0.org2.example.com` | fabric-peer:2.5.4 | Peer chính Org2 |
| `peer1.org2.example.com` | fabric-peer:2.5.4 | Peer dự phòng Org2 |
| `cli` | fabric-tools:2.5.4 | CLI container để chạy lệnh peer admin |

Tất cả container chạy trong network Docker `fabric_network`. Dữ liệu ledger được persist qua named volumes.

**Cơ chế đồng thuận:** etcdraft (Raft) — orderer chạy leader election, đảm bảo fault tolerance nếu một số orderer node lỗi.

### 2.3 configtx.yaml — cấu hình channel

Định nghĩa 3 profile chính:

```
TwoOrgsOrdererGenesis   → Genesis block cho orderer system channel
TwoOrgsApplicationGenesis → Genesis block cho app channel
TwoOrgsChannel          → Cấu hình join channel
```

**Tổ chức tham gia:**
- `OrdererOrg` — quản lý orderer
- `Org1MSP` — anchor peer: `peer0.org1.example.com:7051`
- `Org2MSP` — anchor peer: `peer0.org2.example.com:9051`

**Capabilities:** Channel V2_0, Application V2_5 — đảm bảo tất cả peer chạy cùng phiên bản logic.

### 2.4 network.sh — vòng đời mạng

Script bash điều khiển toàn bộ vòng đời. Các lệnh chính:

```bash
# Khởi động mạng + tạo channel + deploy chaincode
./network.sh up
./network.sh createChannel
./network.sh deployCC

# Tắt mạng, xóa artifacts
./network.sh down
```

**Các bước `deployCC` thực hiện:**
1. `peer lifecycle chaincode package` — đóng gói chaincode Java thành `.tar.gz`
2. `peer lifecycle chaincode install` — cài lên từng peer
3. `peer lifecycle chaincode approveformyorg` — Org1 và Org2 approve
4. `peer lifecycle chaincode commit` — commit chaincode definition lên channel

Chaincode được deploy với tên `identity-ledger` trên channel `mychannel`.

---

## 3. Chaincode — Smart Contract

Chaincode viết bằng **Java**, chạy trong container riêng trên mỗi peer, implement interface `ContractInterface` của Hyperledger Fabric.

### 3.1 IdentityRecord — mô hình dữ liệu trên ledger

```java
public class IdentityRecord {
    String recordId;      // "PROFILE:emp-123"
    String employeeId;    // ID nội bộ
    String recordType;    // PROFILE | CONTRACT | PAYROLL | ATTENDANCE | REQUEST | COMPANY
    String status;        // ACTIVE | REVOKED | DELETED
    String keyFields;     // JSON tóm tắt — trường không nhạy cảm (tên, phòng ban...)
    String dataHash;      // SHA-256 của toàn bộ dữ liệu off-chain
    String action;        // CREATE | UPDATE | DELETE
    String timestamp;     // ISO-8601 UTC
    String updatedBy;     // employeeId của người thực hiện
}
```

**Thiết kế quan trọng:** Fabric chỉ lưu `keyFields` (tóm tắt) và `dataHash` (hash SHA-256), **không** lưu dữ liệu đầy đủ. Dữ liệu thực tế nằm trong MySQL. Mục đích: bảo mật thông tin nhạy cảm, đồng thời vẫn có thể xác minh tính toàn vẹn qua hash.

**Key format trên ledger:** `"{recordType}:{employeeId}"` — ví dụ `"PROFILE:42"`, `"CONTRACT:42"`.

### 3.2 DIDDocument — tài liệu DID

```java
public class DIDDocument {
    String did;            // "did:fabric:trustid:<employeeCode>"
    String employeeId;
    String publicKeyJwk;   // ECDSA P-256, định dạng JWK
    String controller;     // DID của tổ chức phát hành
    String status;         // ACTIVE | REVOKED
    String createdAt;
    String updatedAt;
    String revokedAt;      // null nếu còn active
    String revokedBy;
    String revokeReason;
}
```

DID theo chuẩn W3C DID Specification. Khi employee được admin approve → `RegisterDID`. Khi bị terminate → `RevokeDID`. Toàn bộ lịch sử được giữ nguyên trên ledger (không xóa, chỉ thêm).

### 3.3 IdentityLedger — hàm smart contract

#### Nhóm Identity Record

| Hàm | Tham số | Mô tả |
|---|---|---|
| `InitLedger` | — | No-op, khởi tạo chaincode |
| `UpsertRecord` | employeeId, recordType, status, keyFields, dataHash, action, updatedBy | Tạo mới hoặc cập nhật bản ghi. Emit event `IdentityRecordUpserted` |
| `DeleteRecord` | employeeId, recordType, updatedBy | Soft-delete: đổi status=DELETED, action=DELETE. Emit `IdentityRecordDeleted` |
| `GetRecord` | recordType, employeeId | Lấy trạng thái hiện tại |
| `GetRecordHistory` | recordType, employeeId | Lấy toàn bộ lịch sử thay đổi (immutable history) |
| `GetAllRecords` | — | Lấy tất cả bản ghi (dùng GetStateByRange) |
| `GetAllRecordsByEmployee` | employeeId | Lấy tất cả record của một employee |
| `VerifyRecord` | recordType, employeeId, hashToVerify | So sánh hash được cung cấp với hash trên chain → trả về true/false |
| `RecordExists` | recordType, employeeId | Kiểm tra tồn tại |

#### Nhóm DID

| Hàm | Tham số | Mô tả |
|---|---|---|
| `RegisterDID` | did, employeeId, publicKeyJwk, controller | Đăng ký DID mới. Emit `DIDRegistered` |
| `RevokeDID` | did, revokedBy, revokeReason | Thu hồi DID. Emit `DIDRevoked` |
| `ResolveDID` | did | Trả về DIDDocument hiện tại |

**Fabric Events:** Mỗi transaction thành công emit một event (tên + payload JSON). Client có thể subscribe để nhận notification real-time.

---

## 4. fabric-spring-backend — Kết nối Backend → Fabric

Toàn bộ code kết nối nằm trong package `infrastructures/fabric/`.

### 4.1 FabricProperties — cấu hình

```kotlin
@ConfigurationProperties(prefix = "fabric")
data class FabricProperties(
    val mspId: String,           // "Org1MSP"
    val channelName: String,     // "mychannel"
    val chaincodeName: String,   // "identity-ledger"
    val peer: PeerProperties,
    val gateway: GatewayProperties,
)

data class PeerProperties(
    val endpoint: String,        // "localhost:7051"
    val tlsCertPath: String,     // đường dẫn TLS CA cert của peer
)

data class GatewayProperties(
    val certPath: String,        // X.509 cert của User1@org1
    val keyPath: String,         // thư mục chứa private key
)
```

Trong `application.yml`:

```yaml
fabric:
  msp-id: Org1MSP
  channel-name: mychannel
  chaincode-name: identity-ledger
  peer:
    endpoint: localhost:7051
    tls-cert-path: .../peer0.org1.example.com/tls/ca.crt
  gateway:
    cert-path: .../User1@org1.example.com/msp/signcerts/cert.pem
    key-path:  .../User1@org1.example.com/msp/keystore/
```

### 4.2 FabricGatewayConfig — kết nối gRPC

Class này tạo kết nối đến Fabric network qua gRPC có TLS:

```kotlin
// Bước 1: Tạo gRPC channel đến peer với TLS
fun grpcChannel(): ManagedChannel {
    val tlsCert = TlsChannelCredentials.newBuilder()
        .trustManager(File(props.peer.tlsCertPath))
        .build()
    return Grpc.newChannelBuilder(props.peer.endpoint, tlsCert).build()
}

// Bước 2: Tạo Fabric Gateway với identity + signing
fun fabricGateway(): Gateway {
    val cert = readCertificate(props.gateway.certPath)       // X.509 PEM
    val privateKey = readPrivateKey(props.gateway.keyPath)   // ECDSA private key

    return Gateway.newInstance()
        .identity(X509Identity(props.mspId, cert))
        .signer(Signers.newPrivateKeySigner(privateKey))
        .connection(grpcChannel())
        .evaluateOptions(CallOption.deadline(5, SECONDS))    // query
        .endorseOptions(CallOption.deadline(15, SECONDS))    // propose tx
        .submitOptions(CallOption.deadline(5, SECONDS))      // submit
        .commitStatusOptions(CallOption.deadline(60, SECONDS)) // wait commit
        .connect()
}
```

**Deadline:** Backend đặt timeout cho từng giai đoạn transaction — endorse (15s), submit (5s), commit status (60s). Đảm bảo app không bị treo vô thời hạn.

Private key được load từ thư mục keystore — tự động tìm file `_sk` hoặc `.pem`.

### 4.3 IdentityLedgerBridge — điểm giao tiếp chính

Đây là class trung tâm, được inject vào mọi UseCase cần ghi lên Fabric.

**Pattern hoạt động:**

```
1. UseCase gọi bridge method (ví dụ: upsertProfileRecord)
2. Bridge tính SHA-256 hash của toàn bộ entity
3. Bridge tạo keyFields JSON (chỉ trường không nhạy cảm)
4. Bridge gọi Fabric Gateway submit transaction (@Async — không block UseCase)
5. Nếu lỗi → enqueue vào FabricOutboxService
```

**@Async:** Tất cả method đều annotated `@Async`, chạy trên thread pool riêng. UseCase trả về response cho client ngay, không chờ Fabric xác nhận.

**Các method và loại record tương ứng:**

| Method | Chaincode function | Record type | Dữ liệu keyFields |
|---|---|---|---|
| `upsertProfileRecord` | UpsertRecord | PROFILE | name, department, position, role |
| `upsertContractRecord` | UpsertRecord | CONTRACT | typeContract, startDate, endDate |
| `upsertPayrollRecord` | UpsertRecord | PAYROLL | salaryType, currency (không ghi số tiền) |
| `deleteProfileRecord` | DeleteRecord | PROFILE | — |
| `logAttendance` | UpsertRecord | ATTENDANCE | checkIn, checkOut, workDate |
| `logRequest` | UpsertRecord | REQUEST | requestType, action, requestedBy |
| `logCompany` | UpsertRecord | COMPANY | companyName, action |
| `registerDID` | RegisterDID | — | did, publicKeyJwk, approvedBy |
| `revokeDID` | RevokeDID | — | did, revokedBy, reason |

**Ví dụ flow — khi Admin approve nhân viên:**

```
AdminController.approveAccount()
  → fabricBridge.registerDID(employeeId, publicKeyJwk, approvedBy)
       [chạy @Async trên thread riêng]
       → tính hash của DID document
       → Gateway.submit("RegisterDID", did, employeeId, publicKeyJwk, controller)
       → Fabric emits DIDRegistered event
       → Ledger lưu DIDDocument với status=ACTIVE
```

### 4.4 FabricOutboxService — outbox pattern

Giải quyết vấn đề: **nếu Fabric network tạm thời không khả dụng, dữ liệu không bị mất**.

```
┌──────────────┐   thất bại   ┌─────────────────────┐
│ LedgerBridge │ ──────────── │  fabric_outbox_events│
│  (gRPC call) │              │  (MySQL table)       │
└──────────────┘              └──────────┬────────────┘
                                         │ mỗi 30s
                              ┌──────────▼────────────┐
                              │  RetryScheduler        │
                              │  processDueEvents()    │
                              └───────────────────────┘
```

**Cơ chế retry với exponential backoff:**

| Lần retry | Delay chờ |
|---|---|
| 1 | 30 giây |
| 2 | 60 giây |
| 3 | 120 giây |
| 4 | 240 giây |
| 5 | 480 giây |
| > 5 | → DEAD_LETTER (cần xử lý thủ công) |

```
nextRetryAt = now + BASE_DELAY * 2^retryCount   (BASE_DELAY = 30s, max 3600s)
```

Mỗi event trong outbox lưu: loại event (UPSERT/DELETE/REGISTER_DID/REVOKE_DID), payload JSON đầy đủ, số lần đã retry, thời điểm retry tiếp theo, status (PENDING/DEAD_LETTER).

### 4.5 FabricRetryScheduler — retry tự động

```kotlin
@Scheduled(fixedDelay = 30_000)   // chạy mỗi 30 giây
fun retryPendingEvents() {
    try {
        outboxService.processDueEvents()
    } catch (e: Exception) {
        log.error("Retry cycle failed", e)
        // không throw — scheduler tiếp tục chạy
    }
}
```

Scheduler không throw exception để tránh Spring hủy scheduling task. Lỗi được log và bỏ qua, vòng lặp tiếp theo sẽ thử lại.

---

## 5. Luồng dữ liệu end-to-end

### Luồng 1 — Tạo employee mới (Chief)

```
1. Flutter gọi POST /api/v1/chief/employees
2. ChiefController.createEmployee()
3. Lưu AuthJpaEntity + EmployeeJpaEntity vào MySQL  ← response trả về ngay
4. ledgerBridge.logRequest(empId, "EMPLOYEE_CREATE", "CREATE", actor)
   [Thread riêng @Async]
   → UpsertRecord(empId, "REQUEST", "ACTIVE", keyFields, hash, "CREATE", actor)
   → Fabric block được tạo, emit IdentityRecordUpserted
```

### Luồng 2 — Admin approve account → DID được đăng ký

```
1. Flutter gọi PUT /api/v1/admin/accounts/{id}/approve
2. AdminController.approveAccount()
3. Cập nhật AccountStatus = ACTIVE trong MySQL
4. Nếu employee có publicKeyJwk:
   fabricBridge.registerDID(employeeId, publicKeyJwk, approvedBy)
   [Thread riêng @Async]
   → RegisterDID("did:fabric:trustid:empId", empId, publicKeyJwk, controller)
   → DIDDocument lưu trên ledger với status=ACTIVE
5. vcIssuerService.issueEmploymentVC(employee) — issue VC riêng
```

### Luồng 3 — Terminate employee → DID bị thu hồi

```
1. PUT /api/v1/chief/employees/{id}/terminate
2. isActive = false, status = TERMINATED → MySQL
3. ledgerBridge.revokeDID(empId, actor, reason)
   [Thread riêng @Async]
   → RevokeDID(did, revokedBy, revokeReason)
   → DIDDocument.status = REVOKED, revokedAt = now
4. vcIssuerService.issueTerminationVC(emp, actor, reason)
```

### Luồng 4 — Fabric tạm lỗi → outbox retry

```
1. LedgerBridge gọi Gateway.submit() → ConnectionException
2. catch → outboxService.enqueue(event)
   → INSERT fabric_outbox_events (status=PENDING, retryCount=0, nextRetryAt=now+30s)
3. Sau 30s: RetryScheduler.retryPendingEvents()
   → load events có nextRetryAt <= now
   → thử lại Gateway.submit()
   → thành công: DELETE event khỏi outbox
   → thất bại: retryCount++, nextRetryAt = now + 30 * 2^retryCount
4. Sau 5 lần thất bại: status = DEAD_LETTER
   → cần admin xử lý thủ công
```

---

## 6. Chiến lược độ tin cậy

### MySQL-first, Fabric-async

Ứng dụng **không block** vào Fabric. Mọi response trả về client dựa trên MySQL. Fabric là lớp bổ sung bất biến, không phải bottleneck.

| Ưu điểm | Hệ quả |
|---|---|
| Latency thấp cho client | Fabric có thể lag vài giây so với MySQL |
| App hoạt động khi Fabric down | Trong cửa sổ down, dữ liệu lưu ở outbox |
| Retry tự động | Dead-letter cần giám sát thủ công |

### Không lưu dữ liệu nhạy cảm lên chain

- **Lưu trên chain:** recordType, employeeId, timestamp, action, SHA-256 hash, keyFields tóm tắt
- **Không lưu:** số lương, số CMND, số bảo hiểm, mật khẩu, token

Để xác minh tính toàn vẹn: lấy dữ liệu từ MySQL → tính SHA-256 → so sánh với hash trên chain qua `VerifyRecord`.

### Immutable audit trail

Fabric ledger không cho phép xóa transaction. Dù UseCase gọi `DeleteRecord`, chaincode chỉ thêm một entry mới với `status=DELETED` — lịch sử đầy đủ vẫn truy vết được qua `GetRecordHistory`.
