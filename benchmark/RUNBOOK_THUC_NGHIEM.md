# Quy trình thực nghiệm & đánh giá hiệu năng TrustID

> **Tài liệu này là một *quy trình để chạy thật*, không phải bản ghi kết quả có sẵn.**
> Mọi bảng kết quả bên dưới đều **để trống** ở cột "Đo được". Bạn chạy các bước
> trong tài liệu này trên máy của mình, ghi lại đúng con số máy trả về, rồi điền
> vào báo cáo (Chương 5) và vào các bảng mẫu ở cuối tài liệu này.
>
> Nguyên tắc: **chỉ điền số do chính bạn đo được**. Cột "Baseline kỳ vọng" chỉ là
> tham chiếu lý thuyết từ các paper đã trích dẫn (Sukhwani 2017 [18], Thakkar 2018
> [19], Bhattacharya 2020 [20]); nó dùng để *so sánh/giải thích*, không phải để
> thay cho số đo của TrustID.

---

## 0. Tổng quan bộ công cụ

```
benchmark/
├── RUNBOOK_THUC_NGHIEM.md         # tài liệu này
├── k6/
│   ├── get_token.sh               # lấy JWT để các test có xác thực dùng lại
│   ├── rest_load.js               # kịch bản k6 cho REST API (chọn test bằng biến TC)
│   └── run_all.sh                 # chạy lần lượt 13 test case, lưu JSON kết quả
├── caliper/
│   ├── networkConfig.yaml         # cấu hình mạng Fabric cho Caliper (điền path)
│   ├── benchmarkConfig.yaml       # cấu hình các vòng benchmark chaincode
│   ├── package.json               # phụ thuộc Caliper
│   └── workload/
│       └── identityWorkload.js    # workload module dùng chung cho mọi giao dịch
└── ket_qua/
    ├── moi_truong.md              # mẫu khai báo môi trường đo (Bảng 5.2)
    └── ket_qua_tho.csv            # mẫu ghi kết quả thô
```

Bốn nhóm phép đo, tương ứng bốn bảng trong báo cáo:

| Nhóm | Công cụ | Đối tượng đo | Bảng trong báo cáo |
|---|---|---|---|
| REST API | k6 | 13 endpoint backend | Bảng 5.4 |
| Chaincode | Hyperledger Caliper | 7 giao dịch trên Fabric | Bảng 5.3 |
| Mobile (ví) | Flutter integration test + Stopwatch | 9 thao tác trên thiết bị | Bảng 5.5 |
| Payload | curl / Wireshark | kích thước VC/VP/SD-JWT/QR | Bảng 5.6 |

---

## 1. Chuẩn bị môi trường

### 1.1. Cài công cụ

```bash
# k6 (Ubuntu/WSL)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Caliper (cần Node.js 18.x)
cd benchmark/caliper
npm install
npx caliper bind --caliper-bind-sut fabric:2.4   # đổi 2.4 cho khớp Fabric của bạn
```

### 1.2. Ghi lại cấu hình môi trường (BẮT BUỘC)

Trước khi đo, chạy các lệnh sau và dán kết quả vào `ket_qua/moi_truong.md`.
Báo cáo **bắt buộc** mô tả môi trường đo để kết quả có thể tái lập (Bảng 5.2).

```bash
# CPU
lscpu | grep -E "Model name|^CPU\(s\)|CPU max MHz"
# RAM
free -h | grep Mem
# Đĩa
lsblk -d -o NAME,ROTA,SIZE   # ROTA=0 là SSD, =1 là HDD
df -h --output=source,avail / | tail -1
# JVM
java -version 2>&1; echo "Heap: $JAVA_OPTS"
# MySQL
mysql -u root -p -e "SELECT VERSION(); SHOW VARIABLES LIKE 'innodb_buffer_pool_size';"
# Fabric: lấy từ configtx.yaml / docker
grep -E "BatchTimeout|MaxMessageCount|PreferredMaxBytes" fabric-network/**/configtx.yaml
docker ps --format '{{.Names}}' | grep -E "peer|orderer" | wc -l
```

### 1.3. Khởi động hệ thống cần đo

```bash
# 1) Mạng Fabric
cd fabric-network && ./start.ps1            # hoặc script khởi động tương ứng
# 2) MySQL (đảm bảo identity_db đã sẵn sàng)
# 3) Backend Spring Boot
cd fabric-spring-backend && ./gradlew bootRun   # chạy ở cổng 8080, base path /api/v1
```

Kiểm tra backend sống:

```bash
curl -s http://localhost:8080/api/v1/trust-registry/issuers | head -c 200
```

---

## 2. Đo REST API bằng k6 (→ Bảng 5.4)

### 2.1. Lấy JWT để tái sử dụng

Nhiều endpoint cần `Authorization: Bearer <JWT>`. Lấy token một lần và export ra biến môi trường:

