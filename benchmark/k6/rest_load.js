// rest_load.js — Kịch bản đo hiệu năng REST API của backend TrustID bằng k6.
// ĐÃ HIỆU CHỈNH path/body/role khớp controller thật (xem RUNBOOK mục 2).
//
// Cách dùng: chọn test case bằng biến môi trường TC (1..13), chạy TỪNG cái một.
//   export BASE_URL=http://localhost:8080
//   export LOGIN_EMAIL=...  LOGIN_PASSWORD=...     # tài khoản user thường (TC1)
//   export TOKEN=$(./get_token.sh)                 # token user thường
//   export ADMIN_TOKEN=...                         # token ADMIN — cho TC3/6/12/13
//   export USER_ID=...                             # id user (TC2)
//   export EMP_ID=12  DID=did:fabric:trustid:12    # employee có VC + DID thật
//   export STATUS_LIST_ID=employment-status-list-1  REC_ID=12  REC_TYPE=PROFILE
//   export VC_JSON="$(curl -s $BASE_URL/api/v1/identity/vc/employment/$EMP_ID | jq -r '.data.vc')"
//   export SD_PRESENTATION=...                     # SD-JWT presentation (TC7) — seed ở RUNBOOK
//   TC=4 k6 run rest_load.js
//   # TC cần ADMIN chạy kèm token admin:  TC=6 TOKEN=$ADMIN_TOKEN k6 run rest_load.js
//
// Script gửi request THẬT tới backend đang chạy; k6 đo độ trễ thật.

import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const EMP_ID = __ENV.EMP_ID || '12';
const DID = __ENV.DID || 'did:fabric:trustid:12';
const STATUS_LIST_ID = __ENV.STATUS_LIST_ID || 'employment-status-list-1';
const REC_ID = __ENV.REC_ID || '12';
const REC_TYPE = __ENV.REC_TYPE || 'PROFILE';
const TC = parseInt(__ENV.TC || '4', 10);

