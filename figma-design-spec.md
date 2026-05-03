# TrustID — Figma Design Specification
> Tài liệu này được generate từ Flutter source code.  
> Import vào Figma bằng cách tạo Frame theo từng section.

---

## 1. DESIGN TOKENS

### 1.1 Color Palette

| Token Name | Hex | Dùng cho |
|---|---|---|
| `primary` | `#1A237E` | AppBar, button, icon nền |
| `primaryLight` | `#3949AB` | Gradient end, hover |
| `primaryDark` | `#0D1257` | Pressed state |
| `accent` | `#F59E0B` | Payroll card, badge |
| `accentLight` | `#FBFF24` | Accent gradient end |
| `background` | `#F7F9FC` | Nền toàn màn hình |
| `surface` | `#FFFFFF` | Card, dialog |
| `surfaceVariant` | `#F1F5F9` | Input nền, chip |
| `textPrimary` | `#0F172A` | Tiêu đề, nội dung chính |
| `textSecondary` | `#64748B` | Label phụ, mô tả |
| `textHint` | `#CBD5E1` | Placeholder |
| `border` | `#E2E8F0` | Viền card, input |
| `divider` | `#E2E8F0` | Đường kẻ |
| `inactive` | `#94A3B8` | Icon disabled, placeholder icon |
| `disabled` | `#E2E8F0` | Nền disabled |
| `success` | `#059669` | Trạng thái active/approved |
| `successLight` | `#ECFDF5` | Nền badge success |
| `error` | `#DC2626` | Lỗi, check out |
| `errorLight` | `#FEF2F2` | Nền badge error |
| `warning` | `#F59E0B` | Đang chờ duyệt |
| `warningLight` | `#FFFBEB` | Nền badge warning |
| `info` | `#0EA5E9` | Thông tin, lịch sử |
| `infoLight` | `#F0F9FF` | Nền badge info |

### Gradients

| Name | From → To | Direction |
|---|---|---|
| `primaryGradient` | `#1A237E` → `#3949AB` | TopLeft → BottomRight |
| `accentGradient` | `#F59E0B` → `#FBFF24` | TopLeft → BottomRight |
| `cardGradient` | `#1A237E` → `#283593` | TopLeft → BottomRight |

---

### 1.2 Typography

**Font Family:** Inter (Google Fonts)

| Style | Size | Weight | Line Height | Dùng cho |
|---|---|---|---|---|
| Display Large | 32px | 700 | 1.2 | — |
| Display Medium | 28px | 700 | — | Tiêu đề màn hình (Welcome Back) |
| Headline Large | 24px | 700 | — | AppBar title (Chấm Công) |
| Headline Medium | 20px | 600 | — | Tiêu đề section |
| Title Large | 16px | 600 | — | Card header |
| Title Medium | 15px | 500 | — | Sub-header |
| Body Large | 15px | 400 | — | Nội dung chính |
| Body Medium | 14px | 400 | — | Mô tả |
| Body Small | 13px | 400 | — | Label phụ |
| Label Large | 14px | 600 | — | Button text |
| Label Medium | 13px | 600 | — | Quick action label |
| Label Small | 11px | 500 | — | Badge text, caption |
| Clock | 42px | 300 | — | Đồng hồ chấm công (letter-spacing: 2) |

---

### 1.3 Spacing & Radius

| Token | Value |
|---|---|
| Screen Horizontal Padding | 20–24px |
| Section Gap | 20px |
| Card Padding | 16–24px |
| Component Gap | 12–16px |
| Border Radius — Card lớn | 20px |
| Border Radius — Card | 16px |
| Border Radius — Button | 12–14px |
| Border Radius — Input | 12px |
| Border Radius — Icon Chip | 8–10px |
| Border Radius — Badge | 20px (full) |
| Button Height (primary) | 52px |
| Button Height (small) | 40px |
| AppBar Height (expanded) | 140–200px |

### Shadows

| Name | Blur | Offset Y | Color |
|---|---|---|---|
| Card Shadow | 12px | 4px | `#1A237E` 5% alpha |
| Primary Card Shadow | 20px | 8px | `#1A237E` 30% alpha |

---

## 2. COMPONENT LIBRARY

### 2.1 PrimaryButton

```
Frame: full width × 52px
Background: #1A237E (solid) hoặc transparent + border #1A237E (outlined)
Border Radius: 14px
Label: 15px / 700 / #FFFFFF
Icon (optional): 20px, bên trái label
Loading: CircularProgressIndicator 18×18px trắng, stroke 2px

States:
  • Default: bg #1A237E
  • Loading: bg #1A237E + spinner thay icon
  • Disabled: bg #E2E8F0, label #94A3B8
```

