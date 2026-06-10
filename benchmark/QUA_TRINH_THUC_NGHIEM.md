# Quá trình thực nghiệm & đánh giá hiệu năng TrustID

> Tài liệu này ghi lại **toàn bộ quá trình thực nghiệm đã thực hiện** (không phải hướng
> dẫn lý thuyết): cách thiết lập, từng bước làm gì, gặp vấn đề gì, xử lý ra sao, kết quả
> đo được. Mọi con số đều do chạy thật trên hệ thống TrustID đang vận hành, commit
> `fdc3813`, ngày 2026-06-04/05.

---

## 0. Tổng quan

| Hạng mục | Công cụ | Đối tượng | Bảng | Trạng thái |
|---|---|---|---|---|
| REST API | k6 | 13 endpoint backend | 5.4 | ✅ 12/13, Err=0% |
| Chaincode | Hyperledger Caliper | 7 giao dịch Fabric | 5.3 | ✅ 7/7, Fail=0 |
| Payload | curl + wc -c | 7 loại payload | 5.6 | ✅ 5/7 (2 cần ví) |
| Mobile | (bỏ qua) | — | 5.5 | ⏭️ |

**Nguyên tắc xuyên suốt:** mỗi script trong repo (`k6/rest_load.js`, `caliper/workload`) ban
đầu được viết *phỏng đoán*; trước khi đo phải **đối chiếu controller/service thật của
backend** để sửa path / body / chữ ký tham số cho khớp — nếu không, mọi request fail.

---

## A. Thiết lập môi trường thực nghiệm (Setup)

### A.1. Hạ tầng
- **Máy đo + máy bị đo chung 1 VM** (GCP Compute Engine), giao tiếp qua **localhost/loopback**
  → loại nhiễu mạng, đúng tinh thần đo năng lực backend.
- **OS**: Ubuntu 22.04 LTS (Jammy), kernel 64-bit, chạy native (không WSL).
- **CPU**: Intel Xeon @2.20GHz, 4 vCPU · **RAM**: 15 GiB · **Đĩa**: SSD (sda, ROTA=0).
- **JVM**: OpenJDK 17, G1GC, MaxHeap mặc định ≈ 3.8 GB.
- **MySQL**: 8.0.46, innodb_buffer_pool_size = 128 MB (mặc định).
- **Fabric**: 7 container (4 peer + 1 orderer + 2 chaincode `dev-peer`), `BatchTimeout=2s`,
  `MaxMessageCount=500`, `PreferredMaxBytes=2MB`.

### A.2. Công cụ đo
- **k6** (REST load testing) — cài qua APT.
- **Hyperledger Caliper 0.6** (chaincode benchmark) — cài qua npm, bind SUT `fabric:2.4`.
- **jq** (parse JSON trong shell).

### A.3. Hệ thống bị đo
- **Spring Boot backend** chạy bằng `systemd` (`fabric-backend.service`), cổng 8080, base path `/api/v1`.
- **Mạng Fabric** (chaincode `identity-ledger`, channel `mychannel`).
- **MySQL** `identity_db`.

> Cấu hình môi trường đầy đủ (Bảng 5.2) lưu ở `ket_qua/moi_truong.md`.

---

## B. Các bước thực nghiệm

### Bước 1 — Cài đặt k6
- **Mục đích:** có công cụ bắn tải REST và đo độ trễ/percentile.
- **Thực hiện:** thêm GPG key + repo `dl.k6.io`, `apt-get install k6`.
- **Kết quả:** k6 v2.0.0 cài thành công.
- **Thu được:** ✅ sẵn sàng đo REST API (Bảng 5.4).

### Bước 2 — Cài & bind Caliper *(xử lý sự cố Node version)*
- **Mục đích:** có công cụ đo TPS/latency giao dịch chaincode.
- **Vấn đề gặp phải:** `npx caliper bind` lỗi `SyntaxError: Unexpected token '='` tại toán tử
  `||=`. Nguyên nhân: máy đang chạy **Node v12.22.9**, trong khi Caliper 0.6 yêu cầu
  **Node ≥ 18.19** (toán tử `||=` chỉ có từ Node 15+).
