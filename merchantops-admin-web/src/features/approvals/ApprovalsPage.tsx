import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import { getApprovalRequests, isAuthenticationError } from '../../lib/api-client'
import type { ApprovalRequestListItem } from '../../lib/schemas'

const approvalRequestsPageRequest = { page: 0, size: 10 } as const

export function ApprovalsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const approvalRequestsQuery = useQuery({
    queryKey: ['approval-requests', approvalRequestsPageRequest],
    queryFn: () => getApprovalRequests(approvalRequestsPageRequest),
  })

  useEffect(() => {
    handleAuthenticationError(approvalRequestsQuery.error)
  }, [approvalRequestsQuery.error, handleAuthenticationError])

  if (approvalRequestsQuery.isPending) {
    return (
      <StatusPanel
        title="Loading approvals"
        message="Fetching the current approval queue."
      />
    )
  }

  if (
    approvalRequestsQuery.error &&
    !isAuthenticationError(approvalRequestsQuery.error)
  ) {
    return (
      <StatusPanel
        title="Approvals unavailable"
        message={approvalRequestsQuery.error.message}
      />
    )
  }

  if (!approvalRequestsQuery.data || approvalRequestsQuery.data.items.length === 0) {
    return (
      <section className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-emerald-700">Approvals</p>
        <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
          Current queue
        </h2>
        <p className="mt-4 text-sm text-neutral-600">
          No approval requests were returned for the current tenant.
        </p>
      </section>
    )
  }

  const page = approvalRequestsQuery.data

  return (
    <section className="rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Approvals</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Current queue
          </h2>
        </div>
        <p className="text-sm text-neutral-600">
          Showing {page.items.length} of {page.total}
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-[1180px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[10%] px-5 py-3">Request</th>
              <th className="w-[18%] px-5 py-3">Action</th>
              <th className="w-[16%] px-5 py-3">Entity</th>
              <th className="w-[12%] px-5 py-3">Status</th>
              <th className="w-[11%] px-5 py-3">Requested by</th>
              <th className="w-[11%] px-5 py-3">Reviewed by</th>
              <th className="w-[11%] px-5 py-3">Created</th>
              <th className="w-[11%] px-5 py-3">Review / execution</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {page.items.map((approvalRequest) => (
              <ApprovalRequestRow
                key={approvalRequest.id}
                approvalRequest={approvalRequest}
              />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function ApprovalRequestRow({
  approvalRequest,
}: {
  approvalRequest: ApprovalRequestListItem
}) {
  return (
    <tr>
      <td className="px-5 py-4 align-top">
        <p className="font-medium text-neutral-950">#{approvalRequest.id}</p>
      </td>
      <td className="px-5 py-4 align-top">
        <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
          {approvalRequest.actionType}
        </code>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        <p className="font-medium text-neutral-900">{approvalRequest.entityType}</p>
        <p className="mt-1 text-xs text-neutral-500">#{approvalRequest.entityId}</p>
      </td>
      <td className="px-5 py-4 align-top">
        <span className="inline-flex rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-700">
          {approvalRequest.status}
        </span>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        #{approvalRequest.requestedBy}
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        {formatOptionalUser(approvalRequest.reviewedBy)}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(approvalRequest.createdAt)}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        <p>{formatOptionalDateTime(approvalRequest.reviewedAt, 'Not reviewed')}</p>
        <p className="mt-2 text-xs text-neutral-500">
          Executed: {formatOptionalDateTime(approvalRequest.executedAt, 'Not executed')}
        </p>
      </td>
    </tr>
  )
}

function formatOptionalUser(value: number | null): string {
  if (value === null) {
    return 'Not reviewed'
  }

  return `#${value}`
}

function formatOptionalDateTime(value: string | null, fallback: string): string {
  if (!value) {
    return fallback
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
