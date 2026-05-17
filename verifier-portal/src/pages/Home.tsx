import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import QRCode from 'qrcode'
import { smartVerify, type VcVerifyResult } from '../lib/trustid-client'

type InputMode = 'paste' | 'qr-request'

const SKILL_PROOF_REQUEST = JSON.stringify({
  type: 'VerifiablePresentationRequest',
  challenge: crypto.randomUUID(),
  domain: window.location.origin,
  query: [{ type: 'QueryByExample', credentialQuery: [{ reason: 'Job application skill check', example: { type: 'SkillCredential' } }] }],
  callbackUrl: `${window.location.origin}/verify`,
}, null, 2)

export default function Home() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<InputMode>('paste')
  const [payload, setPayload] = useState('')
  const [loading, setLoading] = useState(false)
  const [qrDataUrl, setQrDataUrl] = useState('')
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    if (mode === 'qr-request') {
      QRCode.toDataURL(SKILL_PROOF_REQUEST, { width: 256, margin: 2 })
        .then(setQrDataUrl)
        .catch(console.error)
    }
  }, [mode])

  async function handleVerify() {
    if (!payload.trim()) return
    setLoading(true)
    try {
      const result: VcVerifyResult = await smartVerify(payload.trim())
      navigate('/verify', { state: { result, raw: payload.trim() } })
    } finally {
      setLoading(false)
    }
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
                onClick={handleVerify}
                disabled={!payload.trim() || loading}
                className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 text-white font-semibold py-2.5 rounded-lg transition-colors"
              >
                {loading ? 'Verifying…' : 'Verify Credential'}
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              <p className="text-sm text-gray-600">
                Scan this QR with the <strong>TrustID Holder app</strong>. The holder selects claims to disclose
                and sends an SD-JWT presentation back. Once you receive the presentation, paste it in the
                <button onClick={() => setMode('paste')} className="text-brand-600 underline mx-1">Paste tab</button>
                above to verify.
              </p>
              <div className="flex flex-col items-center gap-4">
                {qrDataUrl ? (
                  <img src={qrDataUrl} alt="VP Request QR" className="rounded-xl border border-gray-200 shadow-sm" width={256} height={256} />
                ) : (
                  <div className="w-64 h-64 bg-gray-100 rounded-xl animate-pulse" />
                )}
                <canvas ref={canvasRef} className="hidden" />
                <div className="bg-gray-50 rounded-lg border border-gray-200 p-3 w-full">
                  <p className="text-xs text-gray-500 font-medium mb-1">VP Request payload</p>
                  <pre className="text-xs text-gray-700 overflow-auto max-h-32 whitespace-pre-wrap break-all">{SKILL_PROOF_REQUEST}</pre>
                </div>
              </div>
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
