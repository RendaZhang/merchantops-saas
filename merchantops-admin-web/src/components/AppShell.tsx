import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'

const readyNavigationItems = [
  { label: 'Dashboard', path: '/' },
  { label: 'Tickets', path: '/tickets' },
] as const

const placeholderNavigationItems = [
  { label: 'Approvals', state: 'Placeholder' },
  { label: 'Imports', state: 'Placeholder' },
  { label: 'AI Interactions', state: 'Placeholder' },
  { label: 'Feature Flags', state: 'Placeholder' },
] as const

type AppShellProps = {
  children: ReactNode
  onSignOut: () => void
  onSignOutAll: () => void
  signOutPending?: boolean
  signOutAllPending?: boolean
}

export function AppShell({
  children,
  onSignOut,
  onSignOutAll,
  signOutPending = false,
  signOutAllPending = false,
}: AppShellProps) {
  return (
    <div className="min-h-svh bg-neutral-50 text-neutral-950">
      <div className="grid min-h-svh grid-cols-1 lg:grid-cols-[264px_1fr]">
        <aside className="border-b border-neutral-200 bg-white lg:border-b-0 lg:border-r">
          <div className="flex min-h-20 items-center justify-between gap-4 px-5 lg:min-h-28 lg:flex-col lg:items-start lg:justify-center">
            <div>
              <p className="text-sm font-semibold text-emerald-700">MerchantOps</p>
              <h1 className="text-xl font-semibold text-neutral-950">Admin Console</h1>
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950 disabled:border-neutral-200 disabled:text-neutral-400"
                type="button"
                onClick={onSignOut}
                disabled={signOutPending}
              >
                {signOutPending && !signOutAllPending ? 'Signing out...' : 'Sign out'}
              </button>
              <button
                className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950 disabled:border-neutral-200 disabled:text-neutral-400"
                type="button"
                onClick={onSignOutAll}
                disabled={signOutPending}
              >
                {signOutAllPending ? 'Signing out all...' : 'Sign out all sessions'}
              </button>
            </div>
          </div>

          <nav className="flex gap-2 overflow-x-auto border-t border-neutral-200 px-3 py-3 lg:flex-col lg:overflow-visible lg:px-4">
            {readyNavigationItems.map((item) => (
              <NavLink
                key={item.label}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) =>
                  [
                    'flex min-w-36 items-center justify-between gap-3 rounded-md px-3 py-3 text-left text-sm transition lg:min-w-0',
                    isActive
                      ? 'bg-neutral-950 font-medium text-white'
                      : 'border border-neutral-200 bg-white text-neutral-700 hover:border-neutral-400 hover:text-neutral-950',
                  ].join(' ')
                }
              >
                <span>{item.label}</span>
              </NavLink>
            ))}

            {placeholderNavigationItems.map((item) => (
              <button
                key={item.label}
                type="button"
                disabled
                className="flex min-w-36 items-center justify-between gap-3 rounded-md border border-neutral-200 bg-white px-3 py-3 text-left text-sm text-neutral-500 transition lg:min-w-0"
              >
                <span>{item.label}</span>
                <span className="rounded-full bg-neutral-100 px-2 py-1 text-xs text-neutral-500">
                  Soon
                </span>
              </button>
            ))}
          </nav>
        </aside>

        <div className="min-w-0">
          <header className="flex min-h-20 flex-col justify-center gap-2 border-b border-neutral-200 bg-white px-5 md:px-8">
            <p className="text-sm font-medium text-emerald-700">Productization Baseline</p>
            <p className="max-w-3xl text-sm text-neutral-600">
              Same-origin runtime, current tenant context, sign out, and the
              read-only ticket queue are connected.
            </p>
          </header>

          <main className="px-5 py-6 md:px-8 md:py-8">{children}</main>
        </div>
      </div>
    </div>
  )
}
