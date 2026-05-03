# Hướng dẫn sử dụng tính năng quét QR

Ứng dụng TrustID sử dụng QR Code để chia sẻ và xác minh Verifiable Credentials (VC) giữa **Employee (người giữ VC)** và **Verifier (người xác minh)**. Có hai tab liên quan: **Wallet** và **Verifier**.

---

## Tab Wallet — Phía Employee (người giữ VC)

### Chức năng 1: Xuất QR từ VC

Dùng để **cho Verifier quét** nhằm xác minh VC trực tiếp.

**Các bước:**
1. Vào tab **Wallet**
2. Mỗi thẻ credential (Employment, Salary Range, Promotion, Termination) có nút **Xuất QR**
3. Nhấn **Xuất QR** — popup hiện mã QR
4. Đưa màn hình cho Verifier quét bằng tab **Xác minh VC** của họ
5. Verifier sẽ thấy kết quả HỢP LỆ / KHÔNG HỢP LỆ + thông tin credential

> **Lưu ý:** QR này chứa toàn bộ nội dung VC (hoặc ID ngắn nếu VC có `id`). Bất kỳ ai quét đều thấy được tất cả thông tin trong VC. Nếu muốn **chọn lọc thông tin**, dùng chức năng Present VP.

---

### Chức năng 2: Present VP — Chủ động chọn field chia sẻ

Dùng để **chia sẻ có chọn lọc** thông tin Employment VC với Verifier (không phải toàn bộ VC).

**Các bước:**
1. Vào tab **Wallet**
2. Trong thẻ **Employment Credential**, nhấn **Present VP — tự chọn field chia sẻ**
3. Popup hiện danh sách **tất cả các trường** trong VC của bạn (ví dụ: department, position, employmentStatus, startDate...)
4. Tick chọn các trường muốn chia sẻ, bỏ tick các trường muốn giữ bí mật
5. Nhấn **Tạo VP QR**
6. Hệ thống tạo một VP session và gửi VP Token lên server
7. Verifier dùng tab **Yêu cầu VP** → nhấn **Kiểm tra kết quả** để thấy thông tin bạn đã chia sẻ

> **Lưu ý:** Luồng này là **Employee chủ động** — Employee quyết định chia sẻ gì và Verifier chờ kết quả.

---

### Chức năng 3: Quét VP Request QR

Dùng khi **Verifier yêu cầu** Employee cung cấp thông tin cụ thể qua QR.

**Các bước:**
1. Verifier tạo QR yêu cầu (xem phần Verifier bên dưới) và hiển thị trên màn hình của họ
2. Employee vào tab **Wallet** → thẻ **Employment Credential** → nhấn **Quét VP Request**
3. Camera mở lên — hướng vào QR trên màn hình Verifier
4. App tự động nhận diện QR, **hiển thị popup xác nhận** gồm:
   - Danh sách thông tin mà Verifier yêu cầu
   - Nút **Xác nhận chia sẻ** và nút **Từ chối**
5. Nhấn **Xác nhận chia sẻ** — VP Token được gửi lên server
6. Verifier nhấn **Kiểm tra kết quả** để thấy thông tin đã được chia sẻ

> **Quan trọng:** Employee phải **chủ động nhấn Xác nhận** mới gửi thông tin. Tuyệt đối không chia sẻ nếu không nhận ra QR Request từ nguồn tin cậy.

---

## Tab Verifier — Phía Verifier (người xác minh)

### Tab "Xác minh VC" — Quét QR của Employee

Dùng để xác minh tính hợp lệ của VC mà Employee đưa ra.

**Các bước:**
1. Vào tab **Verifier** → chọn tab **Xác minh VC**
2. Camera tự động mở
3. Hướng camera vào QR trên màn hình Employee (QR từ nút "Xuất QR")
4. Kết quả tự động hiển thị:
   - **HỢP LỆ** (xanh lá) + loại credential + thông tin subject
   - **KHÔNG HỢP LỆ** (đỏ) + lý do
5. Nhấn **Quét lại** để xác minh credential khác

**Chấp nhận 2 loại QR:**
- QR từ nút **Xuất QR** — chứa VC đầy đủ hoặc short-token (`vcid:xxx`)
- QR từ nút **Present VP** — chứa VP Token đã được Employee ký và submit

---

### Tab "Yêu cầu VP" — Tạo QR yêu cầu thông tin từ Employee

Dùng khi Verifier muốn **yêu cầu Employee cung cấp thông tin cụ thể** (không cần Employee chủ động).

**Các bước:**

**Bước 1 — Chọn thông tin muốn yêu cầu:**
- Tick chọn các trường cần: `employmentStatus`, `department`, `position`, `startDate`
- Có thể chọn một hoặc nhiều trường

**Bước 2 — Tạo QR:**
- Nhấn **Bước 2 — Tạo QR cho Employee quét**
- Server tạo một VP Request session với `state` và `nonce`
- QR hiển thị trên màn hình

**Bước 3 — Cho Employee quét:**
- Employee dùng chức năng **Quét VP Request** trong tab Wallet
- Employee quét QR, xem thông tin được yêu cầu, rồi nhấn **Xác nhận**

**Bước 4 — Employee đã quét và xác nhận chia sẻ**

**Bước 5 — Kiểm tra kết quả:**
- Nhấn **Bước 5 — Kiểm tra kết quả**
- Popup hiển thị thông tin Employee đã chia sẻ (các trường đã được chọn)
- Nếu chưa có kết quả, trạng thái hiện "Chờ Employee quét QR và gửi VP..."

---

## Tóm tắt so sánh các luồng QR

| Luồng | Ai khởi tạo | Phương thức | Thông tin chia sẻ |
|-------|------------|-------------|-------------------|
| Xuất QR VC | Employee | Wallet → Xuất QR | Toàn bộ VC |
| Present VP | Employee | Wallet → Present VP | Fields do Employee chọn |
| VP Request | Verifier | Verifier → Yêu cầu VP | Fields do Verifier yêu cầu (Employee xác nhận) |

---

## Lưu ý bảo mật

- **Không quét QR từ nguồn không rõ ràng** — VP Request QR luôn cần Employee xác nhận trước khi gửi thông tin
- QR VC chứa thông tin công việc — chỉ chia sẻ với Verifier đáng tin cậy
- VP Token có `nonce` để chống replay attack — mỗi session chỉ dùng một lần
- SalaryRangeVC và TerminationVC chứa thông tin nhạy cảm — cân nhắc kỹ trước khi Xuất QR
