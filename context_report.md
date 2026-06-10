# Context viết báo cáo học thuật — TrustID: Ứng dụng Blockchain trong Định danh số

> **Mục đích**: File này ánh xạ **từng đầu mục** của khung báo cáo `Bao_cao_do_an_v8_hoan_thien_formatted.md` sang **nội dung/dữ kiện cần viết**, dựa trên hệ thống TrustID đã hiện thực. Dùng kèm `project_context.md` (nguồn sự thật chi tiết, 54KB) — file này là *bản đồ viết*, `project_context.md` là *kho dữ kiện*.
>
> **Cách dùng**: Với mỗi mục → đọc "Cần viết gì" (góc nhìn học thuật) + "Dữ kiện" (số liệu/sự thật từ TrustID) + "Nguồn sâu" (trỏ đến section trong `project_context.md`). Mở rộng thành văn xuôi học thuật theo các quy ước ở phần 0.

---

## 0. Quy ước chung khi viết (áp dụng toàn báo cáo)

- **Văn phong**: học thuật, ngôi "chúng tôi" / "tác giả"; tránh ngôn ngữ marketing ("tuyệt vời", "mạnh mẽ").
- **Trích dẫn**: chuẩn **IEEE** `[n]`, đánh số theo thứ tự xuất hiện; danh mục đầy đủ ở chương Tài liệu tham khảo (lấy từ `project_context.md` §17).
- **Thuật ngữ**: lần đầu viết đầy đủ + viết tắt, ví dụ "Định danh tự chủ (Self-Sovereign Identity — SSI)"; sau đó dùng viết tắt.
- **Sợi chỉ xuyên suốt (narrative thread)** — nhắc lại nhất quán ở mọi chương:
  1. Định danh tập trung truyền thống → người dùng mất quyền kiểm soát dữ liệu, single point of failure, rò rỉ PII hàng loạt.
  2. SSI (mô hình Trust Triangle W3C) đặt **Holder làm trung tâm**.
  3. Blockchain (Hyperledger Fabric) đóng vai trò **Verifiable Data Registry** — chỉ lưu DID, Trust Registry, Status List, hash; **KHÔNG lưu PII** (PII nằm off-chain ở MySQL để tuân thủ GDPR).
  4. Use-case minh hoạ: **workplace credentials** (chứng chỉ việc làm/kỹ năng/học vấn/chấm dứt HĐ); HRMS chỉ là Issuer minh hoạ, không phải sản phẩm chính.
- **KỶ LUẬT SỐ LIỆU**: tuyệt đối **không bịa số benchmark**. Số hiệu năng phải đo thật (k6 + Hyperledger Caliper) rồi điền vào bảng ở `project_context.md` §12.6. Baseline trong context chỉ là tham chiếu lý thuyết từ paper, phải ghi rõ là "kỳ vọng/tham chiếu".
- **4 vai trò cố định** (dùng đúng tên xuyên suốt):

| Vai trò | Hiện thực trong TrustID | Trách nhiệm |
|---|---|---|
| Issuer | Spring Boot backend (Org1) | Cấp VC, ký số (HMAC-SHA256), anchor hash lên Fabric |
| Holder | Flutter mobile app | Giữ DID + VC + private key ECDSA P-256, tạo VP có chọn lọc |
| Verifier | React Verifier Portal (SPA, không cần login) | Xác minh chữ ký VC/VP, check Status List on-chain |
| Verifiable Data Registry | Hyperledger Fabric 2.x | DID Document, Trust Registry, Status List 2021, audit log, e-sign |

---

# CHƯƠNG 1 — TỔNG QUAN ĐỀ TÀI

> Chương này phần lớn là **literature / lý thuyết**; `project_context.md` mỏng ở đây nên phần dưới cung cấp trực tiếp nội dung cốt lõi cần viết. Mục 1.4 dùng lại §1.5 (Related Work) của `project_context.md`.

## 1.1. Tính cấp thiết của đề tài
**Cần viết gì**: Lập luận vì sao phải nghiên cứu định danh số phi tập trung dùng blockchain — nêu vấn đề của hiện trạng.
**Dữ kiện / luận điểm**:
- Mô hình định danh tập trung (mỗi dịch vụ một tài khoản, hoặc IdP tập trung kiểu federated) gây: (a) silo dữ liệu, (b) single point of failure → các vụ rò rỉ dữ liệu định danh quy mô lớn, (c) người dùng không kiểm soát được ai giữ/chia sẻ dữ liệu của mình.
- Xu hướng quy định: GDPR (EU 2016/679) đề cao data minimization, quyền được lãng quên (Art.17), quyền chuyển dữ liệu (Art.20) → đòi hỏi kiến trúc định danh mới.
- SSI + Verifiable Credentials (chuẩn W3C) là hướng đi được công nhận để trả quyền kiểm soát cho người dùng; blockchain cung cấp registry phi tập trung, bất biến, có kiểm toán mà không cần cơ quan trung tâm.
- Bối cảnh doanh nghiệp: nhu cầu cấp/xác minh chứng chỉ nhân sự (việc làm, kỹ năng, học vấn) nhanh, chống giả mạo, liên thông giữa các tổ chức.
**Citation gợi ý**: W3C VC [1], W3C DID [2], GDPR [8], Mühle et al. [22], Dunphy & Petitcolas [21].

