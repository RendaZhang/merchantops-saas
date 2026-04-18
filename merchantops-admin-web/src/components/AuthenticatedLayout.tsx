import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'

import { getContext, isAuthenticationError, logout } from '../lib/api-client'
import { clearAuthSession, readAuthSession } from '../lib/auth-token'
import type { AuthenticatedRouteContext } from './authenticated-route-context'
import { AppShell } from './AppShell'
import { StatusPanel } from './StatusPanel'
import { TenantContextPanel } from './TenantContextPanel'

export function AuthenticatedLayout() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const session = readAuthSession()
  const contextQuery = useQuery({
    queryKey: ['context'],
    queryFn: getContext,
  })
  const clearSessionState = useCallback(() => {
    clearAuthSession()
    queryClient.removeQueries({ queryKey: ['context'] })
    queryClient.removeQueries({ queryKey: ['tickets'] })
  }, [queryClient])
  const signOutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSessionState()
      navigate('/login', { replace: true })
    },
  })
  const handleAuthenticationError = useCallback(
    (error: unknown) => {
      if (!isAuthenticationError(error)) {
        return false
      }

      clearSessionState()
      navigate('/login', {
        replace: true,
        state: { sessionMessage: 'Session ended. Sign in again to continue.' },
      })
      return true
    },
    [clearSessionState, navigate],
  )

  useEffect(() => {
    handleAuthenticationError(contextQuery.error)
  }, [contextQuery.error, handleAuthenticationError])

  const authenticatedRouteContext = {
    handleAuthenticationError,
  } satisfies AuthenticatedRouteContext

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
          <StatusPanel title="Context unavailable" message={contextQuery.error.message} />
        ) : null}

        {contextQuery.data ? (
          <TenantContextPanel context={contextQuery.data} claims={session?.claims ?? null} />
        ) : null}

        <Outlet context={authenticatedRouteContext} />
      </div>
    </AppShell>
  )
}