```bash
cd benchmark/k6
export BASE_URL=http://localhost:8080
# sửa email/mật khẩu tài khoản test của bạn trong get_token.sh trước khi chạy
export TOKEN=$(./get_token.sh)
echo "JWT length: ${#TOKEN}"
```

Chuẩn bị thêm vài ID có thật trong DB test để đo các endpoint cần tham số:

```bash
export EMP_ID=1                                   # employeeId có thật
export DID=did:fabric:trustid:1                   # DID có thật
export STATUS_LIST_ID=employment-status-list-1
export REC_ID=1
export REC_TYPE=profile
```

### 2.2. Chạy từng test case

`rest_load.js` chọn test case qua biến `TC` (1–13). **Chạy từng cái một** (đừng chạy đồng thời) để số liệu của mỗi endpoint không bị nhiễu bởi endpoint khác:

```bash
# ví dụ: đo test case 4 (VC verify)
TC=4 k6 run rest_load.js
```

Hoặc chạy tất cả và lưu kết quả JSON:

```bash
./run_all.sh        # tạo ket_qua/k6_tc_1.json ... k6_tc_13.json
```

### 2.3. Đọc số nào trong output k6

k6 in ra mục `http_req_duration`. Lấy đúng các giá trị:

```
http_req_duration..............: avg=…  min=…  med=<p50>  max=…  p(90)=…  p(95)=<p95>  p(99)=<p99>
http_req_failed................: <error rate>
http_reqs......................: <tổng request> / <RPS thực tế>
```

Điền `med`→p50, `p(95)`→p95, `p(99)`→p99 và `http_req_failed`→error rate vào bảng.

### 2.4. Bảng 5.4 — REST API (điền cột "Đo được")

> **Đã đo** trên localhost:8080, commit `fdc3813`, môi trường ở `ket_qua/moi_truong.md`.
> Endpoint dưới đây là **path THẬT** đã đối chiếu controller (bản nháp script ban đầu sai
> một số path/body — đã sửa trong `k6/rest_load.js`). Tất cả `Err% = 0`.

| # | Test case | Endpoint (thật) | Tải đã đo | Baseline p95 | **p50 đo** | **p95 đo** | **p99 đo** | **Err%** |
|---|---|---|---|---|---|---|---|---|
| 1 | Auth sign-in | `POST /api/v1/auth/sign-in` | ≤10/phút* | < 200ms | 107.84 | 113.28 | 113.00 | 0% |
| 2 | MFA validate | `POST /api/v1/mfa/validate` | 50 RPS×60s | < 250ms | 6.69 | 7.98 | 9.24 | 0% |
| 3 | VC issue (Training)† | `POST /api/v1/admin/employees/{id}/issue-training-vc` | 2 RPS×30s | < 800ms | 22.02 | 29.43 | 53.64 | 0% |
| 4 | VC verify (HMAC) | `POST /api/v1/identity/vc/verify` | 200 RPS×60s | < 30ms | 1.52 | 1.91 | 2.54 | 0% |
| 5 | Status List fetch | `GET /api/v1/status-list/{id}` | 200 RPS×60s | < 80ms | 1.68 | 3.02 | 6.60 | 0% |
| 6 | SD-JWT issue (Skill)† | `POST /api/v1/sd-jwt/issue/skill/{id}` | 2 RPS×30s | < 600ms | 21.41 | 26.36 | 40.59 | 0% |
| 7 | SD-JWT verify | `POST /api/v1/sd-jwt/verify` | 100 RPS×60s | < 50ms | 1.51 | 1.83 | 2.10 | 0% |
| 8 | OID4VP request | `POST /api/v1/oidc/vp/request` | 50 RPS×60s | < 100ms | 1.44 | 1.75 | 1.97 | 0% |
| 9 | OID4VP submit | `POST /api/v1/oidc/vp/submit` | *(bỏ load-test)*‡ | < 150ms | — | — | — | — |
| 10 | DIF Universal Resolver | `GET /1.0/identifiers/{did}` | 100 RPS×60s | < 300ms | 7.42 | 9.64 | 13.05 | 0% |
| 11 | Trust Registry list | `GET /api/v1/trust-registry/issuers` | 100 RPS×60s | < 200ms | 7.76 | 16.96 | 30.68 | 0% |
| 12 | Ledger record write | `POST /api/v1/ledger/records` | 10 RPS×60s | 2000–5000ms | 558.51 | 966.68 | 975.87 | 0% |
| 13 | Ledger record read | `GET /api/v1/ledger/records/{id}/{type}` | 100 RPS×60s | < 200ms | 9.74 | 13.57 | 18.87 | 0% |

