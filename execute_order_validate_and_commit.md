# Ghi dữ liệu vào Hyperledger Fabric — Mô hình Execute · Order · Validate & Commit

> Tài liệu này bám theo code/config **thực tế** của repo `identity-fabric`.

## Lưu ý kiến trúc quan trọng (đọc trước)

Repo dùng **Fabric Gateway SDK** (Fabric 2.4+), **không** phải Legacy SDK. Điều này thay đổi *ai làm việc gì* ở bước 1–2:

> Với Gateway SDK, backend chỉ kết nối tới **MỘT peer duy nhất** (gọi là *gateway peer* — ở đây là `peer0.org1` qua `localhost:7051`). Chính peer đó mới đi thu thập endorsement từ các org khác, gửi orderer, và chờ commit hộ client. Trong Legacy SDK thì client tự làm hết. Code chỉ gọi `contract.submitTransaction(...)` — một dòng — và peer lo phần còn lại.

---

# BƯỚC 1 — EXECUTE (Đề xuất + Bảo chứng)

## 1.1. Đóng gói lời gọi chaincode như thế nào?

Điểm vào là [IdentityLedgerService.kt:88](fabric-spring-backend/src/main/kotlin/org/fabric/api/service/IdentityLedgerService.kt#L88):

```kotlin
contract.submitTransaction(
    "UpsertRecord",          // tên hàm chaincode cần gọi
    request.employeeId,      // arg[0]
    request.recordType,      // arg[1]
    request.status,          // ...
    request.keyFields,
    request.dataHash,
    request.action,
    timestamp,
    request.updatedBy,
)
```

`contract` lấy từ [IdentityLedgerService.kt:52-53](fabric-spring-backend/src/main/kotlin/org/fabric/api/service/IdentityLedgerService.kt#L52-L53):

```kotlin
private val network  by lazy { gateway.getNetwork(props.channelName) }      // "mychannel"
private val contract by lazy { network.getContract(props.chaincodeName) }   // "identity-ledger"
```

**SDK biến lời gọi này thành một `Proposal` protobuf.** Bên trong Proposal có:

| Trường | Nội dung | Lý do tồn tại |
|---|---|---|
| `ChannelHeader` | channelId = `mychannel`, type = `ENDORSER_TRANSACTION` | định tuyến đúng kênh |
| `ChaincodeID` | `identity-ledger` | peer biết gọi container chaincode nào |
| `input.args` | `["UpsertRecord", employeeId, ...]` | hàm + tham số (tất cả là `byte[]`) |
| `SignatureHeader.creator` | **X.509 cert** của `User1@org1` | peer biết "ai" đề xuất → kiểm tra MSP |
| `SignatureHeader.nonce` | số ngẫu nhiên | chống replay attack |
| `TxId` | `hash(nonce + creator)` | định danh giao dịch duy nhất |

Cert và khóa ký lấy từ cấu hình trong [FabricGatewayConfig.kt:41-45](fabric-spring-backend/src/main/kotlin/org/fabric/api/config/FabricGatewayConfig.kt#L41-L45):

```kotlin
val certificate = Identities.readX509Certificate(FileReader(props.gateway.certPath))
val privateKey  = readPrivateKey(props.gateway.keyPath)
val identity = X509Identity(props.mspId, certificate)   // mspId = "Org1MSP"
val signer   = Signers.newPrivateKeySigner(privateKey)
```

→ Mọi proposal đều được ký bằng `privateKey` này. Đây là *danh tính* mà phần endorsement/validation về sau dùng để xác thực.

### "Chaincode gồm những gì?"

Chaincode = smart contract, ở đây là [IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java). Cấu trúc:

- Class implement `ContractInterface`, gắn `@Contract(name=...)` ([dòng 30-32](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L30-L32)).
- Mỗi hàm public gắn `@Transaction(intent = SUBMIT | EVALUATE)`. `SUBMIT` = ghi ledger (như `UpsertRecord` [dòng 77](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L77)); `EVALUATE` = chỉ đọc (như `GetRecord` [dòng 157](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L157)).
- Tham số đầu luôn là `Context ctx`, từ đó lấy `ChaincodeStub stub = ctx.getStub()` — **mọi tương tác với world state đều qua `stub`**.

Chaincode này được đóng gói (`peer lifecycle chaincode package`), cài lên peer và chạy trong **một container Docker riêng** — đó là lý do peer cần quyền truy cập Docker socket trong [docker-compose.yaml:120](fabric-network/docker-compose.yaml#L120): `/var/run/docker.sock:/host/var/run/docker.sock`.

## 1.2. Gửi tới endorsing peers như thế nào? Gọi hàm gì?

`submitTransaction()` thực chất là 3 RPC gói gọn lại. Bên trong nó gọi tới **Gateway gRPC service** chạy trên gateway peer:

```
submitTransaction()  =  Endorse()  →  Submit()  →  CommitStatus()
                        └ bước 1.2/1.3   └ bước 2     └ chờ bước 3
```

Kết nối gRPC được dựng ở [FabricGatewayConfig.kt:33-36](fabric-spring-backend/src/main/kotlin/org/fabric/api/config/FabricGatewayConfig.kt#L33-L36):

```kotlin
NettyChannelBuilder
    .forTarget(props.peer.endpoint)   // localhost:7051 = peer0.org1
    .sslContext(sslContext)            // TLS bắt buộc vì peer bật CORE_PEER_TLS_ENABLED=true
    .build()
```

**Endorse RPC** gửi Proposal tới gateway peer. Gateway peer dùng **Service Discovery** để tính "endorsement plan": cần chữ ký của những org nào để thỏa policy? Policy được lấy từ [configtx.yaml:86-88](fabric-network/network/configtx/configtx.yaml#L86-L88):

```yaml
Endorsement:
  Type: ImplicitMeta
  Rule: "MAJORITY Endorsement"     # MAJORITY của 2 org = 2 → cần CẢ Org1 VÀ Org2 ký
```

→ Gateway peer (Org1) sẽ tự endorse, rồi forward proposal sang một peer của Org2 để xin chữ ký. Không phải viết dòng code nào cho việc này — đó là khác biệt lớn so với Legacy SDK.

> Lưu ý: đây là *channel-level default policy*. Nếu chaincode được deploy với endorsement policy riêng (`--signature-policy`), policy đó sẽ override. Hiện repo dùng default ⇒ `MAJORITY Endorsement`.

### "Làm thế nào để biết đã gửi thành công?"

Không có "mã trả về" kiểu HTTP. Cơ chế là **exception-based**: nếu endorse thất bại (peer từ chối, không đủ chữ ký, chaincode throw), SDK ném `EndorseException`. Bắt nó ở [IdentityLedgerService.kt:390-398](fabric-spring-backend/src/main/kotlin/org/fabric/api/service/IdentityLedgerService.kt#L390-L398):

```kotlin
private fun handleFabricError(fn: String, t: Throwable): Nothing {
    val msg = when (t) {
        is EndorseException -> "Endorse failed for $fn: ${t.message}"   // hỏng ở bước 1
        is SubmitException  -> "Submit failed for $fn: ${t.message}"    // hỏng ở bước 2
        else                -> "Fabric error in $fn: ${t.message}"
    }
    log.error(t) { msg }
    throw FabricTransactionException(msg, t)
}
```

Nếu `submitTransaction()` **return về `byte[]`** mà không ném exception ⇒ giao dịch đã endorse + commit thành công, và `resultBytes` chính là giá trị `return record;` từ chaincode ([IdentityLedger.java:100](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L100)).

Timeout "biết bao lâu thì coi là hỏng" được set ở [FabricGatewayConfig.kt:51-54](fabric-spring-backend/src/main/kotlin/org/fabric/api/config/FabricGatewayConfig.kt#L51-L54):

```kotlin
.endorseOptions      { it.withDeadlineAfter(15, TimeUnit.SECONDS) }  // chờ endorse
.submitOptions       { it.withDeadlineAfter(5,  TimeUnit.SECONDS) }  // chờ gửi orderer
.commitStatusOptions { it.withDeadlineAfter(60, TimeUnit.SECONDS) }  // chờ commit (lâu nhất)
```

`commitStatus` để 60s vì nó phải đợi qua hết bước order + validate + commit.

## 1.3. Endorsing peer mô phỏng chaincode trên world state như thế nào?

Peer nhận proposal → khởi chạy hàm trong container chaincode → chaincode **đọc/ghi qua `stub`**, nhưng "ghi" chỉ được **ghi vào bộ đệm (simulation)**, KHÔNG chạm DB thật. Ví dụ trong `UpsertRecord` ([IdentityLedger.java:96-97](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L96-L97)):

```java
stub.putStringState(key, genson.serialize(record));   // ghi → vào WRITE SET (chưa commit)
stub.setEvent("IdentityRecordUpserted", ...);          // event → phát khi commit
```

Còn hàm có đọc, ví dụ `DeleteRecord` ([dòng 124](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L124)):

```java
String json = stub.getStringState(key);   // đọc → ghi vào READ SET kèm version
```

Peer **không cập nhật ledger** ở pha này. Nó chỉ ghi lại "nếu giao dịch được duyệt thì sẽ đọc gì / ghi gì".

### "RW set trông như nào? Version của key đã đọc trông như thế nào?"

Read-Write Set là kết quả mô phỏng, dạng (mô tả khái niệm):

```jsonc
{
  "reads": [
    { "key": "profile:emp-123",
      "version": { "blockNum": 12, "txNum": 0 } }   // ← VERSION = vị trí lần ghi gần nhất
  ],
  "writes": [
    { "key": "profile:emp-123",
      "value": "{...IdentityRecord JSON...}",
      "isDelete": false }
  ]
}
```

**Version KHÔNG phải số tăng dần kiểu DB.** Nó là cặp `(blockNumber, transactionNumber)` — "key này được ghi lần cuối ở block số mấy, giao dịch thứ mấy trong block đó". Đây là chìa khóa cho MVCC ở bước 3: nếu lúc commit, version thực tế của `profile:emp-123` đã khác `(12,0)` ⇒ ai đó đã sửa trong lúc mình mô phỏng ⇒ conflict.

> Hệ quả thiết kế đáng chú ý: `UpsertRecord` chỉ `putStringState` mà **không `getStringState` trước** ([dòng 89-96](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L89-L96)) ⇒ read set rỗng cho key đó ⇒ **không bao giờ bị MVCC conflict** dù nhiều giao dịch ghi cùng key (blind write, "last writer wins"). Ngược lại `DeleteRecord` có đọc trước ⇒ *có thể* bị conflict nếu 2 lệnh delete chạy song song.

### "Peer ký diễn ra như thế nào? Trả về RW set cho client ra sao?"

Sau mô phỏng, mỗi endorsing peer tạo **ProposalResponse** và **ký** vào đó bằng khóa riêng (identity MSP của peer). Cụ thể nó ký lên `hash(rwset + response payload + chaincode event)`. Gói trả về gồm:

```
ProposalResponse {
  payload: { rwset, events, ... }
  endorsement: {
     endorser:  <X.509 cert của peer>      // để validator biết org nào ký
     signature: <ECDSA signature>          // bằng khóa peer
  }
  status: 200
}
```

Với Gateway SDK, các ProposalResponse này về **gateway peer** chứ không về thẳng backend. Gateway peer gom đủ chữ ký (Org1 + Org2), kiểm tra các RW set **khớp nhau** (nếu hai org mô phỏng ra kết quả khác nhau ⇒ non-deterministic ⇒ loại), rồi mới chuyển sang bước 2. Backend chỉ nhận kết quả cuối.

---

# BƯỚC 2 — ORDER (Sắp xếp)

## 2.1. Thu thập endorsement thỏa policy + đóng gói + gửi orderer

**Làm sao biết thỏa policy:** gateway peer dùng *Discovery* để biết plan, rồi đếm chữ ký theo `MAJORITY Endorsement`. Đủ ⇒ tiếp; thiếu (vd Org2 peer chết) ⇒ ném `EndorseException` ngược về backend.

**Đóng gói:** gateway peer gói thành một `Envelope` (transaction hoàn chỉnh):

```
Envelope {
  payload: {
    header:  { channelId: "mychannel", txId, creator cert }
    data: TransactionAction {
       chaincode_proposal_payload,           // input gốc
       action: {
          proposal_response_payload: { rwset },   // RW set đã chốt
          endorsements: [ {Org1 peer sig}, {Org2 peer sig} ]   // các chữ ký
       }
    }
  }
  signature: <chữ ký của client identity User1@org1>
}
```

**Gửi orderer:** qua **Submit RPC**, gateway peer broadcast Envelope tới orderer `orderer.example.com:7050`. Backend không mở kết nối trực tiếp tới orderer — đây là lý do `application.yml` chỉ khai báo `peer.endpoint`, không có địa chỉ orderer. Nếu orderer từ chối (vd sai Writers policy) ⇒ `SubmitException`.

## 2.2. Orderer thiết lập thứ tự tổng như thế nào?

Orderer dùng **etcdraft (Raft)**, cấu hình ở [configtx.yaml:92-101](fabric-network/network/configtx/configtx.yaml#L92-L101):

```yaml
Orderer: &OrdererDefaults
  OrdererType: etcdraft        # đồng thuận Raft
  EtcdRaft:
    Consenters:
      - Host: orderer.example.com
        Port: 7050             # repo demo: 1 orderer = single Raft node
```

Cơ chế: các Raft node bầu 1 **leader**. Leader nhận các Envelope, **áp đặt một thứ tự tuyến tính (total order)** bằng cách ghi chúng vào Raft log và replicate sang follower. Orderer **không thực thi chaincode, không kiểm tra nội dung RW set** — nó chỉ quyết định *thứ tự* và *đóng gói*. (Repo này chỉ có 1 orderer nên "đồng thuận" là tầm thường, nhưng cơ chế vẫn là Raft, sẵn sàng scale lên nhiều orderer.)

### "Gom giao dịch thành block như thế nào?"

Theo quy tắc cắt block ở [configtx.yaml:102-106](fabric-network/network/configtx/configtx.yaml#L102-L106):

```yaml
BatchTimeout: 2s              # cắt block sau tối đa 2 giây dù chưa đủ giao dịch
BatchSize:
  MaxMessageCount: 10         # hoặc cắt block ngay khi gom đủ 10 giao dịch
  AbsoluteMaxBytes: 99 MB     # trần cứng kích thước block
  PreferredMaxBytes: 512 KB   # ngưỡng mềm ưu tiên
```

→ Orderer cắt một block **khi xảy ra điều kiện nào trước**: đủ 10 tx, HOẶC đủ 99MB/512KB, HOẶC hết 2 giây. Đây là tham số đánh đổi **latency ↔ throughput**: `MaxMessageCount` cao + `BatchTimeout` lớn ⇒ throughput cao nhưng độ trễ cao. Demo để 10 tx / 2s là cân bằng cho tải nhẹ.

### "Phát block tới tất cả peers như thế nào?"

Orderer cung cấp **Deliver service**. Mỗi peer (cả 4: peer0/1 của org1, org2) mở một luồng deliver tới orderer và **kéo (pull) block mới** theo số thứ tự. Trong nội bộ một org, peer còn dùng **Gossip** để phát tán block cho nhau, giảm tải orderer — đây là lý do compose set `CORE_PEER_GOSSIP_*`, ví dụ [docker-compose.yaml:113-114](fabric-network/docker-compose.yaml#L113-L114):

```yaml
- CORE_PEER_GOSSIP_BOOTSTRAP=peer1.org1.example.com:8051       # peer0 học block từ peer1
- CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org1.example.com:7051
```

Mọi peer nhận **cùng một block, cùng thứ tự** — đó là nền tảng để world state hội tụ giống nhau.

---

# BƯỚC 3 — VALIDATE & COMMIT

Pha này chạy **độc lập trên từng peer**, cùng logic xác định ⇒ kết quả giống hệt nhau ở mọi peer.

## 3.1. Peer nhận block như thế nào?

Như trên: qua Deliver (từ orderer) + Gossip (giữa peer cùng org). Peer xếp block vào hàng đợi theo đúng số thứ tự (block N chỉ được xử lý sau block N-1) — đảm bảo tính tuyến tính.

## 3.2. Peer kiểm tra chữ ký như thế nào? (VSCC)

Với **từng giao dịch** trong block, peer chạy **VSCC** (Validation System Chaincode):

1. Đọc các `endorsements` trong giao dịch.
2. Với mỗi endorsement: lấy cert `endorser`, **xác minh chữ ký ECDSA** lên payload, và kiểm tra cert đó có thuộc MSP hợp lệ không (Org1MSP / Org2MSP — định nghĩa qua `MSPDir` ở [configtx.yaml:24](fabric-network/network/configtx/configtx.yaml#L24)).
3. Đối chiếu tập org đã ký với **endorsement policy** `MAJORITY Endorsement`. Thiếu chữ ký Org2? ⇒ giao dịch bị đánh dấu **`ENDORSEMENT_POLICY_FAILURE`** (invalid).

Đây là kiểm tra *mật mã + chính sách*. Nó bắt được trường hợp giao dịch giả mạo hoặc không đủ org bảo chứng.

## 3.3. Peer kiểm tra key đã bị giao dịch khác sửa chưa? (MVCC)

Sau VSCC là **MVCC check** trên read set:

- Với mỗi `{key, version}` trong **read set**, peer so với **version hiện tại của key đó trong world state**.
- Khớp ⇒ ok. **Lệch ⇒ `MVCC_READ_CONFLICT`** ⇒ giao dịch bị đánh **INVALID**.

Ví dụ minh họa với chaincode này: hai request `DeleteRecord("emp-123","profile")` cùng mô phỏng trên version `(12,0)`. Block N commit cái thứ nhất ⇒ version key thành `(N,0)`. Khi peer validate cái thứ hai, read set của nó vẫn ghi `(12,0)` ≠ `(N,0)` ⇒ conflict ⇒ tx thứ hai invalid (dù chaincode chạy đúng lúc mô phỏng). Đây chính là cơ chế chống "double-write" mà không cần khóa.

**Cách đánh dấu INVALID:** peer KHÔNG vứt bỏ giao dịch. Nó ghi một **mã validation code (1 byte) cho mỗi tx** vào **block metadata** — cụ thể là mảng `TRANSACTIONS_FILTER` trong `block.metadata`. `VALID = 0`, còn lại là các mã lỗi (`ENDORSEMENT_POLICY_FAILURE`, `MVCC_READ_CONFLICT`, ...). Nhờ vậy giao dịch invalid **vẫn nằm trong blockchain** (bất biến, audit được) nhưng **không tác động lên state**.

## 3.4. Ghi block vào blockchain + cập nhật world state

Peer commit theo 2 phần tách biệt:

**(a) Ghi block (immutable log):** append nguyên block vào *block store* — các file `blockfile_000000...` trong volume `peer0.org1.example.com:/var/hyperledger/production` ([docker-compose.yaml:123](fabric-network/docker-compose.yaml#L123)). Đây là chuỗi block bất biến (mỗi block chứa hash của block trước). **Mọi giao dịch — valid lẫn invalid — đều được ghi.**

**(b) Cập nhật world state:** chỉ với giao dịch **VALID**, peer áp **write set** vào *state database*. Đây là lúc `putStringState` ở [IdentityLedger.java:96](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L96) thực sự "có hiệu lực" trên DB. Đồng thời peer cập nhật version của key thành `(blockN, txIndex)` mới. Xong xuôi, peer **phát chaincode event** `IdentityRecordUpserted` (đăng ký ở [dòng 97](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L97)) — đây là tín hiệu để `commitStatus` của backend trả về và `submitTransaction()` kết thúc.

> Liên hệ kiến trúc: state DB chỉ lưu *partial data + hash* (`dataHash`), dữ liệu nhạy cảm đầy đủ nằm ở MySQL off-chain. Hàm `VerifyRecord` ([dòng 624](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L624)) so hash để phát hiện sửa đổi off-chain. Còn `GetRecordHistory` ([dòng 194](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java#L194)) gọi `getHistoryForKey` — nó đọc *toàn bộ block store* (phần a) để dựng lại lịch sử, chứ không đọc world state (phần b, vốn chỉ giữ giá trị mới nhất).

## "World state có phải bản ghi chung lưu đồng bộ ở từng peer không?"

**Không phải "đồng bộ" theo nghĩa replicate trực tiếp.** World state là một **materialized view (bản kết xuất) được mỗi peer tự tính ra cục bộ** từ chuỗi block:

- Mỗi peer giữ **bản sao world state riêng** (LevelDB hoặc CouchDB, trong volume của chính peer đó).
- Cái được đồng bộ giữa các peer là **chuỗi block** (qua orderer + gossip), KHÔNG phải world state.
- Vì mọi peer nhận **cùng block, cùng thứ tự**, và logic validate/commit là **xác định (deterministic)**, nên world state của chúng **tự hội tụ về trạng thái giống hệt nhau** — mà không cần đồng bộ trực tiếp DB với nhau.

Nói cách khác: **block store = nguồn chân lý (source of truth), bất biến, chia sẻ. World state = cache phái sinh, cục bộ, có thể tái dựng** bằng cách replay toàn bộ block từ đầu. Nếu một peer hỏng state DB, nó chỉ cần replay lại blockchain là khôi phục được nguyên trạng.

---

## Tóm tắt ánh xạ code → mô hình

| Pha | Ai làm | Code/Config trong repo |
|---|---|---|
| **Execute** (proposal + endorse) | gateway peer + endorsing peers | `contract.submitTransaction()` → `stub.putStringState()`; policy `MAJORITY Endorsement` ([configtx.yaml:86](fabric-network/network/configtx/configtx.yaml#L86)) |
| **Order** | orderer (etcdraft) | `OrdererType: etcdraft`, `BatchTimeout/BatchSize` ([configtx.yaml:92-106](fabric-network/network/configtx/configtx.yaml#L92-L106)) |
| **Validate** | từng peer (VSCC + MVCC) | mã validation trong block metadata; version `(blockNum, txNum)` của read set |
| **Commit** | từng peer | block store (volume `/var/hyperledger/production`) + state DB; event `setEvent(...)` đánh thức `commitStatus` |
