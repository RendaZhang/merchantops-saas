import { useQuery, useQueryClient } from '@tanstack/react-query'
import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { getApprovalRequests, isAuthenticationError } from '../../lib/api-client'
import type { ApprovalRequestListItem } from '../../lib/schemas'

const approvalPageRequestBase = { page: 0, size: 10 } as const
const approvalStatusOptions = ['PENDING', 'APPROVED', 'REJECTED'] as const
const approvalActionTypeOptions = [
  'USER_STATUS_DISABLE',
  'IMPORT_JOB_SELECTIVE_REPLAY',
  'TICKET_COMMENT_CREATE',
] as const

type ApprovalFilters = {
  status?: string
  actionType?: string
  requestedBy?: number
}

type ApprovalFilterDraft = {
  status: string
  actionType: string
  requestedBy: string
}

const emptyApprovalFilterDraft: ApprovalFilterDraft = {
  status: '',
  actionType: '',
  requestedBy: '',
}

export function ApprovalsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const queryClient = useQueryClient()
  const [filterDraft, setFilterDraft] = useState<ApprovalFilterDraft>(
    emptyApprovalFilterDraft,
  )
  const [appliedFilters, setAppliedFilters] = useState<ApprovalFilters>({})
  const [requestedByError, setRequestedByError] = useState<string | null>(null)
  const approvalRequestsPageRequest = useMemo(
    () => ({
      ...approvalPageRequestBase,
      ...appliedFilters,
    }),
    [appliedFilters],
  )
  const approvalRequestsQuery = useQuery({
    queryKey: ['approval-requests', approvalRequestsPageRequest],
    queryFn: () => getApprovalRequests(approvalRequestsPageRequest),
  })

  useEffect(() => {
    handleAuthenticationError(approvalRequestsQuery.error)
  }, [approvalRequestsQuery.error, handleAuthenticationError])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const normalizedFilters = normalizeApprovalFilters(filterDraft)
    if (normalizedFilters.error) {
      setRequestedByError(normalizedFilters.error)
      return
    }

    setRequestedByError(null)
    setAppliedFilters(normalizedFilters.filters)
  }

  function clearFilters() {
    setFilterDraft(emptyApprovalFilterDraft)
    setRequestedByError(null)
    queryClient.removeQueries({
      queryKey: ['approval-requests', approvalPageRequestBase],
      exact: true,
    })
    setAppliedFilters({})
  }

  const page = approvalRequestsQuery.data ?? null
  const hasActiveFilters = Object.keys(appliedFilters).length > 0

  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Approvals</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Current queue
          </h2>
        </div>
        {page ? (
          <p className="text-sm text-neutral-600">
            Showing {page.items.length} of {page.total}
          </p>
        ) : null}
      </div>

      <ApprovalFiltersForm
        draft={filterDraft}
        requestedByError={requestedByError}
        onApply={applyFilters}
        onClear={clearFilters}
        onDraftChange={setFilterDraft}
        onRequestedByChange={() => setRequestedByError(null)}
      />

      <ApprovalQueueContent
        error={approvalRequestsQuery.error}
        hasActiveFilters={hasActiveFilters}
        isPending={approvalRequestsQuery.isPending}
        page={page}
      />
    </section>
  )
}

