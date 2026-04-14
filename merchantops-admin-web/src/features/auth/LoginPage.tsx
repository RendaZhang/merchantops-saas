import { useMutation, useQueryClient } from '@tanstack/react-query'
import { type FormEvent, useMemo, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'

import { login } from '../../lib/api-client'
import { readAuthSession, saveAuthSession } from '../../lib/auth-token'
import type { LoginRequest } from '../../lib/schemas'

const warehouseImageUrl =
  'https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Warehouse_distribution-center-1136510.jpg/1280px-Warehouse_distribution-center-1136510.jpg'

export function LoginPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const existingSession = readAuthSession()
  const [credentials, setCredentials] = useState<LoginRequest>({
    tenantCode: 'demo-shop',
    username: 'admin',
    password: '',
  })

  const stateMessage = useMemo(() => {
    const state = location.state

    if (
      typeof state === 'object' &&
      state !== null &&
      'sessionMessage' in state &&
      typeof state.sessionMessage === 'string'
    ) {
      return state.sessionMessage
    }

    return null
  }, [location.state])

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (response) => {
      saveAuthSession(response)
      queryClient.removeQueries({ queryKey: ['context'] })
      navigate('/', { replace: true })
    },
  })

  if (existingSession) {
    return <Navigate to="/" replace />
  }

  function updateCredential(field: keyof LoginRequest, value: string) {
    setCredentials((currentCredentials) => ({
      ...currentCredentials,
      [field]: value,
    }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    loginMutation.mutate(credentials)
  }

  return (
    <main className="grid min-h-svh bg-neutral-50 text-neutral-950 lg:grid-cols-[minmax(0,0.9fr)_minmax(420px,1fr)]">
      <section className="relative hidden min-h-svh overflow-hidden lg:block">
        <img
          className="h-full w-full object-cover"
          src={warehouseImageUrl}
          alt="Warehouse operations floor"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-neutral-950/70 via-neutral-950/20 to-transparent" />
        <div className="absolute bottom-6 left-6 right-6 text-white">
          <p className="text-sm font-medium">Inventory, approvals, and AI review</p>
          <p className="mt-2 max-w-lg text-3xl font-semibold leading-tight">
            A thin operator entry for the current backend baseline.
          </p>
        </div>
      </section>

      <section className="flex items-center justify-center px-5 py-10">
        <div className="w-full max-w-md rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
          <p className="text-sm font-semibold text-emerald-700">MerchantOps</p>
          <h1 className="mt-3 text-3xl font-semibold text-neutral-950">Sign in</h1>
          <p className="mt-3 text-sm text-neutral-600">
            Use a tenant-scoped demo account to open the admin console.
          </p>

          {stateMessage ? (
            <p className="mt-5 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {stateMessage}
            </p>
          ) : null}

          {loginMutation.error ? (
            <p className="mt-5 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-900">
              {loginMutation.error.message}
            </p>
          ) : null}

          <form className="mt-6 grid gap-4" onSubmit={handleSubmit}>
            <label className="grid gap-2" htmlFor="tenantCode">
              <span className="text-sm font-medium text-neutral-700">Tenant code</span>
              <input
                className="h-11 rounded-md border border-neutral-300 px-3 text-neutral-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                id="tenantCode"
                name="tenantCode"
                value={credentials.tenantCode}
                onChange={(event) => updateCredential('tenantCode', event.target.value)}
                autoComplete="organization"
                required
              />
            </label>

            <label className="grid gap-2" htmlFor="username">
              <span className="text-sm font-medium text-neutral-700">Username</span>
              <input
                className="h-11 rounded-md border border-neutral-300 px-3 text-neutral-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                id="username"
                name="username"
                value={credentials.username}
                onChange={(event) => updateCredential('username', event.target.value)}
                autoComplete="username"
                required
              />
            </label>

            <label className="grid gap-2" htmlFor="password">
              <span className="text-sm font-medium text-neutral-700">Password</span>
              <input
                className="h-11 rounded-md border border-neutral-300 px-3 text-neutral-950 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
                id="password"
                name="password"
                value={credentials.password}
                onChange={(event) => updateCredential('password', event.target.value)}
                autoComplete="current-password"
                type="password"
                required
              />
            </label>

            <button
              className="mt-2 h-11 rounded-md bg-neutral-950 px-4 text-sm font-semibold text-white transition hover:bg-neutral-800 disabled:bg-neutral-400"
              type="submit"
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <p className="mt-5 text-xs text-neutral-500">
            Demo path: tenant <span className="font-medium">demo-shop</span>,
            username <span className="font-medium">admin</span>, password{' '}
            <span className="font-medium">123456</span>.
          </p>
        </div>
      </section>
    </main>
  )
}