const authHeaders = { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}` } };
const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

// dataHash giả lập (64 hex) cho ledger write — chaincode chỉ lưu chuỗi, không kiểm định dạng.
const DUMMY_HASH = 'a'.repeat(64);

// 13 test case — path/body/role đã đối chiếu controller thật.
// auth=true → gửi Authorization. TC ADMIN (3,6,12,13) chạy với TOKEN=$ADMIN_TOKEN.
const CASES = {
  1:  { name: 'auth_signin',         method: 'POST', url: `${BASE}/api/v1/auth/sign-in`,
        body: { username: __ENV.LOGIN_USERNAME || __ENV.LOGIN_EMAIL || 'user@example.com', password: __ENV.LOGIN_PASSWORD || 'password' },
        rate: 100, dur: '60s', p95: 200, auth: false },                       // public — RATE-LIMIT 10/phút/IP

  2:  { name: 'mfa_validate',        method: 'POST', url: `${BASE}/api/v1/mfa/validate`,
        body: { userId: __ENV.USER_ID || '', code: __ENV.MFA_CODE || '000000' },
        rate: 50, dur: '60s', p95: 250, auth: false },                        // public

  3:  { name: 'vc_issue_training',   method: 'POST', url: `${BASE}/api/v1/admin/employees/${EMP_ID}/issue-training-vc`,
        body: { trainingName: 'Security Awareness', provider: 'TrustID Academy', completedDate: '2026-06-04', score: 'A' },
        rate: 2, dur: '30s', p95: 800, auth: true },                          // ADMIN — write+Fabric (đại diện cấp VC); low-rate

  4:  { name: 'vc_verify',           method: 'POST', url: `${BASE}/api/v1/identity/vc/verify`,
        body: { vc: __ENV.VC_JSON || '' },
        rate: 200, dur: '60s', p95: 30, auth: false },                        // public — HMAC verify

  5:  { name: 'status_list_fetch',   method: 'GET',  url: `${BASE}/api/v1/status-list/${STATUS_LIST_ID}`,
        body: null, rate: 200, dur: '60s', p95: 80, auth: false },            // public

  6:  { name: 'sdjwt_issue_skill',   method: 'POST', url: `${BASE}/api/v1/sd-jwt/issue/skill/${EMP_ID}`,
        body: { skills: { Kotlin: 'ADVANCED', Java: 'INTERMEDIATE', SQL: 'BASIC' } },
        rate: 2, dur: '30s', p95: 600, auth: true },                          // ADMIN — write+Fabric; low-rate

  7:  { name: 'sdjwt_verify',        method: 'POST', url: `${BASE}/api/v1/sd-jwt/verify`,
        body: { presentation: __ENV.SD_PRESENTATION || '', requireClaims: [] },
        rate: 100, dur: '60s', p95: 50, auth: false },                        // public

  8:  { name: 'oid4vp_request',      method: 'POST', url: `${BASE}/api/v1/oidc/vp/request`,
        body: { vcType: 'EmploymentCredential', requestedClaims: ['employmentStatus', 'position'] },
        rate: 50, dur: '60s', p95: 100, auth: false },                        // public — tạo session in-mem

  9:  { name: 'oid4vp_submit',       method: 'POST', url: `${BASE}/api/v1/oidc/vp/submit`,
        body: { state: __ENV.VP_STATE || '', vpToken: __ENV.VP_TOKEN || '' },
        rate: 50, dur: '60s', p95: 150, auth: false },                        // public — cần state+vpToken thật

  10: { name: 'did_resolve',         method: 'GET',  url: `${BASE}/1.0/identifiers/${DID}`,
        body: null, rate: 100, dur: '60s', p95: 300, auth: false },           // public — Fabric query

  11: { name: 'trust_registry',      method: 'GET',  url: `${BASE}/api/v1/trust-registry/issuers`,
        body: null, rate: 100, dur: '60s', p95: 200, auth: false },           // public

  12: { name: 'ledger_write',        method: 'POST', url: `${BASE}/api/v1/ledger/records`,
        body: null, rate: 10, dur: '60s', p95: 5000, auth: true, dynamic: true }, // ADMIN — Fabric SUBMIT (key unique/req)

  13: { name: 'ledger_read',         method: 'GET',  url: `${BASE}/api/v1/ledger/records/${REC_ID}/${REC_TYPE}`,
        body: null, rate: 100, dur: '60s', p95: 200, auth: true },            // ADMIN — Fabric query
};

const tc = CASES[TC];
if (!tc) { throw new Error(`TC không hợp lệ: ${TC} (hợp lệ 1..13)`); }

export const options = {
  // Bắt k6 tính cả p(99) — mặc định chỉ có tới p(95), khiến cột p99 ra 0.
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    [tc.name]: {
      executor: 'constant-arrival-rate',
      rate: parseInt(__ENV.RATE || tc.rate, 10),
      timeUnit: '1s',
      duration: __ENV.DURATION || tc.dur,
      preAllocatedVUs: Math.max(50, tc.rate),
      maxVUs: Math.max(200, tc.rate * 4),
    },
  },
  thresholds: {
    http_req_duration: [`p(95)<${tc.p95}`],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const opts = tc.auth ? authHeaders : jsonHeaders;
  let res;
  if (tc.method === 'GET') {
    res = http.get(tc.url, opts);
  } else {
    let body = tc.body || {};
    // TC12: mỗi request một employeeId duy nhất → tránh xung đột MVCC trên cùng key.
    if (tc.dynamic && tc.name === 'ledger_write') {
      body = {
        employeeId: `bench-${Date.now()}-${__VU}-${__ITER}`,
        recordType: 'PROFILE',
        status: 'ACTIVE',
        keyFields: '{"name":"bench"}',
        dataHash: DUMMY_HASH,
        action: 'CREATE',
        updatedBy: 'bench',
      };
    }
    res = http.post(tc.url, JSON.stringify(body), opts);
  }
  check(res, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });
}

// Xuất summary ra JSON để lưu bằng chứng đo (run_all.sh dùng tên file theo TC).
export function handleSummary(data) {
  const d = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : {};
  const f = data.metrics.http_req_failed ? data.metrics.http_req_failed.values : {};
  const line =
    `\n[KẾT QUẢ TC=${TC} ${tc.name}] ` +
    `p50=${(d['med']||0).toFixed(2)}ms  p95=${(d['p(95)']||0).toFixed(2)}ms  ` +
    `p99=${(d['p(99)']||0).toFixed(2)}ms  err=${((f['rate']||0)*100).toFixed(2)}%\n`;
  const out = {};
  out['stdout'] = line; // in ra màn hình
  out[`../ket_qua/k6_tc_${TC}_${tc.name}.json`] = JSON.stringify(data, null, 2);
  return out;
}