---

### 2.2 AppInput

```
Height: ~52px
Border: 1px #E2E8F0, radius 12px
Background: #FFFFFF
Padding horizontal: 16px
Label: 13px / 500 / #64748B — phía trên input, gap 6px
Hint text: #CBD5E1
Prefix icon: 20px #94A3B8 — trái
Suffix icon (password): 20px #94A3B8 — phải (eye toggle)

States:
  • Default: border #E2E8F0
  • Focused: border #1A237E
  • Error: border #DC2626
```

---

### 2.3 AppCard

```
Background: #FFFFFF
Border: 1px #E2E8F0
Border Radius: 16px
Padding: 16px
Shadow: blur 12px, offset (0, 4), color #1A237E 5%
```

### 2.4 GradientCard

```
Background: LinearGradient (#1A237E → #283593), TopLeft → BottomRight
Border Radius: 16px
Padding: 16–20px
Shadow: blur 20px, offset (0, 8), color #1A237E 30%
Text: #FFFFFF
```

---

### 2.5 StatusBadge

```
Padding: 6px horizontal × 4px vertical
Border Radius: 20px (pill)
Font: 11px / 600

Variants:
  success  → bg #ECFDF5, text #059669
  warning  → bg #FFFBEB, text #F59E0B
  error    → bg #FEF2F2, text #DC2626
  info     → bg #F0F9FF, text #0EA5E9
  neutral  → bg #F1F5F9, text #64748B
```

---

### 2.6 Language Toggle Button

```
Padding: 10px horizontal × 5–6px vertical
Background: rgba(255,255,255, 0.2)
Border Radius: 20px (pill)
Content: Flag emoji 13–14px + text 11–12px / 700 / #FFFFFF
```

---

## 3. SCREENS

---

## Screen 1 — Sign In

**Frame size:** 390 × 844 (iPhone 14)  
**Background:** `#F7F9FC`

### Layout (top → bottom)

#### Section A — Header (Gradient Block)
```
Width: 390px
Padding: 48px top, 28px horizontal, 40px bottom
Background: LinearGradient #1A237E → #3949AB (TopLeft → BottomRight)
Border Radius Bottom: 32px

Nội dung (top → bottom):
  Row:
    ├── Icon Container (44×44px)
    │     bg: rgba(255,255,255,0.15), radius 14px
    │     Icon: verified_user — 28px — #FFFFFF
    └── Language Toggle (phải) → xem component 2.6

  Gap: 20px

  Text "Chào mừng trở lại" — 28px / 700 / #FFFFFF / lineHeight 1.2
  Gap: 6px
  Text "Đăng nhập vào hệ thống..." — 14px / 400 / rgba(255,255,255,0.75)
```

#### Section B — Form Area
```
Padding: 32px top, 24px horizontal, 24px bottom

[AppInput] Email
  Label: "Email"
  Hint: "your@email.com"
  Prefix: mail_outline icon #94A3B8

Gap: 16px

[AppInput] Password
  Label: "Mật khẩu"
  Hint: "••••••••"
  Prefix: lock_outline icon #94A3B8
  Suffix: visibility_off icon (toggle)

Gap: 8px

[TextButton] "Quên mật khẩu?" — right aligned — 14px / #1A237E

Gap: 8px

[PrimaryButton] "Đăng nhập" — full width 52px

Gap: 32px

Row center:
  Text "Chưa có tài khoản?" — 14px #64748B
  [TextButton] "Đăng ký" — 14px / 500 / #1A237E

Gap: 16px

Text "v1.0.0" — 12px / #CBD5E1 — center
```

---

## Screen 2 — Home

**Frame size:** 390 × 844  
**Background:** `#F7F9FC`

### Layout

#### AppBar (SliverAppBar expanded 200px)
```
Background: LinearGradient #1A237E → #3949AB
Padding: 60px top, 24px horizontal, 20px bottom

Row:
  ├── Avatar Circle (48×48px)
  │     bg: rgba(255,255,255,0.2), shape circle
  │     Icon: person_outline — 26px — #FFFFFF
  ├── Gap: 12px
  ├── Column (expand):
  │     Text "Xin chào," — 13px / rgba(255,255,255,0.8)
  │     Text "user@email.com" — 17px / 700 / #FFFFFF (ellipsis)
  ├── Language Toggle (xem 2.6)
  ├── Gap: 8px
  └── IconButton logout_rounded — 22px — #FFFFFF

Gap: 12px
Text "Quản lý danh tính số..." — 13px / rgba(255,255,255,0.75)
```

