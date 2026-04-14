import type { ReactNode } from 'react'

const navigationItems = [
  { label: 'Dashboard', state: 'Ready' },
  { label: 'Tickets', state: 'Placeholder' },
  { label: 'Approvals', state: 'Placeholder' },
  { label: 'Imports', state: 'Placeholder' },
  { label: 'AI Interactions', state: 'Placeholder' },
  { label: 'Feature Flags', state: 'Placeholder' },
] as const

type AppShellProps = {
  children: ReactNode
  onClearSession: () => void
}

export function AppShell({ children, onClearSession }: AppShellProps) {
  return (
    <div className="min-h-svh bg-neutral-50 text-neutral-950">
      <div className="grid min-h-svh grid-cols-1 lg:grid-cols-[264px_1fr]">
        <aside className="border-b border-neutral-200 bg-white lg:border-b-0 lg:border-r">
          <div className="flex min-h-20 items-center justify-between gap-4 px-5 lg:min-h-28 lg:flex-col lg:items-start lg:justify-center">
            <div>
              <p className="text-sm font-semibold text-emerald-700">MerchantOps</p>
              <h1 className="text-xl font-semibold text-neutral-950">Admin Console</h1>
            </div>
            <button
              className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950"
              type="button"
              onClick={onClearSession}
            >
              Clear session
            </button>
          </div>

          <nav className="flex gap-2 overflow-x-auto border-t border-neutral-200 px-3 py-3 lg:flex-col lg:overflow-visible lg:px-4">
            {navigationItems.map((item) => {
              const isReady = item.state === 'Ready'

              return (
                <button
                  key={item.label}
                  type="button"
                  disabled={!isReady}
                  className={[
                    'flex min-w-36 items-center justify-between gap-3 rounded-md px-3 py-3 text-left text-sm transition lg:min-w-0',
                    isReady
                      ? 'bg-neutral-950 font-medium text-white'
                      : 'border border-neutral-200 bg-white text-neutral-500',
                  ].join(' ')}
                >
                  <span>{item.label}</span>
                  {!isReady ? (
                    <span className="rounded-full bg-neutral-100 px-2 py-1 text-xs text-neutral-500">
                      Soon
                    </span>
                  ) : null}
                </button>
              )
            })}
          </nav>
        </aside>

        <div className="min-w-0">
          <header className="flex min-h-20 flex-col justify-center gap-2 border-b border-neutral-200 bg-white px-5 md:px-8">
            <p className="text-sm font-medium text-emerald-700">
              Productization Baseline Slice A
            </p>
            <p className="max-w-3xl text-sm text-neutral-600">
              Login and current tenant context are connected. Workflow pages are
              staged as navigation targets.
            </p>
          </header>

          <main className="px-5 py-6 md:px-8 md:py-8">{children}</main>
        </div>
      </div>
    </div>
  )
}
