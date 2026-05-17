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

export interface TrustedIssuer {
  did: string
  name: string
  role: string
  scope: string
  registeredAt: string
  active: boolean
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
    case 400: return 'Invalid request. Please check your input.'
    case 401: return 'Authentication required. Please sign in again.'
    case 403: return 'You do not have permission to perform this action.'
    case 404: return 'The requested resource was not found.'
    case 409: return 'Conflict — this credential may already exist.'
    case 422: return 'Unprocessable request. Please check your input.'
    case 429: return 'Too many requests. Please wait a moment and try again.'
    case 500: return 'Server error. Please try again later.'
    case 502:
    case 503: return 'Service temporarily unavailable. Please try again later.'
    default:  return `Request failed (HTTP ${status}). Please try again.`
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
      return 'Cannot reach the server. Please check your connection and try again.'
    }
    return e.message
  }
  return 'An unexpected error occurred.'
}

/** Verify a standard W3C VC (JSON string) */
export async function verifyVC(vcJson: string): Promise<VcVerifyResult> {
  try {
    const res = await post<{ valid: boolean; reason: string }>('/api/v1/identity/vc/verify', { vcJson })
    return {
      valid: res.valid,
      status: res.valid ? 'VALID' : 'INVALID',
      reason: res.reason ?? '',
    }
  } catch (e) {
    return { valid: false, status: 'ERROR', reason: toUserMessage(e) }
  }
}

/** Verify an SD-JWT presentation (compact format: header.payload.sig~d1~d2~) */
export async function verifySdJwt(presentation: string): Promise<VcVerifyResult> {
  try {
    const res = await post<{
      valid: boolean
      reason: string
      disclosedClaims?: Record<string, unknown>
      subjectDid?: string
      issuerDid?: string
      vcType?: string
    }>('/api/v1/sd-jwt/verify', { presentation })
    return {
      valid: res.valid,
      status: res.valid ? 'VALID' : 'INVALID',
      reason: res.reason ?? '',
      disclosedClaims: res.disclosedClaims,
      subjectDid: res.subjectDid,
      issuerDid: res.issuerDid,
      vcType: res.vcType ? [res.vcType] : undefined,
    }
  } catch (e) {
    return { valid: false, status: 'ERROR', reason: toUserMessage(e) }
  }
}

/** Resolve a DID via the DIF Universal Resolver-compatible endpoint */
export async function resolveDID(did: string): Promise<DIDDocument> {
  return get<DIDDocument>(`/1.0/identifiers/${encodeURIComponent(did)}`)
}

/** List trusted issuers from the on-chain Trust Registry */
export async function listTrustedIssuers(): Promise<TrustedIssuer[]> {
  return get<TrustedIssuer[]>('/api/v1/trust-registry/issuers')
}

/** Get the W3C Status List 2021 credential for a given listId */
export async function getStatusList(listId: string): Promise<Record<string, unknown>> {
  return get<Record<string, unknown>>(`/api/v1/status-list/${listId}`)
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
    return { valid: false, status: 'ERROR', reason: 'Cannot parse payload — expected JSON (W3C VC) or compact SD-JWT' }
  }
}
