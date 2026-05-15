# Cơ chế Đọc và Ghi Data vào Ledger — Luồng hàm thực tế

---

## Sơ đồ tổng quan các layer

```
Flutter App
    │
    ▼
com.mpcorp.identity          ← Spring Boot Business Logic (MySQL + Blockchain)
    │  presentation/controller/
    │  application/usecase/
    │  infrastructures/fabric/FabricLedgerBridge.kt
    │
    ▼
org.fabric.api               ← Fabric Gateway Layer (Kotlin)
    │  service/IdentityLedgerService.kt
    │  config/FabricGatewayConfig.kt
    │
    ▼
fabric-network/application   ← Fabric Java Gateway SDK (gRPC)
    │  App.java (demo client — cùng pattern)
    │
    ▼
fabric-network/chaincode     ← Chaincode trên Peer (Java)
    │  IdentityLedger.java
    │
    ▼
Hyperledger Fabric Ledger    ← World State + Blockchain Log
```

---

## Phần 1: fabric-network — Application và Chaincode làm gì?

### 1.1 `fabric-network/application/App.java` — Demo Gateway Client

File này KHÔNG phải production code. Nó là demo/test client minh họa cách kết nối đến Fabric network bằng Java Gateway SDK.

**Chức năng chính: thiết lập kết nối đến Peer**

```
App.main()
│
├── newGrpcConnection()
│       ManagedChannel channel = NettyChannelBuilder
│           .forTarget("localhost:7051")            // peer endpoint
│           .sslContext(TLS cert)
│           .build();
│
├── Gateway.newInstance()
│       .identity(newIdentity())                   // X.509 cert từ crypto-config/
│       .signer(newSigner())                       // private key
│       .connection(grpcChannel)
│       .connect()
│
├── gateway.getNetwork("mychannel")                // chọn channel
│
├── network.getContract("asset-transfer")          // chọn chaincode
│
├── contract.submitTransaction("CreateAsset", ...) // WRITE → qua Orderer
│
└── contract.evaluateTransaction("GetAllAssets")   // READ → chỉ hỏi Peer
```

> **Pattern này** được tái dùng hoàn toàn trong `FabricGatewayConfig.kt` ở production.

---

### 1.2 `fabric-network/chaincode/IdentityLedger.java` — Smart Contract

Chaincode chạy **bên trong Peer**, không phải bên trong Spring Boot.
Mọi hàm đều nhận `Context ctx` — wrapper của `ChaincodeStub stub`.

**Hàm GHI vào Ledger**

```
UpsertRecord(ctx, employeeId, recordType, status, keyFields, dataHash, action, timestamp, updatedBy)
│
├── key = recordType.toLowerCase() + ":" + employeeId
│       ví dụ: "profile:123", "contract:123"
│
├── Tạo IdentityRecord object
│
├── stub.putStringState(key, genson.serialize(record))
│       ↑ GHI vào World State — đây là lệnh ghi thực sự
│
├── stub.setEvent("IdentityRecordUpserted", genson.serialize(record).getBytes())
│       ↑ phát sự kiện cho listener bên ngoài
│
└── return record (trả về bytes cho caller)
```

```
RegisterDID(ctx, did, employeeId, publicKeyJwk, controller)
│
├── key = "did:" + did
│       ví dụ: "did:did:fabric:trustid:123"
│
├── Tạo DIDDocument object (status=ACTIVE, createdAt=now)
│
└── stub.putStringState(key, genson.serialize(didDoc))
```

```
DeleteRecord(ctx, employeeId, recordType, updatedBy)
│
├── Đọc record hiện tại: stub.getStringState(key)
├── record.status = "DELETED"
│       ↑ KHÔNG xóa thực sự — soft delete để giữ audit trail
└── stub.putStringState(key, genson.serialize(record))
```

**Hàm ĐỌC từ Ledger**

```
GetRecord(ctx, recordType, employeeId)
│
├── key = recordType.toLowerCase() + ":" + employeeId
├── String json = stub.getStringState(key)
│       ↑ ĐỌC từ World State
└── return genson.deserialize(json, IdentityRecord.class)
```

