import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { translations, type Lang } from './translations'

const STORAGE_KEY = 'verifier-portal-lang'

function detectInitialLang(): Lang {
  if (typeof window === 'undefined') return 'en'
  const saved = window.localStorage.getItem(STORAGE_KEY)
  if (saved === 'en' || saved === 'vi') return saved
  const browserLang = (window.navigator.language || '').toLowerCase()
  return browserLang.startsWith('vi') ? 'vi' : 'en'
}

// Module-level mirror so non-React code (e.g. trustid-client.ts error helpers)
// can resolve the current language without going through React context.
let currentLang: Lang = detectInitialLang()
export function getLang(): Lang {
  return currentLang
}

function resolve(lang: Lang, key: string, params?: Record<string, string | number>): string {
  const parts = key.split('.')
  let val: unknown = translations[lang]
  for (const p of parts) {
    if (val && typeof val === 'object' && p in (val as Record<string, unknown>)) {
      val = (val as Record<string, unknown>)[p]
    } else {
      val = undefined
      break
    }
  }
  let str = typeof val === 'string' ? val : key
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      str = str.replace(new RegExp(`{{\\s*${k}\\s*}}`, 'g'), String(v))
    }
  }
  return str
}

export function tStatic(key: string, params?: Record<string, string | number>): string {
  return resolve(currentLang, key, params)
}

interface I18nContextValue {
  lang: Lang
  setLang: (l: Lang) => void
  t: (key: string, params?: Record<string, string | number>) => string
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(currentLang)

  useEffect(() => {
    currentLang = lang
    window.localStorage.setItem(STORAGE_KEY, lang)
    document.documentElement.lang = lang
  }, [lang])

  const value: I18nContextValue = {
    lang,
    setLang: setLangState,
    t: (key, params) => resolve(lang, key, params),
  }
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useTranslation() {
  const ctx = useContext(I18nContext)
  if (!ctx) throw new Error('useTranslation must be used inside <I18nProvider>')
  return ctx
}