- **Thực hiện:**
  1. Cài `nvm`, rồi `nvm install 18 && nvm use 18` (→ Node v18.20.8).
  2. Xoá `node_modules` + `package-lock.json` cũ (đã build bằng Node 12), `npm install` lại.
  3. `npx caliper bind --caliper-bind-sut fabric:2.4`.
- **Kết quả:** bind thành công (cài `@hyperledger/fabric-gateway@1.5.0` + `@grpc/grpc-js`).
- **Thu được:** ✅ Caliper sẵn sàng; rút ra bài học **không chạy bằng `sudo`** (sudo dùng lại
  Node 12 của root, không thấy Node 18 trong nvm của user).

### Bước 3 — Ghi nhận cấu hình môi trường (Bảng 5.2)
- **Mục đích:** đảm bảo kết quả **tái lập được** — báo cáo bắt buộc mô tả môi trường đo.
- **Thực hiện:** chạy `lscpu`, `free -h`, `lsblk`, `java -version`, truy vấn MySQL,
  `grep` configtx, đếm container Fabric → dán vào `ket_qua/moi_truong.md`.
- **Kết quả:** điền đủ Bảng 5.2 (xem mục A.1).
- **Thu được:** ✅ bộ thông số môi trường gắn với từng con số đo.

### Bước 4 — Khởi động & kiểm tra hệ thống
- **Mục đích:** đảm bảo cả 3 thành phần (Fabric, MySQL, backend) sống trước khi đo.
- **Thực hiện:**
  - `docker ps | grep -E "peer|orderer"` → 7 container chạy.
  - `systemctl status fabric-backend.service / mysql` → cả hai `active (running)`.
  - `curl /api/v1/trust-registry/issuers` → trả `[]` (HTTP 200, endpoint sống).
- **Kết quả:** hệ thống sẵn sàng.
- **Thu được:** ✅ xác nhận baseline "hệ thống hoạt động" trước khi gây tải.

### Bước 5 — Đo REST API bằng k6 (→ Bảng 5.4)

#### 5a. Lấy JWT *(xử lý sai schema đăng nhập)*
- **Mục đích:** nhiều endpoint cần `Authorization: Bearer <JWT>`.
- **Vấn đề:** `get_token.sh` gửi `{"email","password"}` → backend báo
  *"missing parameter username"*. Đối chiếu `SignInRequest.kt` → field thật là **`username`**.
- **Thực hiện:** sửa script gửi `{"username","password"}`; đăng nhập lại.
- **Kết quả:** lấy được JWT (length 196), role `EMPLOYEE`.
- **Thu được:** ✅ token tái sử dụng cho mọi test cần xác thực.

#### 5b. Chạy thử & phát hiện rate-limit trên `/auth` *(phát hiện quan trọng)*
- **Mục đích:** đo độ trễ đăng nhập (TC1).
- **Hiện tượng:** ở 100 RPS → **99.7% request fail** ở ~1ms (server từ chối ngay).
- **Truy nguyên:** `RateLimitFilter.kt` giới hạn **10 request/phút/IP** cho `/api/v1/auth/*`
  (chống brute-force). Kiểm chứng khớp số: 100 RPS×60s chỉ lọt 10 → ~99.8% fail; 2 RPS×5s
  có req thứ 11 vượt capacity → đúng 9.09%.
- **Thực hiện:** đo TC1 ở mức **dưới ngưỡng** (1 RPS×10s) để lấy độ trễ thật 1 request.
- **Kết quả:** p50=107.84ms, p95=113.28ms, Err=0% — **đạt** baseline `<200ms`.
- **Thu được:** ✅ số TC1 trung thực + **một nhận xét bảo vệ**: đánh đổi throughput lấy an
  toàn (chống dò mật khẩu). Đây là *thiết kế đúng*, không phải lỗi.