```
GetRecordHistory(ctx, recordType, employeeId)
│
├── key = recordType.toLowerCase() + ":" + employeeId
├── QueryResultsIterator<KeyModification> history = stub.getHistoryForKey(key)
│       ↑ ĐỌC từ Blockchain log — toàn bộ lịch sử thay đổi
├── Duyệt iterator → collect từng modification:
│       modification.getTxId()
│       modification.getValue()    // JSON bytes tại thời điểm đó
│       modification.getTimestamp()
│       modification.isDeleted()
└── return List<IdentityRecord>
```

```
GetAllRecordsByEmployee(ctx, employeeId)
│
├── stub.getStateByRange("contract:" + employeeId, ...)
├── stub.getStateByRange("payroll:" + employeeId, ...)
├── stub.getStateByRange("profile:" + employeeId, ...)
│       ↑ range query trên World State theo prefix key
└── return tổng hợp 3 loại record
```

```
VerifyRecord(ctx, recordType, employeeId, hashToVerify)
│
├── record = GetRecord(...)           // đọc từ ledger
├── storedHash = record.getDataHash()
├── isValid = storedHash.equals(hashToVerify)
└── return VerifyRecordResponse { valid, reason, storedHash, providedHash }
```

---

## Phần 2: org.fabric.api — Fabric Gateway Layer

Package này là **cầu nối** giữa Spring Boot và Fabric network.

### 2.1 `FabricGatewayConfig.kt` — Khởi tạo kết nối

```
@Configuration FabricGatewayConfig (chạy 1 lần lúc startup)
│
├── @Bean grpcChannel()
│       NettyChannelBuilder.forTarget(props.peer.endpoint)   // "localhost:7051"
│           .sslContext(TLS cert của peer)
│           .build()
│       → ManagedChannel (kết nối gRPC đến Peer)
│
└── @Bean fabricGateway(grpcChannel)
        certificate = Identities.readX509Certificate(props.gateway.certPath)
        privateKey  = readPrivateKey(props.gateway.keyPath)
        identity    = X509Identity(props.mspId, certificate)
        signer      = Signers.newPrivateKeySigner(privateKey)
        │
        Gateway.newInstance()
            .identity(identity)
            .signer(signer)
            .connection(grpcChannel)
            .evaluateOptions(deadline: 5s)
            .endorseOptions(deadline: 15s)
            .submitOptions(deadline: 5s)
            .commitStatusOptions(deadline: 60s)
            .connect()
        → Gateway (singleton bean, inject vào IdentityLedgerService)
```

---

### 2.2 `IdentityLedgerService.kt` — Gọi chaincode từ Spring

Bean này **inject Gateway** và gọi các hàm chaincode.

```kotlin
// Lazy init — chỉ lấy 1 lần
val network  by lazy { gateway.getNetwork(props.channelName) }    // "mychannel"
val contract by lazy { network.getContract(props.chaincodeName) } // "asset-transfer"
```

**Hàm GHI — dùng `submitTransaction`**

```
IdentityLedgerService.upsertRecord(request: UpsertIdentityRecordRequest)
│
├── val resultBytes = contract.submitTransaction(
│       "UpsertRecord",                         // tên hàm trong chaincode
│       request.employeeId,                     // "123"
│       request.recordType,                     // "PROFILE"
│       request.status,                         // "ACTIVE"
│       request.keyFields,                      // JSON string
│       request.dataHash,                       // SHA-256 hash
│       request.action,                         // "CREATE"
│       timestamp,                              // ISO8601
│       request.updatedBy,                      // "system"
│   )
│   ↑ submitTransaction đi qua:
│     1. Endorsing Peers ký (simulate + RWSet)
│     2. Orderer đóng gói block
│     3. Committing Peers validate + ghi World State
│
├── val record = objectMapper.readValue<IdentityRecord>(resultBytes)
├── eventPublisher.publish(FabricEvent(...))    // Spring event
└── return record
```

```
IdentityLedgerService.registerDID(request: RegisterDIDRequest)
│
└── contract.submitTransaction(
        "RegisterDID",
        request.did,           // "did:fabric:trustid:123"
        request.employeeId,
        request.publicKeyJwk,  // từ Flutter app
        request.controller,    // "did:fabric:trustid:org1"
    )
```

