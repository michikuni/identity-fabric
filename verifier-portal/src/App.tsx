import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Home from './pages/Home'
import VerifyResult from './pages/VerifyResult'
import TrustRegistry from './pages/TrustRegistry'
import { ErrorBoundary } from './components/ErrorBoundary'

export default function App() {
  return (
    <ErrorBoundary>
    <BrowserRouter>
      <div className="min-h-screen flex flex-col">
        <header className="bg-brand-700 text-white shadow-md">
          <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center text-lg font-bold">
                🔐
              </div>
              <span className="text-lg font-semibold tracking-tight">TrustID Verifier Portal</span>
              <span className="text-xs bg-white/20 rounded px-2 py-0.5">Independent</span>
            </div>
            <nav className="flex gap-6 text-sm font-medium">
              <NavLink
                to="/"
                end
                className={({ isActive }) =>
                  isActive ? 'text-white underline underline-offset-4' : 'text-white/70 hover:text-white'
                }
              >
                Verify
              </NavLink>
              <NavLink
                to="/trust-registry"
                className={({ isActive }) =>
                  isActive ? 'text-white underline underline-offset-4' : 'text-white/70 hover:text-white'
                }
              >
                Trust Registry
              </NavLink>
            </nav>
          </div>
        </header>

        <main className="flex-1 max-w-5xl mx-auto w-full px-4 py-8">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/verify" element={<VerifyResult />} />
            <Route path="/trust-registry" element={<TrustRegistry />} />
          </Routes>
        </main>

        <footer className="border-t text-center text-xs text-gray-400 py-3">
          TrustID Verifier Portal — operates independently from the Holder app · W3C VC · DIF Universal Resolver
        </footer>
      </div>
    </BrowserRouter>
    </ErrorBoundary>
  )
}
