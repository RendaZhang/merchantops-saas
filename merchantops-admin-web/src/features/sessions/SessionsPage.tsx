import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import { getAuthSessions, isAuthenticationError } from '../../lib/api-client'
import type { AuthSessionListItem } from '../../lib/schemas'

const authSessionsQueryKey = ['auth-sessions'] as const

export function SessionsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const authSessionsQuery = useQuery({
    queryKey: authSessionsQueryKey,
    queryFn: getAuthSessions,
  })

  useEffect(() => {
    handleAuthenticationError(authSessionsQuery.error)
  }, [authSessionsQuery.error, handleAuthenticationError])

  if (authSessionsQuery.isPending) {
    return (
      <StatusPanel
        title="Loading sessions"
        message="Fetching current-user session inventory."
      />
    )
  }

  if (authSessionsQuery.error && !isAuthenticationError(authSessionsQuery.error)) {
    return (
      <StatusPanel
        title="Sessions unavailable"
        message={authSessionsQuery.error.message}
      />
    )
  }

  if (!authSessionsQuery.data || authSessionsQuery.data.items.length === 0) {
    return (
      <section className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-emerald-700">Sessions</p>
        <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
          Current-user inventory
        </h2>
        <p className="mt-4 text-sm text-neutral-600">
          No sessions were returned for the current user.
        </p>
      </section>
    )
  }

  const sessions = authSessionsQuery.data.items

  return (
    <section className="rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Sessions</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Current-user inventory
          </h2>
        </div>
        <p className="text-sm text-neutral-600">
          Showing {formatSessionCount(sessions.length)}
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-[860px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[18%] px-5 py-3">Session</th>
              <th className="w-[16%] px-5 py-3">Status</th>
              <th className="w-[22%] px-5 py-3">Created</th>
              <th className="w-[22%] px-5 py-3">Expires</th>
              <th className="w-[22%] px-5 py-3">Revoked</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {sessions.map((session, index) => (
              <SessionRow
                key={buildDisplayRowKey(session, index)}
                session={session}
              />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function SessionRow({ session }: { session: AuthSessionListItem }) {
  return (
    <tr>
      <td className="px-5 py-4 align-top">
        <span
          className={[
            'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
            session.currentSession
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-neutral-200 bg-neutral-50 text-neutral-600',
          ].join(' ')}
        >
          {session.currentSession ? 'Current' : 'Other'}
        </span>
      </td>
      <td className="px-5 py-4 align-top">
        <span className={getStatusBadgeClassName(session.status)}>
          {session.status}
        </span>
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(session.createdAt)}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(session.expiresAt)}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatOptionalDateTime(session.revokedAt)}
      </td>
    </tr>
  )
}

function buildDisplayRowKey(session: AuthSessionListItem, index: number): string {
  return [
    index,
    session.currentSession,
    session.status,
    session.createdAt,
    session.expiresAt,
    session.revokedAt ?? 'not-revoked',
  ].join(':')
}

function formatSessionCount(count: number): string {
  return count === 1 ? '1 session' : `${count} sessions`
}

function getStatusBadgeClassName(status: string): string {
  const baseClassName = 'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium'

  if (status === 'ACTIVE') {
    return `${baseClassName} border-emerald-200 bg-emerald-50 text-emerald-700`
  }

  if (status === 'REVOKED') {
    return `${baseClassName} border-rose-200 bg-rose-50 text-rose-700`
  }

  if (status === 'EXPIRED') {
    return `${baseClassName} border-amber-200 bg-amber-50 text-amber-800`
  }

  return `${baseClassName} border-neutral-200 bg-neutral-50 text-neutral-700`
}

function formatOptionalDateTime(value: string | null): string {
  if (!value) {
    return 'Not revoked'
  }

  return formatDateTime(value)
}

function formatDateTime(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ')
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
