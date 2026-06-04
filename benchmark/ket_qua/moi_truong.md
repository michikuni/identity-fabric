# Khai báo môi trường đo (điền số THẬT — phục vụ Bảng 5.2)

> Dán output các lệnh ở mục 1.2 của RUNBOOK vào đây. Báo cáo cần ghi đủ để người
> khác tái lập được phép đo.

| Thành phần | Giá trị (điền) |
|---|---|
| CPU (model / số core / xung nhịp) | Intel(R) Xeon(R) CPU @ 2.20GHz — 4 vCPU — base 2.20 GHz (GCP VM, không expose CPU max MHz) |
| RAM | 15 GiB tổng (≈16 GB). Lúc đo: 2.6 GiB used / 9.2 GiB free / 3.8 GiB buff-cache / **12 GiB available** |
| Đĩa (SSD/HDD, dung lượng trống) | `sda` 50 GB, ROTA=0 → **SSD** (non-rotational). Root fs `/dev/root` còn trống **26 GB** (các `loopX` là snap mounts, bỏ qua) |
| Mạng (localhost / LAN gigabit / WAN) | **localhost** — Caliper, backend, MySQL và Fabric chạy chung 1 VM, giao tiếp qua loopback / Docker bridge (không qua mạng vật lý) |
| Hệ điều hành (host + WSL nếu có) | Ubuntu 22.04 LTS (Jammy), kernel 64-bit, chạy native trên GCP VM — **không dùng WSL** |
| JVM (phiên bản / heap / GC) | Java 17, 64-Bit Server VM (host: OpenJDK 17.0.19+10; backend chạy trong container `eclipse-temurin:17-jre-alpine`). Entrypoint `java -jar app.jar` — **không set `-Xmx` và container không giới hạn RAM** → MaxHeap = default 25% × 15 GiB ≈ **3.8 GB**. GC: mặc định **G1GC** |
| MySQL (phiên bản / innodb_buffer_pool_size) | MySQL 8.0.46-0ubuntu0.22.04.2. `innodb_buffer_pool_size` = 134217728 bytes = **128 MB** (default) |
| Fabric (số peer / orderer / BatchTimeout / MaxMessageCount / PreferredMaxBytes) | 7 container peer+orderer. BatchTimeout **2s** / MaxMessageCount **500** / PreferredMaxBytes **2 MB** |
| Phiên bản code (git commit hash) | `fdc3813` (full: `fdc3813f971ca0477269c0c6925922172152733c`) — "Benmark" |
| Ngày giờ đo | 2026-06-04 (giờ VN, UTC+7) |
