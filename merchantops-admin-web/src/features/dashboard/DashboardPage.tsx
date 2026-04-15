import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

import { AppShell } from '../../components/AppShell'
import { TenantContextPanel } from '../../components/TenantContextPanel'
import { getContext, isAuthenticationError, logout } from '../../lib/api-client'
import { clearAuthSession, readAuthSession } from '../../lib/auth-token'

const workflowPlaceholders = [
  {
    title: 'Tickets',
    description: 'Ticket queue, detail, comments, and workflow logs.',
  },
  {
    title: 'Approvals',
    description: 'Human-reviewed user, import, and ticket proposal decisions.',
  },
  {
    title: 'Imports',
    description: 'USER_CSV imports, error pages, and replay paths.',
  },
  {
    title: 'AI Interactions',
    description: 'Suggestion history and tenant usage summary entry points.',
  },
  {
    title: 'Feature Flags',
    description: 'Tenant-scoped rollout controls for AI and workflow bridges.',
  },
] as const

export function DashboardPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const session = readAuthSession()
  const contextQuery = useQuery({
    queryKey: ['context'],
    queryFn: getContext,
  })
  const signOutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearAuthSession()
      queryClient.removeQueries({ queryKey: ['context'] })
      navigate('/login', { replace: true })
    },
  })

  useEffect(() => {
    if (isAuthenticationError(contextQuery.error)) {
      clearAuthSession()
      queryClient.removeQueries({ queryKey: ['context'] })
      navigate('/login', {
        replace: true,
        state: { sessionMessage: 'Session ended. Sign in again to continue.' },
      })
    }
  }, [contextQuery.error, navigate, queryClient])

  return (
    <AppShell
      onSignOut={() => signOutMutation.mutate()}
      signOutPending={signOutMutation.isPending}
    >
      <div className="grid gap-6">
        {contextQuery.isPending ? (
          <StatusPanel title="Loading context" message="Checking the current tenant." />
        ) : null}

        {contextQuery.error && !isAuthenticationError(contextQuery.error) ? (
          <StatusPanel
            title="Context unavailable"
            message={contextQuery.error.message}
          />
        ) : null}

        {contextQuery.data ? (
          <TenantContextPanel context={contextQuery.data} claims={session?.claims ?? null} />
        ) : null}

        <section>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-sm font-medium text-emerald-700">Navigation targets</p>
              <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
                Workflow placeholders
              </h2>
            </div>
            <p className="max-w-xl text-sm text-neutral-600">
              Slice B keeps the shell thin while adding revocable sign-out.
              Workflow data screens stay behind later slices.
            </p>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {workflowPlaceholders.map((item) => (
              <article
                key={item.title}
                className="min-h-36 rounded-lg border border-neutral-200 bg-white p-5"
              >
                <div className="flex items-start justify-between gap-3">
                  <h3 className="text-lg font-semibold text-neutral-950">{item.title}</h3>
                  <span className="rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-600">
                    Placeholder
                  </span>
                </div>
                <p className="mt-4 text-sm leading-6 text-neutral-600">
                  {item.description}
                </p>
              </article>
            ))}
          </div>
        </section>
      </div>
    </AppShell>
  )
}

function StatusPanel({ title, message }: { title: string; message: string }) {
  return (
    <section className="rounded-lg border border-neutral-200 bg-white p-5">
      <h2 className="text-lg font-semibold text-neutral-950">{title}</h2>
      <p className="mt-2 text-sm text-neutral-600">{message}</p>
    </section>
  )
}
