# TrustID v2 — Identity System Chuẩn

Dựa trên bản đánh giá học thuật, **TrustID hiện tại** là một *Blockchain-Anchored HRMS* nhưng chỉ đạt **31% tiêu chí identity chuẩn**.  
Tài liệu này mô tả hướng nâng cấp thành một **identity system hoàn chỉnh**.

---

## 1. Kiến trúc tổng thể
┌─────────────────────────────────────────────────────┐
│ Trust Triangle │
│ │
│ [Issuer: Company/Org1] ←→ [Verifiable Data │
│ signs VCs Registry on Fabric] │
│ ↓ ↑ │
│ [Holder: Employee │ │
│ Flutter Wallet] ──── present ──→ [Verifier: │
│ giữ DID + VCs Bank/Gov/Employer│
└─────────────────────────────────────────────────────

---

## 2. Thành phần cần bổ sung

### 2.1 DID Layer (Ưu tiên cao nhất)

Mỗi nhân viên có một DID:
did:fabric:trustid-channel:<employeeCode>

**DID Document:**
- Chứa public key của nhân viên
- Lưu trên chaincode (Hyperledger Fabric)

**Lưu trữ:**
- Private key → Flutter Secure Storage
- Database `employee` bổ sung:
  - `did`
  - `public_key`

**Ý nghĩa:**
- Là nền tảng của hệ identity
- Không có DID → không có VC, không có selective disclosure

---

### 2.2 Verifiable Credentials Wallet

Ứng dụng Flutter cần thêm module **Wallet**.

| Loại VC           | Thời điểm cấp        | Nội dung |
|------------------|----------------------|----------|
| EmploymentVC     | Khi Admin duyệt      | department, position, startDate |
| SalaryRangeVC    | Hàng năm             | salary band |
| PromotionVC      | Khi thăng chức       | oldPosition → newPosition, date |
| TerminationVC    | Khi nghỉ việc        | endDate, reason |

**Đặc điểm:**
- VC được ký bởi Org1
- Lưu trong app của nhân viên
- Có thể xuất:
  - JWT
  - QR Code

---

### 2.3 Selective Disclosure & Presentation

Ví dụ luồng xác minh:
Nhân viên → chọn: "Chứng minh đang đi làm,
KHÔNG tiết lộ lương/phòng ban"

→ App tạo Verifiable Presentation (VP)
gồm:

isEmployed = true
employerDID
validUntil

→ Verifier kiểm tra chữ ký Org1 trên Fabric

---

## 3. Lộ trình triển khai

### Giai đoạn 1 (Tháng 1–2)
- Cấp DID khi duyệt tài khoản
- Sinh keypair cho nhân viên

---

### Giai đoạn 2 (Tháng 3–4)
- Tự động cấp EmploymentVC
- Hỗ trợ QR verify

---

### Giai đoạn 3 (Tháng 5–6)
- Revocation khi nghỉ việc
- Cập nhật Status List trên Fabric

---

### Giai đoạn 4 (Tháng 7+)
- Triển khai OIDC4VC endpoint
- Cho phép bên thứ ba verify

---

## 4. Điểm mạnh

- Giữ nguyên audit trail trên Fabric
- Không phá vỡ kiến trúc hiện tại
- DID/VC là lớp bổ sung, không thay thế MySQL
- Flutter wallet dễ triển khai (dart_ssi hoặc custom)
- Đáp ứng:
  - W3C DID
  - NIST SP 800-63

---

## 5. Hướng phát triển tiếp

Có thể tập trung vào:

- DID Layer (chaincode + database schema)
- VC Wallet (Flutter implementation)
- Flow verify end-to-end

---
