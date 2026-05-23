import { tStatic } from '../i18n/I18nContext'

const BASE = import.meta.env.VITE_BACKEND_URL ?? ''

export type VerifyStatus = 'VALID' | 'INVALID' | 'REVOKED' | 'EXPIRED' | 'ERROR'

export interface VcVerifyResult {
  valid: boolean
  status: VerifyStatus
  reason: string
  issuerDid?: string
  subjectDid?: string
  vcType?: string[]
  issuanceDate?: string
  expirationDate?: string
  revocationChecked?: boolean
  trustRegistryChecked?: boolean
  issuerTrusted?: boolean
  disclosedClaims?: Record<string, unknown>
}

export interface DIDDocument {
  id: string
  '@context': string[]
  verificationMethod: VerificationMethod[]
  authentication: string[]
  service?: ServiceEndpoint[]
  didDocumentMetadata: {
    deactivated: boolean
    created?: string
    updated?: string
  }
  didResolutionMetadata: {
    contentType: string
    retrieved?: string
  }
}

interface VerificationMethod {
  id: string
  type: string
  controller: string
  publicKeyJwk?: Record<string, unknown>
}

interface ServiceEndpoint {
  id: string
  type: string
  serviceEndpoint: string
}

function friendlyHttpError(status: number, body: string): string {
  // Try to extract message from JSON body first
  try {
    const json = JSON.parse(body)
    if (json?.message) return json.message
    if (json?.error) return json.error
  } catch { /* not JSON */ }

  switch (status) {
    case 400: return tStatic('errors.http400')
    case 401: return tStatic('errors.http401')
    case 403: return tStatic('errors.http403')
    case 404: return tStatic('errors.http404')
    case 409: return tStatic('errors.http409')
    case 422: return tStatic('errors.http422')
    case 429: return tStatic('errors.http429')
    case 500: return tStatic('errors.http500')
    case 502:
    case 503: return tStatic('errors.http503')
    default:  return tStatic('errors.httpDefault', { status })
  }
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(friendlyHttpError(res.status, text))
  }
  return res.json()
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) {
    const text = await res.text()
    throw new Error(friendlyHttpError(res.status, text))
  }
  return res.json()
}

function toUserMessage(e: unknown): string {
  if (e instanceof Error) {
    if (e.message.includes('Failed to fetch') || e.message.includes('NetworkError')) {
      return tStatic('errors.networkUnreachable')
    }
    return e.message
  }
  return tStatic('common.unexpectedError')
}

/** Verify a standard W3C VC (JSON string) */
export async function verifyVC(vcJson: string): Promise<VcVerifyResult> {
  try {
    // Backend bọc kết quả trong ApiResponse { status, message, data } và đòi field 'vc'.
    const res = await post<{ data: { valid: boolean; reason: string } }>(
      '/api/v1/identity/vc/verify',
      { vc: vcJson },
    )
    const d = res.data
    return {
      valid: d.valid,
      status: d.valid
        ? 'VALID'
        : (d.reason ?? '').toLowerCase().includes('revoked')
          ? 'REVOKED'
          : 'INVALID',
      reason: d.reason ?? '',
    }
  } catch (e) {
    return { valid: false, status: 'ERROR', reason: toUserMessage(e) }
  }
}

/** Verify an SD-JWT presentation (compact format: header.payload.sig~d1~d2~) */
export async function verifySdJwt(presentation: string): Promise<VcVerifyResult> {
  try {
    // Backend bọc trong ApiResponse.data và dùng tên field issuer/subject/vct.
    const res = await post<{
      data: {
        valid: boolean
        reason: string
        disclosedClaims?: Record<string, unknown>
        subject?: string
        issuer?: string
        vct?: string
      }
    }>('/api/v1/sd-jwt/verify', { presentation })
    const d = res.data
    return {
      valid: d.valid,
      status: d.valid
        ? 'VALID'
        : (d.reason ?? '').toLowerCase().includes('revoked')
          ? 'REVOKED'
          : 'INVALID',
      reason: d.reason ?? '',
      disclosedClaims: d.disclosedClaims,
      subjectDid: d.subject,
      issuerDid: d.issuer,
      vcType: d.vct ? [d.vct] : undefined,
    }
  } catch (e) {
    return { valid: false, status: 'ERROR', reason: toUserMessage(e) }
  }
}

/** Resolve a DID via the DIF Universal Resolver-compatible endpoint */
export async function resolveDID(did: string): Promise<DIDDocument> {
  // Backend trả DID Resolution Result: { didDocument, didDocumentMetadata, didResolutionMetadata }.
  // Web cần gộp didDocument với metadata để khớp interface DIDDocument.
  const res = await get<{
    didDocument: Omit<DIDDocument, 'didDocumentMetadata' | 'didResolutionMetadata'>
    didDocumentMetadata: DIDDocument['didDocumentMetadata']
    didResolutionMetadata: DIDDocument['didResolutionMetadata']
  }>(`/1.0/identifiers/${encodeURIComponent(did)}`)
  return {
    ...res.didDocument,
    didDocumentMetadata: res.didDocumentMetadata,
    didResolutionMetadata: res.didResolutionMetadata,
  }
}

/** Get the W3C Status List 2021 credential for a given listId */
export async function getStatusList(listId: string): Promise<Record<string, unknown>> {
  return get<Record<string, unknown>>(`/api/v1/status-list/${listId}`)
}

// ── OID4VP (QR request flow) ────────────────────────────────────────────────

export interface VpRequestSession {
  state: string
  nonce: string
  expiresIn: number
  /** OID4VP Authorization Request — encode toàn bộ object này thành QR cho Holder quét. */
  authorizationRequest: unknown
}

export type VpResultStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface VpResult {
  state: string
  status: VpResultStatus
  valid?: boolean
  reason?: string
  disclosedFields?: Record<string, unknown>
}

/** Verifier tạo VP Request → nhận authorizationRequest để render QR + state để poll. */
export async function createVpRequest(
  vcType: string,
  requestedClaims: string[],
): Promise<VpRequestSession> {
  const res = await post<{ data: VpRequestSession }>('/api/v1/oidc/vp/request', {
    vcType,
    requestedClaims,
  })
  return res.data
}

/** Poll kết quả sau khi Holder quét QR và gửi VP. */
export async function pollVpResult(state: string): Promise<VpResult> {
  const res = await get<{ data: VpResult }>(
    `/api/v1/oidc/vp/result/${encodeURIComponent(state)}`,
  )
  return res.data
}

/** Auto-detect payload type and verify */
export async function smartVerify(payload: string): Promise<VcVerifyResult> {
  const trimmed = payload.trim()
  if (trimmed.includes('~')) {
    return verifySdJwt(trimmed)
  }
  try {
    JSON.parse(trimmed)
    return verifyVC(trimmed)
  } catch {
    return { valid: false, status: 'ERROR', reason: tStatic('errors.cannotParsePayload') }
  }
}