#### 5c. Hiệu chỉnh `rest_load.js` cho khớp backend *(audit toàn bộ 13 endpoint)*
- **Mục đích:** tránh "chạy → fail → sửa" lặp lại; sửa hết một lần.
- **Thực hiện:** đọc controller thật của cả 13 endpoint, đối chiếu phân quyền
  (`SecurityConfig.kt`), sửa path/body/role trong `rest_load.js`. Các lỗi đã sửa:
  | TC | Lỗi script gốc | Sửa thành |
  |---|---|---|
  | 1 | body `email` | `username` |
  | 2 | thiếu `userId` | thêm `userId` (`/mfa/validate` cần userId+code) |
  | 3 | path `/employees/{id}/issue` **không tồn tại** | `/issue-training-vc` (cùng class cấp VC) |
  | 4 | body `{vcId}` | `{vc:"<json>"}` |
  | 6 | body `{}` | `{skills:{...}}` |
  | 7 | body `{sdJwt}` | `{presentation,requireClaims}` |
  | 8 | body `{}` | `{vcType,requestedClaims}` |
  | 12 | body sai hẳn | `{employeeId,recordType,status,keyFields,dataHash,action,...}` |
  - Bật `summaryTrendStats` để k6 tính cả **p(99)** (mặc định chỉ tới p95 → cột p99 ra 0).
  - Seed dữ liệu test bằng ADMIN token: tìm employee có VC (id=12), build SD-JWT presentation
    cho TC7, đặt key duy nhất/request cho TC12 (tránh xung đột MVCC của Fabric).
- **Kết quả:** script khớp 100% backend; chạy 12/13 test case **Err=0%** (TC9 cần VP ký bởi
  ví thật → bỏ load-test).
- **Thu được:** ✅ **Bảng 5.4 đầy đủ số liệu thật** (xem mục C).

### Bước 6 — Đo Chaincode bằng Caliper (→ Bảng 5.3)

#### 6a. Cấu hình mạng cho Caliper
- **Mục đích:** Caliper kết nối được peer + ký giao dịch bằng identity hợp lệ.
- **Thực hiện:**
  - Đối chiếu `application.yml` của backend → lấy đúng path crypto material (`User1@org1`:
    `priv_sk` + `User1@org1.example.com-cert.pem`), TLS CA, channel `mychannel`, chaincode
    `identity-ledger`.
  - **Tạo mới `connection-org1.yaml`** (repo chưa có) — CCP 1 peer, `grpcs://localhost:7051`,
    `ssl-target-name-override: peer0.org1.example.com`.
  - Điền `networkConfig.yaml` với path tuyệt đối + `discover: true`.
- **Kết quả:** 2 file cấu hình hoàn chỉnh.
- **Thu được:** ✅ Caliper có đủ thông tin để kết nối.

#### 6b. Sửa workload cho khớp chữ ký chaincode *(audit interface)*
- **Mục đích:** tham số sinh ra phải khớp **đúng** chữ ký hàm chaincode.
- **Vấn đề:** `identityWorkload.js` gốc lệch nặng (đối chiếu `IdentityLedgerService.kt`):
  | Hàm | Workload cũ | Chaincode thật |
  |---|---|---|
  | `UpsertRecord` | 2 args | **8** (employeeId, recordType, status, keyFields, dataHash, action, timestamp, updatedBy) |
  | `RegisterDID` | 2 args | **5** (did, employeeId, publicKeyJwk, controller, timestamp) |
  | `UpdateStatusListEntry` | 3 args | **7** (listId, encodedList, size, updatedIndex, revoked, timestamp, updatedBy) |
  | `GetRecord`/`History` | 1 arg | **2** (recordType, employeeId) |
  | `RecordSignature` | 3 args | **6** (contractId, signerDid, signatureBase64, docHash, timestamp, updatedBy) |
- **Thực hiện:** viết lại `_genArgs()` khớp chữ ký; key **duy nhất/tx** cho giao dịch ghi
  (tránh MVCC); seed 1 record đúng 8 tham số cho các vòng đọc (GetRecord/History).
- **Kết quả:** workload hợp lệ.
- **Thu được:** ✅ sẵn sàng chạy 7 vòng.

#### 6c. Smoke test rồi chạy full
- **Mục đích:** xác nhận kết nối/identity/TLS/chaincode trước khi chạy full ~6600 tx.
- **Thực hiện:** chạy 1 vòng `IsTrustedIssuer` 10 tx → **Succ=10, Fail=0**, peer-gateway
  connector khởi tạo OK. Sau đó chạy full `benchmarkConfig.yaml` (7 vòng, 5 worker).
