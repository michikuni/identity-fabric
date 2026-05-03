# Hướng dẫn sử dụng nút Issue Salary VC

## Salary Range VC là gì?

**Salary Range VC (SalaryRangeVC)** là một Verifiable Credential xác nhận **dải lương** của nhân viên. Thay vì tiết lộ con số lương cụ thể, VC chỉ cho biết nhân viên thuộc **band lương nào** — giúp bảo mật thông tin trong khi vẫn có thể chứng minh mức thu nhập với bên thứ ba (ngân hàng, nhà tuyển dụng mới, đối tác).

---

## Điều kiện để Issue

Trước khi phát hành SalaryRangeVC cho một nhân viên, **bắt buộc** phải:

1. Nhân viên đã được **Admin duyệt tài khoản** (có DID + Employment VC)
2. Nhân viên đã được **gán thông tin Payroll** (lương cơ bản, dải lương, loại tiền tệ)

Nếu nhân viên chưa có Payroll, hệ thống sẽ báo lỗi khi Issue.

---

## Cách thực hiện Issue Salary VC

### Cách 1: Từ màn Admin Dashboard

1. Vào tab **Dashboard** (màn Admin)
2. Nhấn ô **Issue Salary VC** trong phần Quick Links
3. Popup hiện ra — nhập **Employee ID** (số ID nhân viên)
4. Nhấn **Issue VC**
5. Thông báo thành công/lỗi sẽ hiển thị

> Employee ID có thể tìm trong màn **Quản lý nhân sự** — số hiển thị dưới tên nhân viên.

---

### Cách 2: Từ màn Quản lý nhân sự (Chief/Admin)

1. Vào tab **Nhân sự** (màn Chief hoặc Admin)
2. Tìm nhân viên muốn issue
3. Nhấn nút **⋮** (ba chấm) ở góc phải thẻ nhân viên
4. Chọn **Issue Salary VC** trong menu popup
5. Xác nhận trong dialog — nhấn **Issue VC**
6. Hệ thống tự động gọi API và phát hành VC cho nhân viên

---

## Luồng kỹ thuật

```
Admin/Chief nhấn "Issue Salary VC"
         ↓
Gọi API: PUT /admin/employees/{id}/issue-salary-vc
         ↓
Backend đọc thông tin Payroll của nhân viên
  - baseSalary / totalIncome / currency
  - Tính toán salaryBand tương ứng
         ↓
Backend tạo SalaryRangeVC:
  {
    "type": ["VerifiableCredential", "SalaryRangeCredential"],
    "credentialSubject": {
      "salaryBand": "BAND_B",   // dải lương (không phải số tiền)
      "currency": "VND",
      "position": "Senior Engineer"
    }
  }
         ↓
VC được ký và lưu lên Hyperledger Fabric Blockchain
         ↓
Nhân viên mở tab Wallet → Salary Range Credential xuất hiện
```

---

## Thông tin trong SalaryRangeVC

| Trường | Ý nghĩa | Ví dụ |
|--------|---------|-------|
| `salaryBand` | Dải lương được mã hóa (không phải số tiền thực) | `BAND_A`, `BAND_B`, `50M-70M` |
| `currency` | Đơn vị tiền tệ | `VND`, `USD` |
| `position` | Chức vụ tương ứng với dải lương | `Senior Engineer` |
| `issuanceDate` | Ngày phát hành | `2025-01-15` |
| `expirationDate` | Ngày hết hạn (thường 1 năm) | `2026-01-15` |
| `issuer` | DID của tổ chức phát hành | `did:fabric:trustid:org1` |

---

## Nhân viên sử dụng SalaryRangeVC như thế nào?

Sau khi được issue, nhân viên vào tab **Wallet** và thấy thẻ **Salary Range Credential**:

1. **Xuất QR** — Tạo mã QR chứa VC
2. Đưa QR cho Verifier (ngân hàng, đơn vị đối tác) quét
3. Verifier dùng tab **Xác minh VC** để quét và xác nhận tính hợp lệ
4. Verifier thấy được `salaryBand` và `currency` — biết nhân viên đủ điều kiện tài chính mà không cần biết con số lương cụ thể

---

## Lưu ý

- SalaryRangeVC **không thể** được phát hành nhiều lần — lần issue sau sẽ ghi đè lần trước
- Khi nhân viên **nghỉ việc** (TerminationVC được tạo), SalaryRangeVC vẫn tồn tại trong Wallet nhưng có thể đã hết hạn
- Admin và Chief đều có quyền issue SalaryRangeVC
- Nếu cần cập nhật dải lương, phải cập nhật Payroll trước rồi issue lại VC