#### Content (padding 16px all sides, gap 16px)

**Block 1 — GradientCard (Employee Summary)**
```
GradientCard (xem component 2.4)

Row:
  ├── Column (expand):
  │     Text [Position] — 18px / 700 / #FFFFFF
  │     Gap: 4px
  │     Text [Department] — 14px / rgba(255,255,255,0.75)
  └── StatusBadge

Gap: 20px

Row:
  ├── StatItem (Working Type)
  │     Icon: work_outline — 16px / rgba(255,255,255,0.7)
  │     Gap: 6px
  │     Column:
  │       Text label — 11px / rgba(255,255,255,0.6)
  │       Text value — 13px / 600 / #FFFFFF
  └── StatItem (Role) — tương tự
```

**Block 2 — Quick Access Grid**
```
Label "Truy cập nhanh" — titleLarge

GridView 2 cột:
  mainAxisSpacing: 12px
  crossAxisSpacing: 12px
  childAspectRatio: 1.6

  Mỗi ô (AppCard, padding 14px):
    Row:
      ├── Icon Container (36×36px)
      │     padding: 8px, radius 10px
      │     bg: color 12% alpha
      │     Icon: 20px, color
      └── Gap 10px + Label 13px / 600 / #0F172A

  Ô 1: person_outlined "Hồ sơ" — color: #0EA5E9
  Ô 2: description_outlined "Hợp đồng" — color: #059669
  Ô 3: payments_outlined "Lương" — color: #F59E0B
  Ô 4 (CHIEF only): how_to_reg "Duyệt TK" — color: #F59E0B
```

**Block 3 — Info Card (AppCard)**
```
Row header:
  Icon Container (30×30px) bg: #1A237E 8%, radius 8px
    Icon: info_outline — 16px / #1A237E
  Gap 10px
  Text "Nhân viên" — titleMedium

Gap: 12px
Divider: #E2E8F0

Detail Rows (each):
  Padding vertical: 10px
  Row:
    Flex(4) Label — 13px / #64748B
    Flex(6) Value — 13px / 500 / #0F172A (right align)
  Divider (trừ hàng cuối)

  Rows: Phòng ban / Vị trí / Loại hình / Ngày vào làm / Ghi chú
```

---

## Screen 3 — Attendance (Chấm Công)

**Frame size:** 390 × 844  
**Background:** `#F7F9FC`

### Layout

#### AppBar (SliverAppBar expanded 140px)
```
Background: LinearGradient #1A237E → #3949AB
Padding: 60px top, 24px horizontal, 16px bottom

Column (bottom-aligned):
  Text "Chấm Công" — 24px / 700 / #FFFFFF
  Text "Thứ 5, 1/5/2026" — 13px / rgba(255,255,255,0.75)
```

#### Content (padding 20px all, gap 20px)

**Block 1 — Check Card (GradientCard)**
```
Background: #1A237E → #3949AB
Border Radius: 20px
Padding: 24px
Shadow: blur 20px, offset (0,8), #1A237E 30%

Column center:
  Text "08:32:15" (clock) — 42px / 300 / #FFFFFF / letterSpacing 2
  Gap: 4px
  Text "Thứ 5, 1/5/2026" — 13px / rgba(255,255,255,0.75)
  Gap: 24px

  Trạng thái A — Chưa check in:
    [Button] "Check In"
      bg: #FFFFFF, text: #1A237E, icon: login_rounded
      padding vertical: 14px, radius 12px, full width

  Trạng thái B — Đã check in, chưa check out:
    [Button] "Check Out"
      bg: rgba(255,255,255,0.2), text: #FFFFFF, icon: logout_rounded
      border: 1px rgba(255,255,255,0.3)

  Trạng thái C — Đã hoàn thành:
    Container pill:
      bg: rgba(255,255,255,0.15)
      border: 1px rgba(255,255,255,0.3)
      radius: 12px
      padding: 12px×20px
      Row: check_circle icon #FFFFFF + Text "Đã hoàn thành hôm nay" 14px/600/#FFFFFF
```

**Block 2 — Today Info Card**
```
AppCard (bg #FFFFFF, border #E2E8F0, radius 16px, padding 16px)

Text "Hôm nay" — titleMedium
Gap: 12px

Row (gap 12px):
  ├── TimeCell "Giờ vào" (50%)
  └── TimeCell "Giờ ra" (50%)

TimeCell structure:
  bg: color 7% alpha, radius 10px, padding 12px
  Row:
    Icon 18px (color)
    Gap 8px
    Column:
      Text label — 11px / #64748B
      Text time — 15px / 700 / color (hoặc #94A3B8 nếu chưa có)

  "Giờ vào": icon login_rounded, color #059669
  "Giờ ra": icon logout_rounded, color #DC2626
```