- **Kết quả:** **cả 7 vòng Fail=0**.
- **Thu được:** ✅ **Bảng 5.3 đầy đủ** + báo cáo `ket_qua/caliper_report.html`.

### Bước 7 — Đo kích thước payload (→ Bảng 5.6)
- **Mục đích:** đo độ lớn VC/SD-JWT/Status List/DID Document trên dây.
- **Thực hiện:** `curl <endpoint> | jq -rj '<field>' | wc -c` cho từng loại; issue SD-JWT
  10 claim để so với baseline; đo presentation reveal 3/10 claim.
- **Kết quả:** 5/7 loại đo được (VP token & QR cần ví Flutter — Mục 4 đã bỏ).
- **Thu được:** ✅ **Bảng 5.6** + nhận xét về HMAC (payload gọn) và gzip Status List.

---

## C. Bảng kết quả đo (tổng hợp)

### Bảng 5.2 — Môi trường đo (tóm tắt)
| Thành phần | Giá trị |
|---|---|
| CPU / RAM / Đĩa | Xeon @2.20GHz 4 vCPU / 15 GiB / SSD |
| OS | Ubuntu 22.04 LTS (native, không WSL) |
| JVM | OpenJDK 17, G1GC, MaxHeap ≈ 3.8 GB |
| MySQL | 8.0.46, buffer pool 128 MB |
| Fabric | 7 container, BatchTimeout 2s, MaxMsg 500, 2 MB |
| Mạng | localhost (cùng VM) |
| Commit | `fdc3813` |

### Bảng 5.4 — REST API (k6, Err=0% toàn bộ)
| # | Test case | Tải | p50 (ms) | p95 (ms) | p99 (ms) | Baseline p95 | Đạt |
|---|---|---|---|---|---|---|---|
| 1 | Auth sign-in | ≤10/phút¹ | 107.84 | 113.28 | ~113 | <200ms | ✅ |
| 2 | MFA validate | 50 RPS×60s | 6.69 | 7.98 | 9.24 | <250ms | ✅ |
| 3 | VC issue (Training)² | 2 RPS×30s | 22.02 | 29.43 | 53.64 | <800ms | ✅ |
| 4 | VC verify (HMAC) | 200 RPS×60s | 1.52 | 1.91 | 2.54 | <30ms | ✅ |
| 5 | Status List fetch | 200 RPS×60s | 1.68 | 3.02 | 6.60 | <80ms | ✅ |
| 6 | SD-JWT issue (Skill)² | 2 RPS×30s | 21.41 | 26.36 | 40.59 | <600ms | ✅ |
| 7 | SD-JWT verify | 100 RPS×60s | 1.51 | 1.83 | 2.10 | <50ms | ✅ |
| 8 | OID4VP request | 50 RPS×60s | 1.44 | 1.75 | 1.97 | <100ms | ✅ |
| 9 | OID4VP submit | bỏ³ | — | — | — | <150ms | — |
| 10 | DID resolve | 100 RPS×60s | 7.42 | 9.64 | 13.05 | <300ms | ✅ |
| 11 | Trust Registry | 100 RPS×60s | 7.76 | 16.96 | 30.68 | <200ms | ✅ |
| 12 | Ledger write (SUBMIT) | 10 RPS×60s | 558.51 | 966.68 | 975.87 | 2000–5000ms | ✅ |
| 13 | Ledger read | 100 RPS×60s | 9.74 | 13.57 | 18.87 | <200ms | ✅ |

¹ Rate-limit 10/phút/IP (`RateLimitFilter`) → đo độ trễ 1 request, n nhỏ nên p99≈p95.
² Latency thấp vì ký VC + lưu MySQL đồng bộ; ghi Fabric chạy bất đồng bộ (background).
³ Cần VP token ký bởi ví thật + state hợp lệ → không tạo hàng loạt được, đo bằng test chức năng.