**Hàm ĐỌC — dùng `evaluateTransaction`**

```
IdentityLedgerService.getRecord(recordType, employeeId)
│
├── val result = contract.evaluateTransaction(
│       "GetRecord",
│       recordType,    // "PROFILE"
│       employeeId,    // "123"
│   )
│   ↑ evaluateTransaction CHỈ hỏi 1 Peer cục bộ
│     KHÔNG qua Orderer — nhanh hơn, không thay đổi state
│
└── return objectMapper.readValue<IdentityRecord>(result)
```

```
IdentityLedgerService.getRecordHistory(recordType, employeeId)
│
└── contract.evaluateTransaction("GetRecordHistory", recordType, employeeId)
    → List<IdentityRecord> (toàn bộ lịch sử từ blockchain log)
```

```
IdentityLedgerService.verifyRecord(recordType, employeeId, hashToVerify)
│
└── contract.evaluateTransaction("VerifyRecord", recordType, employeeId, hashToVerify)
    → VerifyRecordResponse { valid: Boolean, reason, storedHash, providedHash }
```

**REST Endpoints trong `IdentityLedgerController.kt`**

```
POST   /api/v1/ledger/records                            → upsertRecord()
DELETE /api/v1/ledger/records/{employeeId}/{recordType}  → deleteRecord()
GET    /api/v1/ledger/records/{employeeId}/{recordType}  → getRecord()
GET    /api/v1/ledger/records/{employeeId}/{recordType}/history  → getRecordHistory()
GET    /api/v1/ledger/records/{employeeId}/{recordType}/verify?hash=...  → verifyRecord()
GET    /api/v1/ledger/did/{did}                          → resolveDID()
```

---

## Phần 3: com.mpcorp.identity — Business Logic kết nối với org.fabric.api như thế nào?

### 3.1 Kiến trúc Clean Architecture

```
presentation/controller/EmployeeController.kt
    │  @PostMapping("/api/v1/employee")
    ▼
application/usecase/employee/CreateCurrentEmployeeUseCase.kt
    │  gọi repository + trigger blockchain
    ▼
infrastructures/
    ├── persistence/repository/EmployeeRepositoryImpl.kt   → MySQL (JPA)
    └── fabric/FabricLedgerBridge.kt                       → Blockchain (async)
            │
            ▼
        org.fabric.api.service.IdentityLedgerService.kt   ← inject vào đây
```

### 3.2 Luồng GHI đầy đủ — Tạo Employee Profile

```
POST /api/v1/employee  (Flutter gọi)
│
▼
EmployeeController.create(request)
│
▼
CreateCurrentEmployeeUseCase.execute(request)
│
├── 1. Lưu MySQL (source of truth)
│       employeeJpaRepository.save(EmployeeJpaEntity)    // commit
│       profileJpaRepository.save(ProfileJpaEntity)      // commit
│
└── 2. Trigger blockchain async (fire-and-forget)
        FabricLedgerBridge.upsertProfileRecord(profile, action="CREATE")
        │  @Async → chạy trên thread riêng, không block HTTP response
        │
        ├── keyFields = { name, email, educationLevel }   // loại bỏ PII
        ├── dataHash = sha256(json toàn bộ profile)       // hash để verify
        │
        ├── TRY:
        │   IdentityLedgerService.upsertRecord(
        │       UpsertIdentityRecordRequest(
        │           employeeId = "123",
        │           recordType = "PROFILE",
        │           status = "ACTIVE",
        │           keyFields = "{name:'Minh', email:'...'}",
        │           dataHash = "a3f1b9c2...",
        │           action = "CREATE",
        │           updatedBy = "system"
        │       )
        │   )
        │   → contract.submitTransaction("UpsertRecord", ...)
        │   → Chaincode stub.putStringState("profile:123", json)
        │   → Ledger ghi block
        │
        └── CATCH (network/endorsement failure):
            FabricOutboxService.enqueue(event)
            → Lưu vào bảng fabric_outbox_events (MySQL)
            → Status = PENDING, nextRetryAt = now + 30s
```

