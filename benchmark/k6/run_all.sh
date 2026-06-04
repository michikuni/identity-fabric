#!/usr/bin/env bash
# run_all.sh — Chạy lần lượt 13 test case k6, lưu JSON kết quả vào ../ket_qua/.
# Chạy tuần tự (không song song) để mỗi endpoint được đo độc lập.
set -uo pipefail
mkdir -p ../ket_qua
for tc in $(seq 1 13); do
  echo "=================== Đang đo TC=$tc ==================="
  TC=$tc k6 run rest_load.js || echo "TC=$tc gặp lỗi (kiểm tra body/endpoint/token)"
  echo "Nghỉ 10s cho hệ thống ổn định..."
  sleep 10
done
echo "Xong. Xem các file ../ket_qua/k6_tc_*.json"