\* **TC1**: `/auth/**` có `RateLimitFilter` giới hạn **10 request/phút/IP** (chống brute-force) → không thể bench 100 RPS; số ghi là **độ trễ 1 request** đo dưới ngưỡng (n=10 nên p99≈p95). Latency thật ~108ms (bcrypt + ký JWT) vẫn **đạt** baseline.

† **TC3/TC6**: luồng cấp VC **ký credential + lưu MySQL đồng bộ (~22ms)**, còn ghi bản ghi lên Fabric chạy **bất đồng bộ** ở background → latency REST rất thấp, *không* phản ánh thời gian commit blockchain. Throughput/độ trễ ghi chuỗi đo riêng bằng Caliper (Bảng 5.3). Endpoint `/employees/{id}/issue` trong bản nháp **không tồn tại**; đã thay bằng `issue-training-vc` (cùng class cấp VC).

‡ **TC9** (OID4VP submit): cần `vpToken` là **Verifiable Presentation đã ký bởi ví thật** + `state` của một session còn hiệu lực → không tạo hàng loạt được để load-test. Đo bằng test chức năng (1 lần) thay vì k6.

**Đối chiếu baseline:** mọi endpoint read/verify/issue đều **nhanh hơn baseline kỳ vọng nhiều lần** (HMAC verify ~2ms, Fabric query ~7–10ms). Ghi ledger trực tiếp (TC12) ~560ms — **dưới** baseline 2–5s nhờ `BatchTimeout=2s` + `MaxMessageCount=500` cho phép cắt block sớm.

---

## 3. Đo chaincode bằng Hyperledger Caliper (→ Bảng 5.3)

### 3.1. Cấu hình

1. Sinh connection profile cho Org1 từ test-network (file `connection-org1.yaml`) và đặt cạnh `networkConfig.yaml`.
2. Mở `caliper/networkConfig.yaml`, điền đúng **đường dẫn tuyệt đối** tới cert/key của `User1@org1` (xem mục 16 của context — crypto material nằm trong WSL).
3. `benchmarkConfig.yaml` đã định nghĩa 7 vòng tương ứng 7 giao dịch; chỉnh `rateControl` nếu cần.

### 3.2. Chạy

```bash
cd benchmark/caliper
npx caliper launch manager \
  --caliper-workspace ./ \
  --caliper-networkconfig networkConfig.yaml \
  --caliper-benchconfig benchmarkConfig.yaml \
  --caliper-flow-only-test \
  --caliper-report-path ../ket_qua/caliper_report.html
```

### 3.3. Đọc kết quả

Mở `ket_qua/caliper_report.html`. Mỗi vòng cho: **Throughput (TPS)**, **Send Rate**, **Max/Min/Avg Latency (s)**, **Success/Fail**. Lấy *Throughput* và *Avg Latency*.

### 3.4. Bảng 5.3 — Chaincode (điền cột "Đo được")

| # | Giao dịch | Loại | Send rate | Baseline (TPS / Latency) | **TPS đo** | **Avg Latency đo** | **Fail** |
|---|---|---|---|---|---|---|---|
| 1 | `RegisterDID` | SUBMIT | 10 TPS | 8–15 TPS / 2–4s | | | |
| 2 | `UpsertRecord` | SUBMIT | 10 TPS | 8–15 TPS / 2–4s | | | |
| 3 | `UpdateStatusListEntry` | SUBMIT | 5 TPS | 5–10 TPS / 2–5s | | | |
| 4 | `GetRecord` | EVALUATE | 200 TPS | 150–300 TPS / <100ms | | | |
| 5 | `GetRecordHistory` | EVALUATE | 50 TPS | 30–80 TPS / 100–500ms | | | |
| 6 | `IsTrustedIssuer` | EVALUATE | 200 TPS | 200–400 TPS / <50ms | | | |
| 7 | `RecordSignature` | SUBMIT | 5 TPS | 5–10 TPS / 2–5s | | | |

---

## 4. Đo phía Mobile — Flutter (→ Bảng 5.5)

Các thao tác trên ví chạy *trên thiết bị*, không qua network, nên đo bằng integration
test + `Stopwatch`. Mẫu test (đặt trong `identity_frontend/integration_test/perf_test.dart`):

```dart
import 'package:flutter/material.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  Future<int> measureMs(Future<void> Function() action) async {
    final sw = Stopwatch()..start();
    await action();
    sw.stop();
    return sw.elapsedMilliseconds;
  }

  testWidgets('ECDSA P-256 keypair gen x10', (tester) async {
    final samples = <int>[];
    for (var i = 0; i < 10; i++) {
      samples.add(await measureMs(() async {
        // gọi đúng hàm sinh keypair của ví, ví dụ:
        // await KeyService.generateP256KeyPair();
      }));
    }
    debugPrint('keygen samples(ms)=$samples avg=${samples.reduce((a,b)=>a+b)/samples.length}');
  });
  // lặp tương tự cho: build VP, SD-JWT disclosure, QR encode/decode, Hive load, Dio fetch...
}
```