## 1.2. Mục tiêu nghiên cứu của đề tài
**Cần viết gì**: Đề tài giải quyết vấn đề gì — phát biểu mục tiêu tổng quát + mục tiêu cụ thể.
**Dữ kiện**:
- Mục tiêu tổng quát: nghiên cứu và **hiện thực hoá một nền tảng SSI hoàn chỉnh (reference implementation)** trên Hyperledger Fabric cho workplace credentials, tuân thủ chuẩn W3C/IETF.
- Mục tiêu cụ thể: (1) triển khai đầy đủ Trust Triangle Issuer/Holder/Verifier/Registry; (2) áp dụng 4 chuẩn: W3C VC Data Model, W3C DID Core, W3C Status List 2021, IETF SD-JWT; (3) đảm bảo privacy (selective disclosure) + tuân thủ GDPR; (4) thiết kế cơ chế đồng bộ off-chain/on-chain tin cậy (Outbox/eventual consistency); (5) đánh giá bảo mật (STRIDE/DREAD) và hiệu năng.
**Nguồn sâu**: `project_context.md` §1, §13 (Đạt được).

## 1.3. Đối tượng và phạm vi nghiên cứu
### 1.3.1. Đối tượng nghiên cứu
- **Công nghệ**: Hyperledger Fabric (permissioned blockchain), W3C VC/DID, SD-JWT, Status List 2021, chữ ký số (HMAC-SHA256 cho VC ở POC, ECDSA P-256 cho VP của Holder).
- **Nghiệp vụ**: cấp phát chứng chỉ số (workplace credentials), xác minh chứng chỉ, thu hồi danh tính/credential, e-sign hợp đồng, công chứng tài liệu (notarization).
### 1.3.2. Phạm vi nghiên cứu
- **1.3.2.1. Không gian**: ứng dụng trong phạm vi **một doanh nghiệp đơn lẻ (single-Org, Org1)** đóng vai trò Issuer minh hoạ; mô hình thiết kế hướng tới khả năng mở rộng đa tổ chức (cross-organization) nhưng POC giới hạn 1 Org. Là **POC học thuật** (đồ án tốt nghiệp), không phải sản phẩm thương mại.
- **1.3.2.2. Thời gian**: công nghệ/chuẩn cập nhật đến 2025–2026 (W3C VC Data Model 2.0 (2025), Fabric 2.x); dữ liệu thực nghiệm đo trong giai đoạn thực hiện đồ án (ghi rõ mốc cụ thể khi bảo vệ).

## 1.4. Khảo sát các nền tảng blockchain hiện có
**Cần viết gì**: Các loại blockchain + ứng dụng nổi bật có giá trị thực tiễn; sau đó hẹp dần về các nền tảng SSI để dẫn vào research gap.
**Dữ kiện — nền tảng blockchain tiêu biểu**:
- **Public permissionless**: Bitcoin (tiền điện tử, lưu trữ giá trị), Ethereum (smart contract, DeFi, NFT, DApp).
- **Permissioned / enterprise**: Hyperledger Fabric (chuỗi cung ứng, tài chính, định danh), R3 Corda (tài chính liên ngân hàng), Quorum/Hyperledger Besu (Ethereum permissioned).
- **Ứng dụng thực tiễn nổi bật**: IBM Food Trust & TradeLens (truy xuất chuỗi cung ứng — Fabric), BC Gov OrgBook (SSI — Indy), EBSI (hạ tầng định danh/giáo dục EU).
**Dữ kiện — nền tảng SSI (dẫn vào research gap)**: bảng so sánh đầy đủ TrustID vs **Hyperledger Indy/Aries, Sovrin, Microsoft Entra Verified ID, ION (DIF), EBSI, Veramo** có sẵn ở `project_context.md` §1.5.1; phân tích positioning §1.5.2; research gap §1.5.3 (4 khoảng trống: reference impl SSI trên Fabric còn hiếm; kết hợp Status List 2021 + SD-JWT thay vì AnonCreds; HRMS làm Issuer minh hoạ; mobile-first wallet biometric ECDSA).
**Nguồn sâu**: `project_context.md` §1.5 (toàn bộ).
**Citation**: Indy/Aries [12], Sovrin [13], Entra [14], ION [15], EBSI [16], Veramo [17].

