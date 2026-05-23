import { useQuery } from '@tanstack/react-query'
import { listTrustedIssuers, type TrustedIssuer } from '../lib/trustid-client'
import { useTranslation } from '../i18n/I18nContext'

export default function TrustRegistry() {
  const { t, lang } = useTranslation()
  const { data: issuers, isLoading, error, refetch } = useQuery({
    queryKey: ['trust-registry'],
    queryFn: listTrustedIssuers,
  })

  const dateLocale = lang === 'vi' ? 'vi-VN' : 'en-US'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
              {t('trustRegistry.title')}
            </h1>
            <p className="text-sm text-gray-500 mt-1">{t('trustRegistry.subtitle')}</p>
          </div>
          <button
            onClick={() => refetch()}
            className="shrink-0 text-sm text-brand-600 hover:text-brand-800 border border-brand-200 rounded-lg px-3 py-1.5 transition-colors"
          >
            {t('common.refresh')}
          </button>
        </div>
      </div>

      {/* Badges */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[
          { label: t('trustRegistry.statTotal'),      value: issuers?.length ?? '—' },
          { label: t('trustRegistry.statActive'),     value: issuers?.filter(i => i.active).length ?? '—' },
          { label: t('trustRegistry.statRevoked'),    value: issuers?.filter(i => !i.active).length ?? '—' },
          { label: t('trustRegistry.statBlockchain'), value: 'Fabric' },
        ].map(stat => (
          <div key={stat.label} className="bg-white rounded-xl border border-gray-200 p-4 text-center shadow-sm">
            <p className="text-2xl font-bold text-brand-700">{String(stat.value)}</p>
            <p className="text-xs text-gray-500 mt-1">{stat.label}</p>
          </div>
        ))}
      </div>

      {/* Issuers table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-400 animate-pulse">{t('trustRegistry.loading')}</div>
        ) : error ? (
          <div className="p-8 text-center text-red-400 space-y-2">
            <p className="font-medium">{t('trustRegistry.loadFailed')}</p>
            <p className="text-xs text-red-300">{error instanceof Error ? error.message : t('common.unexpectedError')}</p>
            <button
              onClick={() => refetch()}
              className="mt-2 text-xs text-red-400 underline hover:text-red-600"
            >
              {t('common.retry')}
            </button>
          </div>
        ) : !issuers?.length ? (
          <div className="p-8 text-center text-gray-400">{t('trustRegistry.empty')}</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {[
                  t('trustRegistry.colIssuerDid'),
                  t('trustRegistry.colName'),
                  t('trustRegistry.colRole'),
                  t('trustRegistry.colScope'),
                  t('trustRegistry.colStatus'),
                  t('trustRegistry.colRegistered'),
                ].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {issuers.map((issuer: TrustedIssuer) => (
                <tr key={issuer.did} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-gray-700 max-w-[200px] truncate">{issuer.did}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{issuer.name}</td>
                  <td className="px-4 py-3 text-gray-600">{issuer.role}</td>
                  <td className="px-4 py-3 text-gray-600">{issuer.scope}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
                      issuer.active
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {issuer.active ? t('trustRegistry.statusActive') : t('trustRegistry.statusRevoked')}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500 text-xs">
                    {issuer.registeredAt ? new Date(issuer.registeredAt).toLocaleDateString(dateLocale) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Explanation */}
      <div className="bg-brand-50 rounded-xl border border-brand-100 p-5 text-sm text-brand-900 space-y-2">
        <p className="font-semibold">{t('trustRegistry.howItWorksTitle')}</p>
        <ol className="list-decimal list-inside space-y-1 text-brand-800">
          <li>{t('trustRegistry.step1')}</li>
          <li>
            {t('trustRegistry.step2Prefix')}
            <code className="bg-brand-100 px-1 rounded">issuer</code>
            {t('trustRegistry.step2Suffix')}
          </li>
          <li>
            {t('trustRegistry.step3Prefix')}
            <code className="bg-brand-100 px-1 rounded">GET /1.0/identifiers/&#123;did&#125;</code>
            {t('trustRegistry.step3Suffix')}
          </li>
          <li>
            {t('trustRegistry.step4Prefix')}
            <code className="bg-brand-100 px-1 rounded">GET /api/v1/trust-registry/issuers</code>
            {t('trustRegistry.step4Suffix')}
          </li>
          <li>{t('trustRegistry.step5')}</li>
        </ol>
      </div>
    </div>
  )
}
