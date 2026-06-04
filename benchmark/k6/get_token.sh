#!/usr/bin/env bash
# get_token.sh — Đăng nhập backend TrustID và in ra JWT (để các test k6 tái sử dụng).
# Sửa email/mật khẩu cho khớp tài khoản test có thật trong DB của bạn.
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${LOGIN_EMAIL:-user@example.com}"
PASSWORD="${LOGIN_PASSWORD:-password}"

RESP=$(curl -s -X POST "$BASE_URL/api/v1/auth/sign-in" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

# Thử lấy trường token phổ biến (token / accessToken / data.token). Cần `jq`.
echo "$RESP" | jq -r '.token // .accessToken // .data.token // .data.accessToken // empty'