**Block 3 — Quick Actions**
```
Row (gap 12px):
  ├── QuickBtn "Lịch sử" (50%)
  └── QuickBtn "Bảng công" (50%)

QuickBtn structure:
  AppCard, padding 16px, radius 14px
  Row:
    Icon Container (34×34px)
      padding 8px, radius 8px
      bg: color 10% alpha
      Icon: 18px / color
    Gap 10px
    Text label — 13px / 600 / #0F172A

  "Lịch sử": icon calendar_month, color #0EA5E9
  "Bảng công": icon table_chart, color #F59E0B
```

---

## Screen 4 — Profile

**Frame size:** 390 × 844  
**Background:** `#F7F9FC`

### Layout

#### AppBar
```
Standard height 56px
Background: #1A237E
Title: "Hồ sơ cá nhân" — 18px / 600 / #FFFFFF
Back button: arrow_back #FFFFFF
```

#### Content (ScrollView, padding 16px, gap 16px)

**Header Card (GradientCard)**
```
Padding: 24px
Column center items:
  Circle Avatar (72×72px)
    bg: rgba(255,255,255,0.2)
    Text initial — 28px / 700 / #FFFFFF
  Gap: 12px
  Text [Full Name] — 18px / 700 / #FFFFFF
  Gap: 4px
  Text [email] — 14px / rgba(255,255,255,0.8)
```

**Info Cards (AppCard each)**

Mỗi card:
```
SectionHeader row:
  Icon Container (30×30px) bg: #1A237E 8%, radius 8px
  Text title — titleMedium
Gap: 12px
Divider
InfoRows...
```

Cards:
1. **Thông tin cá nhân** — Họ tên / Giới tính / Ngày sinh / Điện thoại / Email
2. **Giấy tờ tùy thân** — Loại / Số / Nơi cấp
3. **Người liên hệ khẩn cấp** — Tên / Điện thoại / Quan hệ
4. **Địa chỉ** — Thường trú / Tạm trú
5. **Học vấn & kinh nghiệm** — Trình độ / Chuyên ngành / Kinh nghiệm / Kỹ năng (Chip Wrap) / Chứng chỉ

InfoRow structure:
```
Padding vertical 10px
Row:
  Flex(4) label — 13px / #64748B
  Flex(6) value — 13px / 500 / #0F172A — right align
Divider (trừ cuối)
```

---

## Screen 5 — Payroll (Lương)

**Frame size:** 390 × 844  
**Background:** `#F7F9FC`

### Layout

#### AppBar
```
Standard, bg #1A237E
Title: "Bảng lương" — 18px / 600 / #FFFFFF
```

#### Content (padding 16px, gap 16px)

**Header Card (Accent GradientCard)**
```
Background: #F59E0B → #FBFF24 (accentGradient)
Padding: 24px
Border Radius: 16px

Text "Tổng thu nhập" — 13px / rgba(255,255,255,0.8)
Gap: 8px
Text "12,000,000 đ" — 28px / 700 / #FFFFFF
Gap: 4px
Text "Ngày thanh toán: 25/05/2026" — 13px / rgba(255,255,255,0.75)
```

**Salary Breakdown Card (AppCard)**
```
Title "Chi tiết lương"
InfoRows:
  Lương cơ bản — [amount]
  Thưởng — [amount]
  Tăng ca — [amount]
```

**Bank Info Card (AppCard)**
```
Title "Thông tin ngân hàng"
InfoRows:
  Ngân hàng — [name]
  Chủ tài khoản — [name]
  Số tài khoản — [number]
  Chi nhánh — [branch]
```

---

## Screen 6 — Admin Dashboard

**Frame size:** 390 × 844  
**Background:** `#F7F9FC`

### Layout

#### AppBar
```
Standard, bg #1A237E
Title: "Quản lý hệ thống" — 18px / 600 / #FFFFFF
Actions: refresh + logout icons
```

#### Content (padding 16px, gap 16px)

**Header Card (GradientCard)**
```
Row:
  Icon Container (48×48px) bg: rgba(255,255,255,0.15), radius 12px
    Icon: admin_panel_settings — 28px / #FFFFFF
  Gap 16px
  Column:
    Text "Hệ thống quản lý" — 18px / 700 / #FFFFFF
    Text "Tổng quan nhân sự" — 13px / rgba(255,255,255,0.75)
```

