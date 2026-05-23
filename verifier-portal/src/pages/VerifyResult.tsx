import { useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { resolveDID, type VcVerifyResult, type VerifyStatus } from '../lib/trustid-client'
import { useTranslation } from '../i18n/I18nContext'

interface LocationState {
  result: VcVerifyResult
  raw: string
}

const statusStyles: Record<VerifyStatus, { bg: string; border: string; text: string; badge: string; icon: string }> = {
  VALID:    { bg: 'bg-green-50',  border: 'border-green-300',  text: 'text-green-800',  badge: 'bg-green-100 text-green-800',  icon: '✅' },
  INVALID:  { bg: 'bg-red-50',    border: 'border-red-300',    text: 'text-red-800',    badge: 'bg-red-100 text-red-800',      icon: '❌' },
  REVOKED:  { bg: 'bg-orange-50', border: 'border-orange-300', text: 'text-orange-800', badge: 'bg-orange-100 text-orange-800',icon: '🚫' },
  EXPIRED:  { bg: 'bg-yellow-50', border: 'border-yellow-300', text: 'text-yellow-800', badge: 'bg-yellow-100 text-yellow-800',icon: '⏰' },
  ERROR:    { bg: 'bg-gray-50',   border: 'border-gray-300',   text: 'text-gray-800',   badge: 'bg-gray-100 text-gray-800',    icon: '⚠️' },
}

const statusLabelKey: Record<VerifyStatus, string> = {
  VALID:   'verify.statusValid',
  INVALID: 'verify.statusInvalid',
  REVOKED: 'verify.statusRevoked',
  EXPIRED: 'verify.statusExpired',
  ERROR:   'verify.statusError',
}

export default function VerifyResult() {
  const navigate = useNavigate()
  const location = useLocation()
  const { t } = useTranslation()
  const state = location.state as LocationState | undefined

  const result = state?.result
  const raw = state?.raw ?? ''

  const issuerDid = result?.issuerDid ?? extractIssuerDid(raw)
  const subjectDid = result?.subjectDid ?? extractSubjectDid(raw)

  const { data: didDoc, isLoading: didLoading } = useQuery({
    queryKey: ['did', issuerDid],
    queryFn: () => resolveDID(issuerDid!),
    enabled: !!issuerDid,
  })

  if (!result) {
    return (
      <div className="text-center py-16 space-y-4">
        <p className="text-gray-500">{t('verify.noResult')}</p>
        <button onClick={() => navigate('/')} className="text-brand-600 underline">{t('common.back')}</button>
      </div>
    )
  }

  const style = statusStyles[result.status] ?? statusStyles.ERROR
  const statusLabel = t(statusLabelKey[result.status] ?? 'verify.statusError')
  const disclosedClaims = result.disclosedClaims ?? {}
  const hasDisclosures = Object.keys(disclosedClaims).length > 0

  return (
    <div className="space-y-6">
      {/* Status banner */}
      <div className={`rounded-2xl border-2 ${style.border} ${style.bg} p-8 text-center shadow-sm`}>
        <div className="text-6xl mb-3">{style.icon}</div>
        <div className={`inline-block px-4 py-1.5 rounded-full text-lg font-bold ${style.badge} mb-3`}>
          {statusLabel}
        </div>
        <p className={`text-sm ${style.text} max-w-lg mx-auto`}>{result.reason}</p>
      </div>

      {/* Details grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Credential info */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-3">
          <h2 className="font-semibold text-gray-900">{t('verify.credentialDetails')}</h2>
          <DetailRow label={t('verify.detailType')} value={result.vcType?.join(', ') ?? detectType(raw)} />
          <DetailRow label={t('verify.detailIssued')} value={result.issuanceDate ?? extractDate(raw, 'issuanceDate')} />
          <DetailRow label={t('verify.detailExpires')} value={result.expirationDate ?? extractDate(raw, 'expirationDate')} />
          <DetailRow label={t('verify.detailSubjectDid')} value={subjectDid} mono />
        </div>

        {/* Issuer info */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-3">
          <h2 className="font-semibold text-gray-900">{t('verify.issuerSection')}</h2>
          <DetailRow label={t('verify.issuerDid')} value={issuerDid} mono />
          {didLoading ? (
            <p className="text-xs text-gray-400 animate-pulse">{t('verify.issuerResolving')}</p>
          ) : didDoc ? (
            <>
              <DetailRow
                label={t('verify.issuerDidStatus')}
                value={didDoc.didDocumentMetadata.deactivated ? t('verify.issuerDidDeactivated') : t('verify.issuerDidActive')}
              />
              <DetailRow
                label="Keys"
                value={t('verify.issuerKeys', { count: didDoc.verificationMethod?.length ?? 0 })}
              />
              <DetailRow
                label={t('verify.issuerTrustRegistry')}
                value={
                  result.issuerTrusted == null
                    ? t('verify.issuerTrustChecking')
                    : result.issuerTrusted
                      ? t('verify.issuerTrustTrusted')
                      : t('verify.issuerTrustNotInRegistry')
                }
              />
            </>
          ) : (
            <p className="text-xs text-red-400">{t('verify.issuerResolveFailed')}</p>
          )}
        </div>
      </div>

      {/* Selective disclosures */}
      {hasDisclosures && (
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-3">
          <h2 className="font-semibold text-gray-900">
            {t('verify.disclosuresTitle', { count: Object.keys(disclosedClaims).length })}
          </h2>
          <p className="text-xs text-gray-500">{t('verify.disclosuresSubtitle')}</p>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
            {Object.entries(disclosedClaims).map(([k, v]) => {
              const fieldTranslation = t(`fields.${k}`)
              const label = fieldTranslation === `fields.${k}` ? k : fieldTranslation
              return (
                <div key={k} className="bg-brand-50 rounded-lg p-3 border border-brand-100">
                  <p className="text-xs font-medium text-brand-700 capitalize">{label}</p>
                  <p className="text-sm text-gray-900 mt-0.5">{String(v)}</p>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Raw payload */}
      <details className="bg-white rounded-xl border border-gray-200 shadow-sm">
        <summary className="px-5 py-4 cursor-pointer text-sm font-medium text-gray-700 select-none">
          {t('verify.rawPayload')}
        </summary>
        <pre className="px-5 pb-5 text-xs text-gray-600 overflow-auto max-h-64 whitespace-pre-wrap break-all border-t border-gray-100 pt-3">
          {tryPrettyPrint(raw)}
        </pre>
      </details>

      <div className="flex justify-center">
        <button
          onClick={() => navigate('/')}
          className="bg-brand-600 hover:bg-brand-700 text-white font-semibold px-6 py-2.5 rounded-lg transition-colors"
        >
          {t('verify.verifyAnother')}
        </button>
      </div>
    </div>
  )
}

function DetailRow({ label, value, mono = false }: { label: string; value?: string; mono?: boolean }) {
  if (!value) return null
  return (
    <div className="flex justify-between gap-3 text-sm">
      <span className="text-gray-500 shrink-0">{label}</span>
      <span className={`text-gray-900 text-right truncate ${mono ? 'font-mono text-xs' : ''}`}>{value}</span>
    </div>
  )
}

function tryPrettyPrint(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function extractIssuerDid(raw: string): string | undefined {
  try {
    const obj = JSON.parse(raw)
    return obj.issuer ?? obj.iss
  } catch {
    return undefined
  }
}

function extractSubjectDid(raw: string): string | undefined {
  try {
    const obj = JSON.parse(raw)
    return obj.credentialSubject?.id ?? obj.sub
  } catch {
    return undefined
  }
}

function extractDate(raw: string, field: string): string | undefined {
  try {
    return JSON.parse(raw)[field]
  } catch {
    return undefined
  }
}

function detectType(raw: string): string {
  try {
    const types: string[] = JSON.parse(raw).type ?? []
    return types.filter(t => t !== 'VerifiableCredential').join(', ') || 'VerifiableCredential'
  } catch {
    return raw.includes('~') ? 'SD-JWT' : 'Unknown'
  }
}
