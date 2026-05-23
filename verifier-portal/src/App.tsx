import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Home from './pages/Home'
import VerifyResult from './pages/VerifyResult'
import TrustRegistry from './pages/TrustRegistry'
import { ErrorBoundary } from './components/ErrorBoundary'
import { useTranslation } from './i18n/I18nContext'

function LanguageSwitcher() {
  const { lang, setLang, t } = useTranslation()
  const next = lang === 'en' ? 'vi' : 'en'
  const label = lang === 'en' ? t('header.switchToVi') : t('header.switchToEn')
  return (
    <button
      onClick={() => setLang(next)}
      className="text-xs bg-white/15 hover:bg-white/25 transition-colors rounded px-2.5 py-1 font-medium uppercase tracking-wide"
      aria-label={label}
      title={label}
    >
      {next.toUpperCase()}
    </button>
  )
}

function Header() {
  const { t } = useTranslation()
  return (
    <header className="bg-brand-700 text-white shadow-md">
      <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center text-lg font-bold">
            🔐
          </div>
          <span className="text-lg font-semibold tracking-tight">{t('header.title')}</span>
          <span className="text-xs bg-white/20 rounded px-2 py-0.5">{t('header.badge')}</span>
        </div>
        <div className="flex items-center gap-5">
          <nav className="flex gap-6 text-sm font-medium">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                isActive ? 'text-white underline underline-offset-4' : 'text-white/70 hover:text-white'
              }
            >
              {t('header.navVerify')}
            </NavLink>
            <NavLink
              to="/trust-registry"
              className={({ isActive }) =>
                isActive ? 'text-white underline underline-offset-4' : 'text-white/70 hover:text-white'
              }
            >
              {t('header.navTrustRegistry')}
            </NavLink>
          </nav>
          <LanguageSwitcher />
        </div>
      </div>
    </header>
  )
}

function Footer() {
  const { t } = useTranslation()
  return (
    <footer className="border-t text-center text-xs text-gray-400 py-3">
      {t('footer.tagline')}
    </footer>
  )
}

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <div className="min-h-screen flex flex-col">
          <Header />
          <main className="flex-1 max-w-5xl mx-auto w-full px-4 py-8">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/verify" element={<VerifyResult />} />
              <Route path="/trust-registry" element={<TrustRegistry />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