**Warning Banner (nếu có pending)**
```
bg: #FFFBEB, border #F59E0B 1px, radius 12px, padding 12px×16px
Row: warning icon #F59E0B + Text "X tài khoản chờ duyệt"
```

**Stats Grid (2×2)**
```
GridView 2 cột, gap 12px, childAspectRatio 1.4

Mỗi ô (AppCard, padding 16px):
  Icon Container (36×36px) bg: color 10%, radius 8px
  Gap: 8px
  Text số — 24px / 700 / #0F172A
  Text label — 12px / #64748B

  Ô 1: people — "Tổng nhân viên" — #0EA5E9
  Ô 2: check_circle — "Đang hoạt động" — #059669
  Ô 3: how_to_reg — "Chấm công hôm nay" — #1A237E
  Ô 4: pending_actions — "Yêu cầu chờ" — #F59E0B
```

**Quick Links Grid (2×2)**
```
Tương tự Home Quick Access Grid

  Ô 1: manage_accounts "Nhân sự" — #1A237E
  Ô 2: how_to_reg "Duyệt tài khoản" — #F59E0B
  Ô 3: card_membership "Cấp Salary VC" — #059669
  Ô 4: qr_code_scanner "Quét xác minh" — #0EA5E9
```

---

## Screen 7 — Contract (Hợp đồng)

**Frame size:** 390 × 844  
**Background:** `#F7F9FC`

### Layout

#### AppBar
```
Standard, bg #1A237E
Title: "Hợp đồng lao động"
```

#### Content (padding 16px, gap 16px)

**Header Card (Green GradientCard)**
```
Background: #059669 → #047857
Padding: 24px

Text "Loại hợp đồng" — 13px / rgba(255,255,255,0.8)
Gap: 8px
Text "Hợp đồng chính thức" — 22px / 700 / #FFFFFF
Gap: 12px
StatusBadge "Có hiệu lực" — success variant
```

**Dates Card (AppCard)**
```
Title "Thông tin thời hạn"
InfoRows:
  Ngày bắt đầu
  Ngày kết thúc
  Ngày hết hạn
  Thời gian thử việc
```

**Insurance Card (AppCard)**
```
Title "Bảo hiểm & Thuế"
InfoRows:
  Mã số thuế
  Bảo hiểm xã hội
  Bảo hiểm y tế
```

---

## 4. NAVIGATION

### Bottom Navigation Bar
```
Height: 56px
Background: #FFFFFF
Border Top: 1px #E2E8F0
Type: Fixed (không scroll)

Items (Employee):
  1. Home — home_rounded
  2. Attendance — fingerprint_rounded (hoặc timer)
  3. Requests — assignment_outlined
  4. Wallet — account_balance_wallet_outlined
  5. More — grid_view_rounded

Active state: icon + label #1A237E
Inactive state: icon + label #94A3B8
```

---

## 5. FIGMA IMPORT GUIDE

### Bước 1 — Setup File
1. Tạo Figma file mới
2. Tạo Page: `🎨 Tokens`, `🧩 Components`, `📱 Screens`

### Bước 2 — Tokens Page
Tạo các Color Styles:
- Đặt tên theo format: `Color/Primary`, `Color/Background`, v.v.
- Nhập đúng hex từ bảng Section 1.1

Tạo Text Styles:
- Format: `Type/Display Large`, `Type/Body Medium`, v.v.
- Font: Inter (cần install Google Fonts plugin)

### Bước 3 — Components Page
Tạo Components theo thứ tự:
1. `Button/Primary` — Frame 390×52
2. `Button/Primary/Loading` — variant
3. `Input/Default`, `Input/Focused`, `Input/Error`
4. `Card/Default`, `Card/Gradient`
5. `Badge/Success`, `Badge/Warning`, `Badge/Error`, `Badge/Info`
6. `NavBar/Employee`, `NavBar/Admin`

### Bước 4 — Screens Page
Tạo Frame `iPhone 14` (390×844) cho mỗi screen:
1. Sign In
2. Home (Employee)
3. Attendance
4. Profile
5. Payroll
6. Contract
7. Admin Dashboard

### Plugin gợi ý
- **Google Fonts** — để dùng font Inter
- **Iconify** — import Material Icons (tìm `material-symbols`)
- **Unsplash** — nếu cần placeholder ảnh

---

*Generated from Flutter source code — TrustID Identity Fabric*