function ApprovalFiltersForm({
  draft,
  requestedByError,
  onApply,
  onClear,
  onDraftChange,
  onRequestedByChange,
}: {
  draft: ApprovalFilterDraft
  requestedByError: string | null
  onApply: (event: FormEvent<HTMLFormElement>) => void
  onClear: () => void
  onDraftChange: (draft: ApprovalFilterDraft) => void
  onRequestedByChange: () => void
}) {
  return (
    <form
      className="grid min-w-0 gap-4 border-b border-neutral-200 bg-neutral-50 p-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)_minmax(0,1fr)_auto]"
      noValidate
      onSubmit={onApply}
    >
      <label className="grid min-w-0 gap-2 text-sm font-medium text-neutral-700">
        <span>Status</span>
        <select
          value={draft.status}
          onChange={(event) =>
            onDraftChange({
              ...draft,
              status: event.target.value,
            })
          }
          className="h-10 w-full rounded-md border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
        >
          <option value="">All statuses</option>
          {approvalStatusOptions.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </label>

      <label className="grid min-w-0 gap-2 text-sm font-medium text-neutral-700">
        <span>Action type</span>
        <select
          value={draft.actionType}
          onChange={(event) =>
            onDraftChange({
              ...draft,
              actionType: event.target.value,
            })
          }
          className="h-10 w-full rounded-md border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
        >
          <option value="">All action types</option>
          {approvalActionTypeOptions.map((actionType) => (
            <option key={actionType} value={actionType}>
              {actionType}
            </option>
          ))}
        </select>
      </label>

      <label className="grid min-w-0 gap-2 text-sm font-medium text-neutral-700">
        <span>Requested by</span>
        <input
          type="text"
          inputMode="numeric"
          value={draft.requestedBy}
          onChange={(event) => {
            onRequestedByChange()
            onDraftChange({
              ...draft,
              requestedBy: event.target.value,
            })
          }}
          aria-invalid={requestedByError ? 'true' : undefined}
          aria-describedby={requestedByError ? 'approval-requested-by-error' : undefined}
          placeholder="User id"
          className="h-10 w-full rounded-md border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition placeholder:text-neutral-400 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100 aria-invalid:border-rose-400 aria-invalid:focus:border-rose-500 aria-invalid:focus:ring-rose-100"
        />
        {requestedByError ? (
          <span
            id="approval-requested-by-error"
            className="text-xs font-medium text-rose-700"
          >
            {requestedByError}
          </span>
        ) : null}
      </label>

      <div className="flex flex-wrap items-end gap-2">
        <button
          type="submit"
          className="h-10 rounded-md bg-emerald-700 px-4 text-sm font-medium text-white transition hover:bg-emerald-800 focus:outline-none focus:ring-2 focus:ring-emerald-200"
        >
          Apply
        </button>
        <button
          type="button"
          onClick={onClear}
          className="h-10 rounded-md border border-neutral-300 px-4 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950 focus:outline-none focus:ring-2 focus:ring-neutral-200"
        >
          Clear
        </button>
      </div>
    </form>
  )
}

function ApprovalQueueContent({
  error,
  hasActiveFilters,
  isPending,
  page,
}: {
  error: Error | null
  hasActiveFilters: boolean
  isPending: boolean
  page: { items: ApprovalRequestListItem[]; total: number } | null
}) {
  if (isPending) {
    return (
      <div className="p-5">
        <p className="text-sm font-medium text-neutral-900">Loading approvals</p>
        <p className="mt-2 text-sm text-neutral-600">
          Fetching the current approval queue.
        </p>
      </div>
    )
  }

  if (error && !isAuthenticationError(error)) {
    return (
      <div className="p-5">
        <p className="text-sm font-medium text-neutral-900">Approvals unavailable</p>
        <p className="mt-2 text-sm text-neutral-600">{error.message}</p>
      </div>
    )
  }

  if (!page || page.items.length === 0) {
    return (
      <div className="p-5">
        <p className="text-sm text-neutral-600">
          {hasActiveFilters
            ? 'No approval requests match the current filters.'
            : 'No approval requests were returned for the current tenant.'}
        </p>
      </div>
    )
  }

  return (
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
  )
}

function normalizeApprovalFilters(draft: ApprovalFilterDraft):
  | { filters: ApprovalFilters; error: null }
  | { filters: ApprovalFilters; error: string } {
  const filters: ApprovalFilters = {}
  const normalizedStatus = draft.status.trim()
  const normalizedActionType = draft.actionType.trim()
  const normalizedRequestedBy = draft.requestedBy.trim()

  if (normalizedStatus) {
    filters.status = normalizedStatus
  }

  if (normalizedActionType) {
    filters.actionType = normalizedActionType
  }

  if (!normalizedRequestedBy) {
    return { filters, error: null }
  }

  if (!/^\d+$/.test(normalizedRequestedBy)) {
    return {
      filters,
      error: 'Requested by must be a positive whole-number user id.',
    }
  }

  const requestedBy = Number(normalizedRequestedBy)

  if (!Number.isSafeInteger(requestedBy) || requestedBy <= 0) {
    return {
      filters,
      error: 'Requested by must be a positive whole-number user id.',
    }
  }

  filters.requestedBy = requestedBy
  return { filters, error: null }
}

function ApprovalRequestRow({
  approvalRequest,
}: {
  approvalRequest: ApprovalRequestListItem
}) {
  return (
    <tr>
      <td className="px-5 py-4 align-top">
        <Link
          to={`/approvals/${approvalRequest.id}`}
          className="font-medium text-neutral-950 underline-offset-4 hover:text-emerald-700 hover:underline"
        >
          #{approvalRequest.id}
        </Link>
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
