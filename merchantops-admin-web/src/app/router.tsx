import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AuthenticatedLayout } from '../components/AuthenticatedLayout'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { AiInteractionsPage } from '../features/ai-interactions/AiInteractionsPage'
import { ApprovalRequestDetailPage } from '../features/approvals/ApprovalRequestDetailPage'
import { ApprovalsPage } from '../features/approvals/ApprovalsPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { LoginPage } from '../features/auth/LoginPage'
import { FeatureFlagsPage } from '../features/feature-flags/FeatureFlagsPage'
import { ImportJobDetailPage } from '../features/imports/ImportJobDetailPage'
import { ImportsPage } from '../features/imports/ImportsPage'
import { TicketDetailPage } from '../features/tickets/TicketDetailPage'
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
            path: 'tickets/:id',
            element: <TicketDetailPage />,
          },
          {
            path: 'feature-flags',
            element: <FeatureFlagsPage />,
          },
          {
            path: 'imports',
            element: <ImportsPage />,
          },
          {
            path: 'imports/:id',
            element: <ImportJobDetailPage />,
          },
          {
            path: 'approvals',
            element: <ApprovalsPage />,
          },
          {
            path: 'approvals/:id',
            element: <ApprovalRequestDetailPage />,
          },
          {
            path: 'ai-interactions',
            element: <AiInteractionsPage />,
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
