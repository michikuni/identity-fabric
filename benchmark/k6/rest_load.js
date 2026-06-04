// rest_load.js — Kịch bản đo hiệu năng REST API của backend TrustID bằng k6.
//
// Cách dùng: chọn test case bằng biến môi trường TC (1..13), chạy TỪNG cái một.
//   export BASE_URL=http://localhost:8080
//   export TOKEN=$(./get_token.sh)
//   export EMP_ID=1 DID=did:fabric:trustid:1 STATUS_LIST_ID=employment-status-list-1 REC_ID=1 REC_TYPE=profile
//   TC=4 k6 run rest_load.js
//
// Script KHÔNG sinh ra số liệu giả: nó gửi request thật tới backend đang chạy và
// k6 đo độ trễ thật. Bạn đọc med/p(95)/p(99) trong output và điền vào Bảng 5.4.

import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const EMP_ID = __ENV.EMP_ID || '1';
const DID = __ENV.DID || 'did:fabric:trustid:1';
const STATUS_LIST_ID = __ENV.STATUS_LIST_ID || 'employment-status-list-1';
const REC_ID = __ENV.REC_ID || '1';
const REC_TYPE = __ENV.REC_TYPE || 'profile';
const TC = parseInt(__ENV.TC || '4', 10);

const authHeaders = { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}` } };
const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

// Định nghĩa 13 test case: phương thức, đường dẫn, body, tải đề xuất, ngưỡng p95 (baseline).
// LƯU Ý: body của các endpoint issue/verify/oidc cần khớp schema thực tế của backend bạn —
// hãy mở controller tương ứng và chỉnh body cho đúng trước khi đo.
const CASES = {
  1:  { name: 'auth_signin',        method: 'POST', url: `${BASE}/api/v1/auth/sign-in`,                       body: { username: __ENV.LOGIN_USERNAME || __ENV.LOGIN_EMAIL || 'user@example.com', password: __ENV.LOGIN_PASSWORD || 'password' }, rate: 100, dur: '60s', p95: 200, auth: false },
  2:  { name: 'mfa_validate',       method: 'POST', url: `${BASE}/api/v1/mfa/validate`,                       body: { userId: __ENV.USER_ID || '', code: __ENV.MFA_CODE || '000000' }, rate: 50,  dur: '60s', p95: 250, auth: true },
  3:  { name: 'vc_issue_employment',method: 'POST', url: `${BASE}/api/v1/admin/employees/${EMP_ID}/issue`,    body: { type: 'EmploymentVC' },                          rate: 20,  dur: '60s', p95: 800, auth: true },
  4:  { name: 'vc_verify',          method: 'POST', url: `${BASE}/api/v1/identity/vc/verify`,                 body: { vcId: __ENV.VC_ID || '1' },                      rate: 200, dur: '60s', p95: 30,  auth: true },
  5:  { name: 'status_list_fetch',  method: 'GET',  url: `${BASE}/api/v1/status-list/${STATUS_LIST_ID}`,      body: null,                                              rate: 200, dur: '60s', p95: 80,  auth: false },
  6:  { name: 'sdjwt_issue_skill',  method: 'POST', url: `${BASE}/api/v1/sd-jwt/issue/skill/${EMP_ID}`,       body: {},                                                rate: 20,  dur: '60s', p95: 600, auth: true },
  7:  { name: 'sdjwt_verify',       method: 'POST', url: `${BASE}/api/v1/sd-jwt/verify`,                      body: { sdJwt: __ENV.SD_JWT || '' },                     rate: 100, dur: '60s', p95: 50,  auth: true },
  8:  { name: 'oid4vp_request',     method: 'POST', url: `${BASE}/api/v1/oidc/vp/request`,                    body: {},                                                rate: 50,  dur: '60s', p95: 100, auth: true },
  9:  { name: 'oid4vp_submit',      method: 'POST', url: `${BASE}/api/v1/oidc/vp/submit`,                     body: { vpToken: __ENV.VP_TOKEN || '' },                 rate: 50,  dur: '60s', p95: 150, auth: true },
  10: { name: 'did_resolve',        method: 'GET',  url: `${BASE}/1.0/identifiers/${DID}`,                    body: null,                                              rate: 100, dur: '60s', p95: 300, auth: false },
  11: { name: 'trust_registry',     method: 'GET',  url: `${BASE}/api/v1/trust-registry/issuers`,             body: null,                                              rate: 100, dur: '60s', p95: 200, auth: false },
  12: { name: 'ledger_write',       method: 'POST', url: `${BASE}/api/v1/ledger/records`,                     body: { id: `bench-${Date.now()}`, type: 'profile', payload: { k: 'v' } }, rate: 10, dur: '60s', p95: 5000, auth: true },
  13: { name: 'ledger_read',        method: 'GET',  url: `${BASE}/api/v1/ledger/records/${REC_ID}/${REC_TYPE}`,body: null,                                             rate: 100, dur: '60s', p95: 200, auth: true },
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
  // Ngưỡng = baseline kỳ vọng. Nếu FAIL, không phải "sai" — chỉ là TrustID khác baseline,
  // bạn ghi nhận số thật và giải thích trong phần phân tích.
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
    res = http.post(tc.url, JSON.stringify(tc.body || {}), opts);
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
