import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AuthenticatedLayout } from '../components/AuthenticatedLayout'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { LoginPage } from '../features/auth/LoginPage'
import { FeatureFlagsPage } from '../features/feature-flags/FeatureFlagsPage'
import { ImportsPage } from '../features/imports/ImportsPage'
import { TicketsPage } from '../features/tickets/TicketsPage'

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AuthenticatedLayout />,
        children: [
          {
            index: true,
            element: <DashboardPage />,
          },
          {
            path: 'tickets',
            element: <TicketsPage />,
          },
          {
            path: 'feature-flags',
            element: <FeatureFlagsPage />,
          },
          {
            path: 'imports',
            element: <ImportsPage />,
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])
