import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import QRCode from 'qrcode'
import {
  smartVerify,
  createVpRequest,
  pollVpResult,
  type VcVerifyResult,
  type VpResult,
} from '../lib/trustid-client'

type InputMode = 'paste' | 'qr-request'

// Đồng bộ với kVcSchemas (Flutter) + VcIssuerService (backend).
const VC_SCHEMAS: Record<string, { label: string; fields: string[] }> = {
  EmploymentCredential: {
    label: 'Employment Credential',
    fields: ['department', 'position', 'employmentStatus', 'startDate'],
  },
  SalaryRangeCredential: {
    label: 'Salary Range Credential',
    fields: ['salaryBand', 'currency', 'position', 'department', 'issuedAt'],
  },
  PromotionCredential: {
    label: 'Promotion Credential',
    fields: ['department', 'oldPosition', 'newPosition', 'promotionDate', 'promotedBy'],
  },
  TerminationCredential: {
    label: 'Termination Credential',
    fields: ['department', 'position', 'employmentStatus', 'terminationDate', 'terminationReason', 'revokedBy'],
  },
}

function humanize(key: string): string {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, c => c.toUpperCase()).trim()
}

export default function Home() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<InputMode>('paste')

  // ── Paste mode ──
  const [payload, setPayload] = useState('')
  const [loading, setLoading] = useState(false)

  // ── QR request (OID4VP) mode ──
  const [vcType, setVcType] = useState<string>('EmploymentCredential')
  const [selected, setSelected] = useState<Set<string>>(new Set(['employmentStatus', 'position']))
  const [creating, setCreating] = useState(false)
  const [state, setState] = useState<string | null>(null)
  const [qrDataUrl, setQrDataUrl] = useState('')
  const [result, setResult] = useState<VpResult | null>(null)
  const [qrError, setQrError] = useState('')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const stopPolling = useCallback(() => {
    if (pollRef.current) {
      clearInterval(pollRef.current)
      pollRef.current = null
    }
  }, [])

  useEffect(() => stopPolling, [stopPolling])

  // Reset selection khi đổi loại VC
  function changeVcType(t: string) {
    setVcType(t)
    setSelected(new Set())
    resetRequest()
  }

  function toggleClaim(field: string) {
    setSelected(prev => {
      const next = new Set(prev)
      next.has(field) ? next.delete(field) : next.add(field)
      return next
    })
    resetRequest()
  }

  function resetRequest() {
    stopPolling()
    setState(null)
    setQrDataUrl('')
    setResult(null)
    setQrError('')
  }

  async function handleVerifyPaste() {
    if (!payload.trim()) return
    setLoading(true)
    try {
      const res: VcVerifyResult = await smartVerify(payload.trim())
      navigate('/verify', { state: { result: res, raw: payload.trim() } })
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateQr() {
    if (selected.size === 0) {
      setQrError('Select at least one claim to request.')
      return
    }
    setCreating(true)
    setQrError('')
    setResult(null)
    try {
      const session = await createVpRequest(vcType, [...selected])
      setState(session.state)
      const url = await QRCode.toDataURL(JSON.stringify(session.authorizationRequest), {
        width: 280,
        margin: 2,
        errorCorrectionLevel: 'L',
      })
      setQrDataUrl(url)
      startPolling(session.state)
    } catch (e) {
      setQrError(e instanceof Error ? e.message : 'Failed to create VP request.')
    } finally {
      setCreating(false)
    }
  }

  function startPolling(s: string) {
    stopPolling()
    pollRef.current = setInterval(async () => {
      try {
        const r = await pollVpResult(s)
        if (r.status !== 'PENDING') {
          stopPolling()
          setResult(r)
        }
      } catch {
        /* keep polling; transient errors ignored */
      }
    }, 2500)
  }

  return (
    <div className="space-y-8">
      {/* Hero */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-8 text-center">
        <div className="text-5xl mb-4">🔍</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Credential Verifier</h1>
        <p className="text-gray-500 max-w-lg mx-auto">
          Independently verify W3C Verifiable Credentials and SD-JWT presentations issued by TrustID.
          No account required — operates as a standalone third-party verifier.
        </p>
      </div>

      {/* Mode tabs */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="flex border-b">
          <button
            onClick={() => setMode('paste')}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${mode === 'paste' ? 'bg-brand-50 text-brand-700 border-b-2 border-brand-600' : 'text-gray-500 hover:text-gray-900'}`}
          >
            📋 Paste Credential
          </button>
          <button
            onClick={() => setMode('qr-request')}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${mode === 'qr-request' ? 'bg-brand-50 text-brand-700 border-b-2 border-brand-600' : 'text-gray-500 hover:text-gray-900'}`}
          >
            📱 Request via QR
          </button>
        </div>

        <div className="p-6">
          {mode === 'paste' ? (
            <div className="space-y-4">
              <label className="block text-sm font-medium text-gray-700">
                Paste W3C VC (JSON) or SD-JWT presentation
              </label>
              <textarea
                className="w-full h-48 rounded-lg border border-gray-300 p-3 font-mono text-xs focus:outline-none focus:ring-2 focus:ring-brand-500 resize-none"
                placeholder={`Paste either:\n• W3C VC JSON:  { "@context": [...], "type": ["VerifiableCredential", ...], ... }\n• SD-JWT:       eyJ...~disclosure1~disclosure2~`}
                value={payload}
                onChange={e => setPayload(e.target.value)}
                spellCheck={false}
              />
              <button
                onClick={handleVerifyPaste}
                disabled={!payload.trim() || loading}
                className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 text-white font-semibold py-2.5 rounded-lg transition-colors"
              >
                {loading ? 'Verifying…' : 'Verify Credential'}
              </button>
            </div>
          ) : (
            <div className="space-y-5">
              <p className="text-sm text-gray-600">
                Pick a credential type and the exact claims you want proven, then generate a request QR.
                The holder scans it with the <strong>TrustID app</strong>, approves the disclosure, and the
                result appears here automatically — no copy-paste needed.
              </p>

              {/* VC type selector */}
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">Credential type</label>
                <select
                  value={vcType}
                  onChange={e => changeVcType(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                >
                  {Object.entries(VC_SCHEMAS).map(([type, s]) => (
                    <option key={type} value={type}>{s.label}</option>
                  ))}
                </select>
              </div>

              {/* Claim checkboxes */}
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">Claims to request</label>
                <div className="grid grid-cols-2 gap-2">
                  {VC_SCHEMAS[vcType].fields.map(field => (
                    <label
                      key={field}
                      className={`flex items-center gap-2 rounded-lg border p-2.5 text-sm cursor-pointer transition-colors ${selected.has(field) ? 'border-brand-400 bg-brand-50' : 'border-gray-200 hover:border-gray-300'}`}
                    >
                      <input
                        type="checkbox"
                        checked={selected.has(field)}
                        onChange={() => toggleClaim(field)}
                        className="accent-brand-600"
                      />
                      {humanize(field)}
                    </label>
                  ))}
                </div>
              </div>

              {qrError && <p className="text-sm text-red-600">{qrError}</p>}

              {!state ? (
                <button
                  onClick={handleCreateQr}
                  disabled={creating || selected.size === 0}
                  className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 text-white font-semibold py-2.5 rounded-lg transition-colors"
                >
                  {creating ? 'Creating request…' : 'Generate Request QR'}
                </button>
              ) : (
                <div className="flex flex-col items-center gap-4">
                  {qrDataUrl && (
                    <img src={qrDataUrl} alt="VP Request QR" className="rounded-xl border border-gray-200 shadow-sm" width={280} height={280} />
                  )}

                  {/* Status */}
                  {!result ? (
                    <div className="flex items-center gap-2 text-sm text-gray-500">
                      <span className="inline-block w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
                      Waiting for holder to scan and approve…
                    </div>
                  ) : result.status === 'ACCEPTED' ? (
                    <div className="w-full bg-green-50 border border-green-200 rounded-xl p-4">
                      <p className="text-green-800 font-semibold mb-2">✅ Verified — holder shared:</p>
                      <div className="grid grid-cols-1 gap-1.5">
                        {Object.entries(result.disclosedFields ?? {}).map(([k, v]) => (
                          <div key={k} className="flex justify-between text-sm">
                            <span className="text-gray-500">{humanize(k)}</span>
                            <span className="text-gray-900 font-medium">{String(v)}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="w-full bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
                      ❌ Rejected{result.reason ? `: ${result.reason}` : ''}
                    </div>
                  )}

                  <button onClick={resetRequest} className="text-brand-600 underline text-sm">
                    New request
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Info cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { icon: '🏛️', title: 'W3C VC Support', desc: 'Employment, Salary, Promotion, Termination credentials issued by TrustID.' },
          { icon: '🔏', title: 'SD-JWT Selective Disclosure', desc: 'Verify only the fields the holder chooses to share — issuer signature still validated.' },
          { icon: '📋', title: 'Trust Registry', desc: 'All issuers cross-checked against the on-chain Trust Registry (EBSI / eIDAS pattern).' },
        ].map(card => (
          <div key={card.title} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
            <div className="text-2xl mb-2">{card.icon}</div>
            <h3 className="font-semibold text-gray-900 mb-1">{card.title}</h3>
            <p className="text-sm text-gray-500">{card.desc}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