### 3.3 Retry với Outbox Pattern (`FabricOutboxService.kt`)

```
FabricRetryScheduler (@Scheduled mỗi 5 phút)
│
└── FabricOutboxService.processDueEvents()
        │
        ├── Query: SELECT * FROM fabric_outbox_events
        │          WHERE status IN ('PENDING','RETRYING')
        │          AND nextRetryAt <= now
        │
        └── forEach event:
                event.retryCount++
                │
                ├── TRY: gọi lại IdentityLedgerService (cùng logic trên)
                │         event.status = COMPLETED
                │
                └── CATCH:
                        if retryCount >= 5:
                            event.status = DEAD_LETTER   // cần can thiệp thủ công
                        else:
                            delaySeconds = min(30 * 2^retryCount, 3600)
                            // lần 1: 60s, lần 2: 120s, lần 3: 240s, lần 4: 480s
                            event.nextRetryAt = now + delaySeconds
                            event.status = RETRYING
```

### 3.4 Luồng GHI DID — Khi Admin phê duyệt tài khoản

```
AdminController.approveEmployee(employeeId)
│
▼
ApproveEmployeeUseCase.execute(employeeId)
│
├── Cập nhật status = APPROVED trong MySQL
│
└── FabricLedgerBridge.registerDID(employeeId, publicKeyJwk, approvedBy)
        │  @Async
        │
        └── IdentityLedgerService.registerDID(
                RegisterDIDRequest(
                    did = "did:fabric:trustid:123",
                    employeeId = "123",
                    publicKeyJwk = "{ kty:'EC', crv:'P-256', x:..., y:... }",
                    controller = "did:fabric:trustid:org1"
                )
            )
            → contract.submitTransaction("RegisterDID", ...)
            → Chaincode stub.putStringState("did:did:fabric:trustid:123", json)
```

### 3.5 Luồng ĐỌC đầy đủ

```
GET /api/v1/ledger/records/123/PROFILE  (Flutter gọi)
│
▼
IdentityLedgerController.getRecord("123", "PROFILE")
│
▼
IdentityLedgerService.getRecord("PROFILE", "123")
│
└── contract.evaluateTransaction("GetRecord", "PROFILE", "123")
        │  KHÔNG qua Orderer
        │  Peer thực thi chaincode cục bộ
        │
        └── Chaincode GetRecord():
                key = "profile:123"
                json = stub.getStringState("profile:123")
                return IdentityRecord (JSON bytes)
        │
        └── objectMapper.readValue<IdentityRecord>(bytes)
            → HTTP 200 { recordId, employeeId, recordType, keyFields, dataHash, ... }
```

---

## Tóm tắt: submitTransaction vs evaluateTransaction

| | `submitTransaction` | `evaluateTransaction` |
|---|---|---|
| Dùng cho | WRITE (UpsertRecord, RegisterDID, DeleteRecord) | READ (GetRecord, GetHistory, VerifyRecord) |
| Luồng | Endorser → Orderer → Committer | Chỉ 1 Peer cục bộ |
| Thay đổi ledger | Có | Không |
| Tốc độ | Chậm (~2-5 giây) | Nhanh (~ms) |
| Trong code | `IdentityLedgerService.upsertRecord()` | `IdentityLedgerService.getRecord()` |
| Trong chaincode | `stub.putStringState()` | `stub.getStringState()` |

---

## Tóm tắt: Vai trò từng package

| Package | Vai trò |
|---|---|
| `fabric-network/chaincode` | Smart contract chạy trên Peer — ghi/đọc World State bằng `stub.putStringState` / `stub.getStringState` |
| `fabric-network/application` | Demo client — minh họa pattern kết nối Gateway SDK |
| `org.fabric.api` | Fabric Gateway Layer — khởi tạo kết nối, expose `contract.submitTransaction` / `contract.evaluateTransaction` như Spring service |
| `com.mpcorp.identity` | Business logic — lưu MySQL trước, sau đó async ghi blockchain qua `FabricLedgerBridge` với retry bằng Outbox pattern |