### Bảng 5.3 — Chaincode (Caliper, Fail=0 toàn bộ)
| # | Giao dịch | Loại | Send rate | TPS đo | Avg Latency | Max Latency | Fail |
|---|---|---|---|---|---|---|---|
| 1 | RegisterDID | SUBMIT | 10 | 10.1 | 0.35s | 0.66s | 0 |
| 2 | UpsertRecord | SUBMIT | 10 | 10.1 | 0.35s | 0.66s | 0 |
| 3 | UpdateStatusListEntry | SUBMIT | 5 | 5.1 | 0.59s | 1.13s | 0 |
| 4 | GetRecord | EVALUATE | 200 | 200.1 | 0.01s | 0.07s | 0 |
| 5 | GetRecordHistory | EVALUATE | 50 | 50.2 | 0.01s | 0.08s | 0 |
| 6 | IsTrustedIssuer | EVALUATE | 200 | 200.1 | 0.02s | 0.06s | 0 |
| 7 | RecordSignature | SUBMIT | 5 | 5.1 | 0.59s | 1.12s | 0 |

> Throughput đo = send rate đã đặt (Fail=0) → hệ thống **chưa bão hòa**; chỉ số có nghĩa là
> **latency**. Tìm TPS trần là hướng mở rộng.

### Bảng 5.6 — Payload
| Loại payload | Đo được | Baseline | Nhận xét |
|---|---|---|---|
| EmploymentVC (JSON VC) | **920 B** | 800–1500 | ✅ |
| SkillVC SD-JWT (10 claims) | **1972 B** | 2000–4000 | Gần ngưỡng dưới (SD-JWT gọn) |
| SD-JWT presentation (reveal 3/10) | **1529 B** | 1500–3000 | Proxy cho VP; ít claim → nhỏ hơn |
| Status List 2021 | **761 B** | ~17 KB | Bitstring chủ yếu = 0 → gzip nén mạnh |
| DID Document | **668 B** | 500–1200 | ✅ (full result + metadata = 1045 B) |
| VP token W3C / QR PNG | (đo trên ví) | — | Mục 4 bỏ |

---

## D. Phát hiện & phân tích chính (cho mục 5.2.1)

1. **Đối chiếu chéo 2 lớp tăng độ tin cậy:** ghi ledger đo bằng Caliper (SUBMIT ~0.35–0.59s)
   ≈ đo bằng REST (TC12 ~0.56s) — cùng một thao tác ghi chuỗi nhìn từ chaincode và từ API,
   số khớp nhau.
2. **EVALUATE 10–20ms vs SUBMIT 350–590ms** — chênh ~25 lần, đúng bản chất: đọc world-state
   (LevelDB) không qua ordering, ghi phải endorsement → ordering → commit.
3. **Issue VC nhanh (~22ms)** do Fabric anchoring bất đồng bộ; throughput ghi chuỗi thật đo
   ở Caliper. Kiến trúc tách "trải nghiệm cấp VC nhanh" khỏi "anchoring blockchain nền sau".
4. **SUBMIT nhanh hơn baseline 2–5s** nhờ `BatchTimeout=2s` cho phép cắt block sớm.
5. **HMAC-SHA256** (thay chữ ký bất đối xứng) → VC/SD-JWT gọn (920–1972 B) và verify ~2ms;
   đánh đổi: cần khóa chia sẻ để verify (điểm hội đồng có thể phản biện).
6. **Rate-limit 10/phút/IP trên `/auth`** — đánh đổi throughput lấy chống brute-force.

---

## E. Bằng chứng (artifact)
| File | Vị trí | Nội dung |
|---|---|---|
| `k6_tc_*.json` | VPS `benchmark/ket_qua/` | summary từng test REST |
| `caliper_report.html` | VPS `benchmark/ket_qua/` | báo cáo Caliper 7 vòng |
| `caliper_smoke.html` | VPS `benchmark/ket_qua/` | báo cáo smoke test |
| `ket_qua_tho.csv` | repo `benchmark/ket_qua/` | tổng hợp số thô |
| `moi_truong.md` | repo `benchmark/ket_qua/` | cấu hình môi trường (Bảng 5.2) |

> **Lưu ý:** file báo cáo nằm ở repo (Windows); bằng chứng JSON/HTML đang ở VPS — copy về
> (vd `scp`) để đưa vào phụ lục.
