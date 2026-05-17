import { Component, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  message: string
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, message: '' }
  }

  static getDerivedStateFromError(error: unknown): State {
    const message = error instanceof Error ? error.message : 'An unexpected error occurred.'
    return { hasError: true, message }
  }

  override render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <div className="max-w-md w-full bg-white rounded-2xl border border-red-200 shadow-sm p-8 text-center space-y-4">
            <div className="text-5xl">⚠️</div>
            <h2 className="text-lg font-semibold text-gray-900">Something went wrong</h2>
            <p className="text-sm text-gray-500">{this.state.message}</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-2 bg-brand-600 hover:bg-brand-700 text-white font-medium px-5 py-2 rounded-lg transition-colors text-sm"
            >
              Reload page
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
