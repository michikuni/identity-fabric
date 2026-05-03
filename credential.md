# Giải thích các Credential trong tab Wallet

Tab **Wallet** trong ứng dụng TrustID lưu trữ và hiển thị các **Verifiable Credentials (VC)** — chứng chỉ kỹ thuật số được ký bởi tổ chức, tuân theo chuẩn W3C Verifiable Credentials. Mỗi VC được lưu an toàn trong Secure Storage của thiết bị và gắn với **DID (Decentralized Identifier)** của nhân viên.

---

## 1. DID Card — Decentralized Identifier

**Định nghĩa:** DID là định danh phi tập trung duy nhất của nhân viên trong hệ thống blockchain Hyperledger Fabric.

**Định dạng:** `did:fabric:trustid:<employeeId>`
Ví dụ: `did:fabric:trustid:42`

**Các thông tin hiển thị:**

| Trường | Ý nghĩa |
|--------|---------|
| DID | Định danh duy nhất, có thể sao chép |
| Controller | Tổ chức kiểm soát DID (thường là `did:fabric:trustid:org1`) |
| Cấp lúc | Thời điểm DID được đăng ký lên blockchain |
| Status | `ACTIVE` = đang hoạt động; trạng thái khác = bị khóa/thu hồi |

**Khi nào có DID?**
DID được tạo tự động khi Admin duyệt tài khoản nhân viên. Trước đó, Wallet hiển thị trạng thái "Chờ Admin duyệt".

---

## 2. Employment Credential — Chứng chỉ Việc làm

**Định nghĩa:** VC xác nhận tình trạng công việc hiện tại của nhân viên trong tổ chức.

**Màu viền:** Xanh dương (primary)
**Badge:** `VALID` (xanh lá) hoặc `EXPIRED` (đỏ)

**Thông tin trong credentialSubject:**

| Trường | Ý nghĩa |
|--------|---------|
| `department` | Phòng ban hiện tại |
| `position` | Chức vụ hiện tại |
| `employmentStatus` | Trạng thái: `ACTIVE`, `RESIGNED`, `TERMINATED` |
| `startDate` | Ngày bắt đầu làm việc |

**Các nút hành động:**
- **Xuất QR** — Tạo mã QR chứa VC để Verifier quét và xác minh trực tiếp
- **Quét VP Request** — Quét QR yêu cầu từ Verifier để gửi VP (xem mục VP)
- **Present VP — tự chọn field chia sẻ** — Chủ động chọn fields muốn tiết lộ và gửi cho Verifier

**Khi nào có?**
Được cấp tự động khi Admin duyệt tài khoản, cùng lúc với DID.

---

## 3. Salary Range Credential — Chứng chỉ Dải Lương

**Định nghĩa:** VC xác nhận dải lương (salary band) của nhân viên, không tiết lộ con số cụ thể mà chỉ cho biết nhân viên thuộc band lương nào.

**Màu viền:** Cam/vàng (accent)
**Badge:** `VALID` hoặc `EXPIRED`

**Thông tin trong credentialSubject:**

| Trường | Ý nghĩa |
|--------|---------|
| `salaryBand` | Dải lương, ví dụ: `BAND_A`, `50M-70M`, `SENIOR` |
| `currency` | Đơn vị tiền tệ: `VND`, `USD` |
| `position` | Chức vụ tương ứng với dải lương |

**Các nút hành động:**
- **Xuất QR để Verifier quét** — Hiển thị QR để đối tác hoặc nhà tuyển dụng xác minh dải lương

**Khi nào có?**
Được phát hành bởi Admin thông qua nút **Issue Salary VC** (trong màn Admin Dashboard hoặc trong menu quản lý nhân sự ở ChiefScreen). Yêu cầu nhân viên phải được gán thông tin Payroll trước.

---

## 4. Promotion Credential — Chứng chỉ Thăng chức

**Định nghĩa:** VC ghi lại sự kiện thăng chức hoặc thay đổi chức vụ của nhân viên.