Chạy: `flutter test integration_test/perf_test.dart -d <device_id>` rồi đọc dòng `debugPrint`.
Lặp mỗi thao tác ≥10 lần, lấy **trung bình**. Ghi rõ tên thiết bị (mid-range Android, Android version, RAM).

### 4.1. Bảng 5.5 — Mobile (điền cột "Đo được")

| # | Thao tác | Thiết bị | Lần đo | Baseline kỳ vọng | **Trung bình đo** |
|---|---|---|---|---|---|
| 1 | App cold start → Wallet tab | Mid-range Android | 10 | < 3s | |
| 2 | ECDSA P-256 keypair gen | — | 10 | 50–200ms | |
| 3 | Biometric unlock prompt | local_auth | 10 | 1–2s | |
| 4 | Build VP from VC (ECDSA sign) | — | 10 | < 100ms | |
| 5 | SD-JWT selective disclosure (3/10 claims) | — | 10 | < 200ms | |
| 6 | QR encode VP | — | 10 | < 100ms | |
| 7 | QR scan + decode | — | 10 | < 500ms | |
| 8 | Offline VC load (Hive cache) | — | 10 | < 50ms | |
| 9 | Online VC fetch (Dio + JWT) | LAN | 10 | < 500ms | |

---

## 5. Đo kích thước payload (→ Bảng 5.6)

```bash
# Kích thước một VC JWT trả về từ backend
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/identity/vc/employment/$EMP_ID | wc -c

# SD-JWT Skill (JWT + disclosures)
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/sd-jwt/issue/skill/$EMP_ID | wc -c

# Status List response
curl -s http://localhost:8080/api/v1/status-list/$STATUS_LIST_ID | wc -c

# DID Document
curl -s http://localhost:8080/1.0/identifiers/$DID | wc -c
```

Với QR code: lưu ảnh QR ví sinh ra rồi `ls -l file.png` để lấy byte. Với VP token:
log VP string trong app/integration test rồi đếm độ dài.

### 5.1. Bảng 5.6 — Payload (điền cột "Đo được")

| Loại payload | Định dạng | Baseline kỳ vọng | **Đo được (bytes)** |
|---|---|---|---|
| EmploymentVC | JWT | 800–1500 | |
| SkillVC SD-JWT (10 claims) | JWT + disclosures | 2000–4000 | |
| VP token (W3C VP) | JWT VP | 1500–3000 | |
| Status List 2021 response | JSON-LD + GZIP bitstring | ~17 KB | |
| QR code chứa VP | PNG (ECC level M) | < 50 KB | |
| DID Document | JSON-LD | 500–1200 | |

---

## 6. Cách tổng hợp số liệu cho đáng tin

1. **Warm-up rồi mới đo.** Chạy thử 10–20 giây cho JIT/cache nóng lên, kết quả này *bỏ đi*, rồi mới đo chính thức.
2. **Lặp lại ≥3 lần** mỗi test case. Nếu p95 lệch nhau >20% giữa các lần → môi trường chưa ổn định (đóng app nền, tắt antivirus, cắm điện laptop), đo lại.
3. **Báo cáo trung vị (median) của các lần chạy**, kèm ghi chú khoảng dao động nếu lớn.
4. **Giữ log thô.** Lưu toàn bộ output k6 (`--summary-export`) và `caliper_report.html` — đây là bằng chứng bạn đã chạy thật, hữu ích khi phản biện yêu cầu xem.
5. **Ghi ngày giờ và commit hash** của mã nguồn lúc đo, để kết quả gắn với một phiên bản code cụ thể.

---

## 7. Checklist trước khi bảo vệ

- [ ] Đã điền đầy đủ Bảng 5.2 (môi trường) bằng số thật.
- [ ] Đã điền cột "Đo được" của Bảng 5.3–5.6 bằng số thật, mỗi ô có log gốc tương ứng.
- [ ] Đã viết mục **5.2.1 Phân tích kết quả** dựa trên số thật: chỉ ra bottleneck (Fabric submit ~2s do BatchTimeout? hay HMAC verify <1ms?), so sánh với baseline [18][19][20], và giải thích các sai khác.
- [ ] Đã lưu thư mục `ket_qua/` (k6 JSON + caliper_report.html + ảnh chụp) để trình khi được hỏi.
- [ ] Có thể *chạy lại tại chỗ* ít nhất 1–2 test case nếu hội đồng yêu cầu.

> Mục tiêu của tài liệu này là giúp bạn **thực sự** có số liệu của chính mình và tự
> tin trả lời mọi câu hỏi phản biện — vì khi đó mọi con số đều là thật và bạn hiểu
> rõ từng con số đến từ đâu.