## 1.5. Tổng quan về blockchain
### 1.5.1. Khái niệm và đặc tính
- **Khái niệm**: sổ cái phân tán (distributed ledger) gồm các khối (block) liên kết bằng hàm băm mật mã tạo thành chuỗi; mỗi block chứa hash của block trước → bất biến; được nhân bản và đồng thuận giữa nhiều node, không cần bên trung gian tin cậy.
- **Đặc tính cốt lõi & lợi ích thực tiễn**:
  - *Phi tập trung (decentralization)* → loại bỏ single point of failure, không phụ thuộc bên trung gian.
  - *Bất biến (immutability)* → chống sửa/giả mạo dữ liệu lịch sử; nền tảng cho audit & non-repudiation.
  - *Minh bạch & truy vết (transparency/auditability)* → mọi giao dịch có thể kiểm toán.
  - *An toàn mật mã (cryptographic security)* → chữ ký số, hàm băm bảo đảm toàn vẹn & xác thực.
  - *Đồng thuận (consensus)* → các node thống nhất một trạng thái duy nhất.
### 1.5.2. Phân loại blockchain
- **Theo quyền truy cập**:
  - *Public / permissionless* (Bitcoin, Ethereum): ai cũng tham gia/đọc/ghi; đồng thuận PoW/PoS; chậm, tốn phí.
  - *Private / permissioned* (Fabric): chỉ thành viên được cấp phép; biết rõ danh tính qua MSP/CA; nhanh, không cần token.
  - *Consortium* (liên minh nhiều tổ chức cùng quản trị) và *Hybrid*.
- **Bảng so sánh đề xuất** (public vs permissioned): trục tiêu chí = identity participant, throughput, phí/gas, cơ chế đồng thuận, quyền riêng tư, use case phù hợp → kết luận: enterprise SSI hợp với **permissioned** ⇒ dẫn sang Fabric.
**Citation**: Hyperledger Fabric docs [6].