**Màu viền:** Xanh dương nhạt (info)
**Badge:** `PROMOTED`

**Thông tin trong credentialSubject:**

| Trường | Ý nghĩa |
|--------|---------|
| `department` | Phòng ban tại thời điểm thăng chức |
| `oldPosition` | Chức vụ trước khi thay đổi |
| `newPosition` | Chức vụ mới sau khi thăng chức |
| Ngày thăng | Lấy từ `issuanceDate` — ngày cấp VC |

**Các nút hành động:**
- **Xuất QR để Verifier quét** — Dùng để chứng minh lịch sử thăng tiến với đối tác/nhà tuyển dụng

**Khi nào có?**
Tự động được phát hành khi Giám đốc (Chief) đổi chức vụ cho nhân viên qua màn **Quản lý nhân sự** và nhập "Chức vụ mới". Nếu không nhập chức vụ mới, PromotionVC sẽ không được tạo.

---

## 5. Termination Credential — Chứng chỉ Chấm dứt HĐ

**Định nghĩa:** VC ghi lại sự kiện chấm dứt hợp đồng lao động của nhân viên.

**Màu viền:** Đỏ (error)
**Badge:** `TERMINATED`

**Thông tin trong credentialSubject:**

| Trường | Ý nghĩa |
|--------|---------|
| `department` | Phòng ban tại thời điểm nghỉ việc |
| `position` | Chức vụ cuối cùng |
| `terminationReason` | Lý do chấm dứt hợp đồng |
| Ngày chấm dứt | Lấy từ `issuanceDate` |

**Các nút hành động:**
- **Xuất QR để Verifier quét** — Dùng để xác minh trạng thái đã nghỉ việc nếu cần

**Khi nào có?**
Được phát hành khi Giám đốc thực hiện "Chấm dứt HĐ" từ màn **Quản lý nhân sự**.

---

## 6. Public Key (JWK) — Khóa công khai

**Định nghĩa:** Khóa công khai ECDSA P-256 của nhân viên, dùng để Verifier xác minh chữ ký số trong VC/VP.

**Thông tin hiển thị:**

| Trường | Ý nghĩa |
|--------|---------|
| `kty` | Loại key: `EC` (Elliptic Curve) |
| `crv` | Đường cong: `P-256` |
| `x` | Tọa độ X của điểm trên đường cong (base64url) |
| `y` | Tọa độ Y của điểm trên đường cong (base64url) |

**Nút hành động:** Sao chép JWK JSON để chia sẻ với bên cần xác minh thủ công.

---

## Luồng hoạt động tổng quan

```
Nhân viên đăng ký → Onboarding (phòng ban + chức vụ)
     ↓
Keypair ECDSA được tạo tự động → Public Key JWK hiển thị
     ↓
Admin duyệt tài khoản
     ↓
DID đăng ký lên Hyperledger Fabric Blockchain
Employment VC được phát hành bởi hệ thống
     ↓
Nhân viên có thể:
  - Xuất QR VC → Verifier quét xác minh trực tiếp
  - Present VP → Chủ động chia sẻ thông tin chọn lọc
  - Quét VP Request → Đáp lại yêu cầu của Verifier
     ↓
Admin/Chief có thể phát hành thêm:
  - SalaryRangeVC (qua Issue Salary VC)
  - PromotionVC (khi đổi chức vụ)
  - TerminationVC (khi chấm dứt HĐ)
```

---

## So sánh VC và VP

|  | Verifiable Credential (VC) | Verifiable Presentation (VP) |
|---|---|---|
| Ai tạo | Tổ chức (issuer) | Nhân viên (holder) |
| Mục đích | Chứng chỉ gốc do tổ chức cấp | Gói thông tin chọn lọc để chia sẻ |
| Nội dung | Toàn bộ thông tin | Chỉ các fields được chọn (selective disclosure) |
| Dùng khi | Verifier quét QR VC trực tiếp | Verifier yêu cầu VP qua QR Request |
