import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { readAuthSession } from '../lib/auth-token'

export function ProtectedRoute() {
  const location = useLocation()
  const session = readAuthSession()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