## 1.6. Tổng quan về Hyperledger Fabric
### 1.6.1. Bối cảnh ra đời
- Dự án mã nguồn mở dưới **Linux Foundation** (sáng kiến Hyperledger, khởi xướng 2015–2016), với đóng góp ban đầu lớn từ **IBM** và Digital Asset.
- Mục đích: blockchain **cho doanh nghiệp** (enterprise-grade), permissioned, modular.
- Khác biệt cốt lõi: **không có cryptocurrency/token bắt buộc**, identity của participant được quản lý chặt qua **MSP + X.509 CA**, hỗ trợ smart contract đa năng (chaincode) bằng ngôn ngữ phổ thông (Go/Java/Node.js).
### 1.6.2. Đặc điểm chính
- **Permissioned**: thành viên định danh rõ ràng qua MSP.
- **Modular architecture**: pluggable consensus, pluggable MSP, có thể thay thế thành phần.
- **Privacy & confidentiality**: Channel (cô lập sổ cái theo nhóm) + Private Data Collections.
- **High performance & scalability**: tách Execute/Order/Validate → throughput cao (tham chiếu ~vài nghìn TPS theo điều kiện).
- **Pluggable consensus**: chọn cơ chế đồng thuận theo nhu cầu.
### 1.6.3. Kiến trúc tổng thể
Giải thích các thành phần: **Organization** (đơn vị quản trị), **Peer node** (giữ ledger + thực thi chaincode; có endorsing/committing peer), **Ordering Service** (sắp xếp giao dịch thành block), **Channel** (sổ cái riêng cho nhóm thành viên), **Ledger** (gồm *world state* — key/value hiện tại, thường CouchDB/LevelDB + *blockchain* — chuỗi giao dịch bất biến), **Chaincode** (smart contract), **MSP/CA** (định danh X.509).
> Áp vào TrustID: 1 Orderer, **2 Peers (Org1)**, **1 CA (Org1)**, channel `mychannel`, chaincode deploy tên `identity-ledger`.
### 1.6.4. Cơ chế giao dịch — Execute-Order-Validate
- **Execute**: client gửi proposal → endorsing peers thực thi chaincode (simulate), ký endorsement (chưa cập nhật ledger).
- **Order**: ordering service gom các giao dịch đã endorse, sắp thứ tự, đóng block.
- **Validate**: committing peers kiểm tra endorsement policy + MVCC (đọc/ghi không xung đột) → ghi vào ledger, cập nhật world state.
- Khác biệt với Ethereum (Order-Execute): Fabric thực thi **trước** khi đồng thuận → song song hoá, throughput cao hơn.
### 1.6.5. Cơ chế đồng thuận và sự tiến hoá
- Tiến hoá: **Solo** (dev) → **Kafka/Zookeeper** (deprecated) → **Raft (etcdraft)** mặc định từ Fabric 1.4.1+ (Crash Fault Tolerant — CFT) → **BFT/SmartBFT** (Byzantine Fault Tolerant) bổ sung từ **Fabric 3.0**.
- **Trong TrustID**: orderer dùng **`etcdraft` (Raft)** — xác nhận tại [configtx.yaml:93](fabric-network/network/configtx/configtx.yaml#L93). Đây là **lựa chọn** (pluggable), không bắt buộc; Raft là CFT phù hợp môi trường tin cậy single/few-Org. SmartBFT là tuỳ chọn cho môi trường cần chịu lỗi Byzantine (đa tổ chức không tin nhau).
### 1.6.6. Ưu điểm, hạn chế, ứng dụng thực tế
- **Ưu điểm**: bảng "Tại sao chọn Fabric" ở `project_context.md` §11 (permissioned, không token/gas, chaincode Java/Go, privacy qua channel/PDC, throughput cao, tách endorsement/ordering, MSP X.509).
- **Hạn chế**: phức tạp vận hành (cert/MSP/channel), không phi tập trung tối đa như public chain, CFT (Raft) không chống Byzantine, cần hạ tầng Docker/K8s.
- **Ứng dụng thực tế**: IBM Food Trust (truy xuất thực phẩm), TradeLens (logistics), trade finance liên ngân hàng, định danh (đề tài này).
**Nguồn sâu**: `project_context.md` §11. **Citation**: [6].

---

# CHƯƠNG 2 — PHÂN TÍCH VÀ THIẾT KẾ KIẾN TRÚC

> Chương này được `project_context.md` phủ rất đầy. Phần dưới chỉ ánh xạ heading → section nguồn + chốt các con số.

## 2.1. Phân tích yêu cầu hệ thống định danh số thế hệ mới
### 2.1.1. Yêu cầu chức năng
**Cần viết gì**: liệt kê chức năng theo **tác nhân** (Admin/Issuer, Holder/Employee, Verifier, Chief/Manager) + mô tả.
**Dữ kiện**: ánh xạ với 25 controllers backend & các tab Flutter (Wallet/Verifier/Workplace/Profile). Nhóm chức năng:
- *Issuer/Admin*: cấp các loại VC (xem bảng §8), issue SD-JWT, quản lý Trust Registry, dashboard KPI, audit log, company onboarding, directory.
- *Holder*: lưu DID/VC, sinh keypair, build VP + selective disclosure, e-sign, MFA, GDPR export/erase, quản lý session/device.
- *Verifier*: verify VC/VP, check Status List, xem trusted issuers — không cần đăng nhập.
- *HRMS (Workplace)*: attendance, requests, payroll, contract.
**Nguồn sâu**: §7 (tính năng theo phase), §8 (các loại VC), §15 (API reference đầy đủ).
### 2.1.2. Yêu cầu phi chức năng (theo ISO/IEC 25010)
**Cần viết gì**: ánh xạ thuộc tính chất lượng ISO 25010 ↔ cơ chế trong TrustID.
**Bảng đề xuất**:
| Đặc tính ISO 25010 | Hiện thực trong TrustID |
|---|---|
| Security | JWT, MFA TOTP, rate limit Bucket4j, account lockout, device binding, mã hoá, STRIDE/DREAD (§12.5) |
| Reliability | Dual-write + Outbox Pattern → eventual consistency (§6.3) |
| Performance efficiency | tách read/write Fabric (evaluate vs submit), HMAC verify sub-ms; đo §12.6 |
| Maintainability | Clean Architecture 4 layer (§5) |
| Portability | Docker Fabric, Spring Boot, Flutter đa nền tảng |
| Compatibility / Interoperability | chuẩn W3C VC/DID, SD-JWT, OID4VP, DIF Universal Resolver |
| Usability | mobile-first, song ngữ EN/VI, biometric |
**Citation**: ISO/IEC 25010; OWASP [9][24]; NIST SP 800-63-3 [7].

## 2.2. Đề xuất kiến trúc hệ thống
**Tổng thể**: sơ đồ kiến trúc nền tảng (Flutter app + Verifier Portal → Spring backend → MySQL + Fabric) ở `project_context.md` §3; bảng phân chia trách nhiệm dữ liệu (cái gì on-chain / off-chain) cũng ở §3 (rất quan trọng — trích nguyên).
### 2.2.1. Lớp hạ tầng blockchain
**Dữ kiện**: Fabric 2.x; topology 1 Orderer + 2 Peers (Org1) + 1 CA; channel `mychannel`; consensus **Raft (etcdraft)**; chaincode viết bằng **Java** (class `IdentityLedger.java`, deploy tên `identity-ledger`); triển khai bằng Docker trên WSL; crypto material X.509 cho `User1@org1`. **Nguồn sâu**: §4.4, §16, `blockchain.md` §2.
### 2.2.2. Lớp ứng dụng
- **2.2.2.1. Giao tiếp app ↔ blockchain**: backend kết nối peer qua **Hyperledger Fabric Gateway SDK 1.7 (Java)** trên gRPC + TLS + định danh X.509; hai loại lời gọi `submitTransaction` (ghi) / `evaluateTransaction` (đọc). **Nguồn sâu**: §6.1, §5 (layer 4), `blockchain_mechanism.md` §2.
- **2.2.2.2. Backend**: Kotlin + **Spring Boot 4.0.5**, Kotlin 2.2.21, port 8080, base `/api/v1`, JWT HS256 24h, Spring Security. Cấu trúc 2 package song song `org.fabric.api.*` (Gateway layer) + `com.mpcorp.identity.*` (clean arch). **Nguồn sâu**: §4.1, §5.
- **2.2.2.3. Database**: **MySQL 8** (`identity_db`), JPA/Hibernate. Lưu PII + VC JSON đã ký. Thiết kế bảng chi tiết → Chương 3.5. **Nguồn sâu**: §3 (phân chia dữ liệu), §4.1.
- **2.2.2.4. Ứng dụng di động**: **Flutter 3.x (Dart)**, Clean Architecture, flutter_bloc, go_router, get_it, Dio+JWT interceptor; bảo mật: flutter_secure_storage, local_auth (biometric), ECDSA P-256 (pointycastle), Hive offline cache; song ngữ EN/VI. Chức năng theo tab Wallet/Verifier/Workplace/Profile. **Nguồn sâu**: §4.2.
- **2.2.2.5. Website định danh (Verifier Portal)**: **lý do** — Verifier là bên thứ ba (ngân hàng/nhà tuyển dụng/cơ quan) cần xác minh nhanh, **không nên buộc đăng nhập/cài app**; SPA độc lập paste VC/SD-JWT → verify ngay. Công nghệ: **Vite 6 + React 18 + TypeScript + Tailwind 3**; deploy `verify.michikuni.cloud`. **Nguồn sâu**: §4.3.

## 2.3. Xây dựng các quy trình nghiệp vụ
### 2.3.1. Quy trình cấp phát chứng chỉ số (VC issuance)
**Dữ kiện**: Admin/Chief duyệt → backend (Issuer) tạo VC, ký HMAC-SHA256, lưu MySQL, async anchor hash + DID lên Fabric; Holder tải VC về app. Các loại VC & thời điểm cấp: bảng §8. Luồng end-to-end: `blockchain.md` §5 (Luồng 1, Luồng 2 — Admin approve → RegisterDID).
### 2.3.2. Quy trình xác thực và phân quyền truy cập
**Dữ kiện**: đăng nhập JWT (HS256, 24h) + MFA TOTP (AAL2); phân quyền theo role (Admin/Chief/Manager/Employee) qua Spring Security; Verifier verify không cần login; OID4VP flow (request/submit/result + nonce/state chống replay 300s — §12.5.3).
### 2.3.3. Quy trình thu hồi danh tính
**Dữ kiện**: Chief terminate employee → backend gọi `UpdateStatusListEntry` set bit N=1 trên Fabric → VC chuyển REVOKED; DID có thể bị thu hồi (Luồng 3 ở `blockchain.md` §5). Cơ chế Status List 2021 chi tiết §10.
### 2.3.4. Mô hình đồng bộ blockchain bất đồng bộ
**Dữ kiện**: **fire-and-forget (`@Async`) + Transactional Outbox**. MySQL commit ngay (source of truth) → ghi Fabric bất đồng bộ; lỗi → lưu `fabric_outbox_events` (PENDING) → scheduler retry mỗi 5 phút, exponential backoff 30s→60s→120s→240s→480s → sau 5 lần fail = DEAD_LETTER. **Nguồn sâu**: §6.3, `blockchain.md` §4.4–4.5, §6.

## 2.4. Thiết kế giải pháp bảo mật và quyền riêng tư
### 2.4.1. Quản lý khoá mật mã
- Holder: keypair **ECDSA P-256** sinh trên thiết bị, private key lưu Secure Enclave/Keystore qua flutter_secure_storage, **không bao giờ rời máy**; biometric gate trước mỗi lần ký.
- Issuer: secret HMAC (`vc.secret`, `sd-jwt.secret`) qua env var. **Hạn chế đã ghi nhận** + roadmap migrate Ed25519/ECDSA: §12.5.4.
### 2.4.2. Mã hoá dữ liệu
- Truyền: TLS/HTTPS (gRPC TLS giữa backend↔peer; HTTPS REST). Lưu: secure storage mobile; JWT ký HS256. Hash toàn vẹn: SHA-256 (VC hash, notarization).
### 2.4.3. Bảo vệ quyền riêng tư
- **Selective Disclosure (SD-JWT)** §9; **data minimization**; **k-anonymity** của Status List (1 bitstring 131072 → Verifier không biết check VC nào, §10); **PII off-chain** (chỉ hash on-chain).
### 2.4.4. Phòng chống các loại tấn công
- Lấy nguyên **Threat Model STRIDE + DREAD** ở §12.5: bảng STRIDE theo component (§12.5.1), Top 7 attack scenarios + điểm DREAD + mitigation (§12.5.2), sequence chống VP replay (§12.5.3). Đối ứng OWASP Top 10 (§12.5.5).
### 2.4.5. Tuân thủ pháp lý và tiêu chuẩn
- Bảng compliance mapping §12.5.5: GDPR Art.17/20, OWASP A02/A03/A07, NIST SP 800-63-3 AAL2, W3C VC Integrity. **Citation**: [7][8][9][24].

## 2.5. Các cơ chế đặc trưng của ứng dụng
| Mục | Cơ chế | Nguồn sâu |
|---|---|---|
| 2.5.1 | Outbox Pattern & Dual-write → eventual consistency | §6.3, `blockchain.md` §4.4–4.6 |
| 2.5.2 | Selective Disclosure qua SD-JWT (băm claim + salt 128-bit, disclosure riêng) | §9 |
| 2.5.3 | Status List 2021 — revocation bảo toàn quyền riêng tư (bitstring 131072) | §10 |
| 2.5.4 | Trust Registry on-chain — `RegisterIssuer`/`RevokeIssuer`/`IsTrustedIssuer`/`ListTrustedIssuers` | chaincode; §7 Phase 1 |
| 2.5.5 | E-sign hợp đồng & non-repudiation qua chữ ký Holder (ECDSA) → `RecordSignature` anchor Fabric | §7, chaincode `RecordSignature`/`GetSignatures` |
| 2.5.6 | GDPR Art.17 (quyền lãng quên: xoá PII MySQL, hash on-chain không reverse) & Art.20 (export JSON) | §13, §15 (endpoint `/gdpr/export`) |

---

# CHƯƠNG 3 — PHÁT TRIỂN HỆ THỐNG

> Đây là chương **biểu đồ + code**. Mọi biểu đồ vẽ bằng app.diagrams.net (.drawio) và lưu Google Drive (theo yêu cầu khung). Context dưới mô tả **nội dung từng biểu đồ cần thể hiện** để vẽ đúng.

## 3.1. Biểu đồ Usecase
- **3.1.1. Tổng quát**: 4 actor (Admin/Issuer, Holder/Employee, Verifier, Chief/Manager) + Verifiable Data Registry; gom các use case chính (cấp VC, xác minh, thu hồi, e-sign, MFA, GDPR…).
- **3.1.2. Admin**: cấp các loại VC, issue SD-JWT, quản lý Trust Registry, dashboard KPI, audit log, company/directory.
- **3.1.3. Issuer** (trùng nhiều với Admin/Chief): cấp/ thu hồi credential, ký số, anchor Fabric.
- **3.1.4. Holder**: xem ví, build VP + selective disclosure, e-sign biometric, export/erase GDPR, quản lý session.
- **3.1.5. Verifier**: paste/scan VC-VP, verify chữ ký + Status List, xem trusted issuers (không login).
**Nguồn**: chức năng theo actor lấy từ §7, §8, §15.

## 3.2. Biểu đồ tuần tự (sequence)
- **3.2.1. Admin**: approve account → issue VC → `submitTransaction(RegisterDID/UpsertRecord)` → Outbox nếu lỗi. (`blockchain.md` §5 Luồng 2; `blockchain_mechanism.md` §3.2, §3.4).
- **3.2.2. Issuer**: tạo & ký VC (HMAC) → lưu MySQL → async anchor hash Fabric.
- **3.2.3. Holder**: build VP/SD-JWT → biometric ECDSA sign → tạo QR.
- **3.2.4. Verifier**: nhận VP → verify chữ ký → `evaluateTransaction` check Status List/Trust Registry → trả kết quả. (chống replay: nonce/state §12.5.3).

## 3.3. Biểu đồ hoạt động (activity)
- **3.3.1. Phát hành VC**: quyết định loại VC → tạo payload → ký → ghi MySQL → nhánh async Fabric (thành công / Outbox retry).
- **3.3.2. Xác minh VC**: parse VC → verify chữ ký → fetch Status List → check bit (ACTIVE/REVOKED) → check Trust Registry → kết luận hợp lệ/không.

## 3.4. Thiết kế hệ thống (kiến trúc 4 tầng)
Ánh xạ với kiến trúc 4 layer ở `project_context.md` §5 (lưu ý: §5 là 4 layer *backend*; còn 3.4 yêu cầu 4 tầng *toàn hệ thống* theo User/Gateway/Application/Blockchain — dùng sơ đồ §3 + §5 kết hợp):
- **3.4.1. Tầng 1 — User Layer**: Flutter app (Holder) + Verifier Portal (Verifier) + Admin console.
- **3.4.2. Tầng 2 — Gateway Layer**: API gateway/REST `/api/v1`, JWT auth, rate limit; (và Fabric Gateway gRPC ở phía blockchain).
- **3.4.3. Tầng 3 — Application Layer**: backend clean arch (presentation→usecase→infrastructure), VC Issuer/SD-JWT/Status List, MySQL.
- **3.4.4. Tầng 4 — Blockchain Layer**: Fabric peers/orderer/CA, chaincode `IdentityLedger`.

## 3.5. Thiết kế cơ sở dữ liệu
**Dữ kiện**: vẽ ERD MySQL `identity_db`. Các bảng cốt lõi cần có (suy từ tính năng): users/accounts, employees, credentials (VC JSON), did_documents (hoặc on-chain mirror), status_list, `fabric_outbox_events` (Outbox), contracts/signatures, mfa/totp, sessions/devices, company, directory, didcomm_messages, notarization_records, audit. **Cần kiểm tra entity thực tế** trong `fabric-spring-backend/src/.../infrastructures/persistence/` để vẽ đúng tên cột (đừng bịa — đọc code entity).

## 3.6. Lập trình module hệ thống
**Cần viết gì**: trích **các đoạn code module chính** + mô tả. Ứng viên tốt nhất (có giá trị học thuật):
- `IdentityLedger.java` (chaincode) — các hàm `UpsertRecord`, `RegisterDID`, `UpdateStatusListEntry`, `RegisterIssuer/IsTrustedIssuer`, `RecordSignature`, `GetRecordHistory`, `VerifyRecord`. Đường dẫn: [IdentityLedger.java](fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/IdentityLedger.java).
- `FabricGatewayConfig.kt` (gRPC+TLS), `IdentityLedgerService.kt` (submit/evaluate), `FabricOutboxService.kt` + `FabricRetryScheduler` (retry).
- VC Issuer (HMAC), SdJwtIssuer/Verifier, Status List builder, VpService + VpSessionStore.
**Nguồn sâu**: `blockchain_mechanism.md` (toàn bộ — đã trích code thực tế từng layer).

## 3.7. Tương tác với blockchain
**Cần viết gì**: user tạo dữ liệu → ghi MySQL + ghi Fabric như thế nào; vai trò peer/CA/orderer trong context này.
**Dữ kiện**:
- Hai loại giao dịch: `submitTransaction` (WRITE: UpsertRecord/RegisterDID/UpdateStatusListEntry — qua Endorser→Orderer→Committer, ~2–5s) vs `evaluateTransaction` (READ: GetRecord/GetHistory/VerifyRecord — 1 peer, ~ms). Bảng §6.1.
- Vai trò node trong luồng ghi: **Peer (endorser)** simulate + ký endorsement → **Orderer** sắp xếp & đóng block (Raft) → **Peer (committer)** validate + commit world state; **CA/MSP** cấp & xác thực định danh X.509 cho mọi giao dịch.
- Pattern dual-write + Outbox (§6.3). Bảng chaincode transaction §6.2.
**Nguồn sâu**: §6 toàn bộ; `blockchain_mechanism.md` §1.2, §2, §3; "Tóm tắt submit vs evaluate" cuối `blockchain_mechanism.md`.

---

# CHƯƠNG 4 — KẾT QUẢ HIỆN THỰC HOÁ VÀ ĐÁNH GIÁ THỰC NGHIỆM

## 4.1. Kết quả hiện thực hoá
**Cần viết gì**: hình ảnh thực nghiệm theo Demo Flow.
**Dữ kiện**: kịch bản 8 bước ở `project_context.md` §12 (Wallet → SD-JWT present → Verifier Portal verify → terminate→REVOKED → e-sign → sessions → GDPR export → audit log). Chụp screenshot từng bước, chú thích rõ thành phần nào (app/portal/ledger) đang hiển thị gì.

## 4.2. Đánh giá
**Cần viết gì**: đo tốc độ blockchain + đánh giá.
**Dữ kiện**: dùng **methodology + bảng template** ở `project_context.md` §12.6:
- Môi trường đo (ghi đầy đủ CPU/RAM/JVM/MySQL/Fabric batch) §12.6.1.
- Công cụ: **k6/JMeter** (REST API), **Hyperledger Caliper** (chaincode TPS endorsement+commit), integration test (mobile), Wireshark (payload).
- Bảng kết quả: REST API §12.6.2, chaincode §12.6.3, mobile §12.6.4, payload size §12.6.5.
- Discussion §12.6.6 (bottleneck Fabric submit ~2s do BatchTimeout vs HMAC sub-ms; Outbox backpressure; so sánh Indy AnonCreds ~200ms vs HMAC <30ms — đánh đổi với non-repudiation; ngưỡng scale Status List 131072).
- **⚠️ Phải chạy benchmark thật và điền cột "Đo được"** — không bịa. **Citation**: Sukhwani [18], Thakkar [19], Bhattacharya [20].

---

# KẾT LUẬN

## 1. Các kết quả đạt được
Lấy từ `project_context.md` §13 (Đạt được): Trust Triangle đầy đủ; 4 chuẩn W3C/IETF; Selective Disclosure + biometric; Outbox eventual consistency; GDPR Art.17/20; audit log on-chain non-repudiation; MFA/rate-limit/device-binding theo OWASP.

## 2. Hạn chế của đề tài
Lấy từ `project_context.md` §13 (Hạn chế) + memory: VC ký **HMAC-SHA256** (cần migrate ECDSA/Ed25519); **1 Org** duy nhất (cần đa Org cho cross-org trust); **OID4VP** chỉ `jwt_vp` HMAC, chưa `ldp_vp`/SD-JWT VC trong VP, **OID4VCI chưa làm**; Status List cố định 131072 (cần shard); `ddl-auto=update` (cần Flyway/Liquibase); chưa có ZKP đầy đủ (BBS+/AnonCreds); Trust Registry chưa có UI Admin (và trên prod đang trả `[]` do chưa đăng ký issuer); **DIDComm mới PoC** (plaintext MySQL, chưa mã hoá DIDComm v2). Mỗi hạn chế nên kèm **hướng phát triển**.

---

# TÀI LIỆU THAM KHẢO (chuẩn IEEE)

- **Danh mục đầy đủ 24 nguồn** đã chuẩn bị ở `project_context.md` §17 — copy trực tiếp, đánh số IEEE theo thứ tự trích dẫn trong báo cáo.
- Nhóm theo chủ đề để dễ trích: chuẩn W3C/IETF/DIF [1–5][10–11]; Fabric/NIST/GDPR/OWASP [6–9][23–24]; SSI frameworks [12–17]; performance & survey papers [18–22] (xem thêm §12.6.7 P1–P7).
- Mẫu đầu mục đã có trong khung: `[1] W3C, "Verifiable Credentials Data Model v2.0," W3C Recommendation, 2025.`

---

## Phụ lục A — Bản đồ tra cứu nhanh (heading khung ↔ section project_context.md)

| Mục báo cáo | Nguồn chính trong project_context.md / repo |
|---|---|
| 1.4 Khảo sát nền tảng | §1.5 (Related Work, bảng so sánh SSI) |
| 1.5 Tổng quan blockchain | (lý thuyết — phần 1.5 file này) |
| 1.6 Hyperledger Fabric | §11 + lý thuyết file này; consensus: configtx.yaml |
| 2.1.2 Phi chức năng | ISO 25010 ↔ §12.5, §6, §5 |
| 2.2 Kiến trúc | §3 (tổng thể + phân chia dữ liệu), §4, §5 |
| 2.3 Quy trình nghiệp vụ | §6, §8, blockchain.md §5 |
| 2.4 Bảo mật & privacy | §12.5 (STRIDE/DREAD), §9, §10 |
| 2.5 Cơ chế đặc trưng | §6.3, §9, §10, chaincode |
| 3.6 Module code | blockchain_mechanism.md, IdentityLedger.java |
| 3.7 Tương tác blockchain | §6, blockchain.md, blockchain_mechanism.md |
| 4.2 Đánh giá hiệu năng | §12.6 (methodology + bảng) |
| Kết luận | §13 |
| Tài liệu tham khảo | §17, §12.6.7 |

## Phụ lục B — Việc cần làm trước khi bảo vệ (không được bỏ)
1. **Chạy benchmark thật** (k6 + Caliper) → điền cột "Đo được" §12.6. Không bịa số.
2. **Đọc entity persistence thật** để vẽ ERD 3.5 đúng tên bảng/cột.
3. **Vẽ các .drawio** (3.1–3.5) và lưu Google Drive theo yêu cầu khung.
4. **Chụp screenshot** demo flow §12 cho mục 4.1.
5. Đối chiếu lại các hạn chế (HMAC, 1 Org, Trust Registry `[]`) vẫn đúng tại thời điểm bảo vệ.

---

*Tạo từ phân tích khung `Bao_cao_do_an_v8_hoan_thien_formatted.md` + `project_context.md` (cập nhật 2026-05-30) + xác minh code (etcdraft consensus, IdentityLedger.java). Khi project_context.md đổi, cập nhật lại các con trỏ §.*
