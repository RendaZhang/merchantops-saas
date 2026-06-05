import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { type ReactNode, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import {
  approveApprovalRequest,
  getApprovalRequest,
  isAuthenticationError,
  rejectApprovalRequest,
} from '../../lib/api-client'
import type { ApprovalRequest } from '../../lib/schemas'

type ReviewIntent = 'approve' | 'reject'

const approvalRequestsQueryKey = ['approval-requests'] as const

export function ApprovalRequestDetailPage() {
  const { id } = useParams()
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const queryClient = useQueryClient()
  const [reviewIntent, setReviewIntent] = useState<ReviewIntent | null>(null)
  const approvalRequestId = parseApprovalRequestId(id)
  const hasValidApprovalRequestId = approvalRequestId !== null
  const approvalRequestDetailQueryKey = useMemo(
    () => ['approval-requests', 'detail', approvalRequestId] as const,
    [approvalRequestId],
  )
  const approvalRequestQuery = useQuery({
    queryKey: approvalRequestDetailQueryKey,
    queryFn: () => getApprovalRequest(requireApprovalRequestId(approvalRequestId)),
    enabled: hasValidApprovalRequestId,
  })
  const reviewMutation = useMutation({
    mutationFn: (intent: ReviewIntent) => {
      const resolvedId = requireApprovalRequestId(approvalRequestId)
      return intent === 'approve'
        ? approveApprovalRequest(resolvedId)
        : rejectApprovalRequest(resolvedId)
    },
    onSuccess: (updatedApprovalRequest) => {
      setReviewIntent(null)
      queryClient.setQueryData(
        ['approval-requests', 'detail', updatedApprovalRequest.id],
        updatedApprovalRequest,
      )
      void queryClient.invalidateQueries({ queryKey: approvalRequestsQueryKey })
    },
  })

  useEffect(() => {
    handleAuthenticationError(approvalRequestQuery.error)
  }, [approvalRequestQuery.error, handleAuthenticationError])

  useEffect(() => {
    handleAuthenticationError(reviewMutation.error)
  }, [handleAuthenticationError, reviewMutation.error])

  if (!hasValidApprovalRequestId) {
    return (
      <StatusPanel
        title="Approval request unavailable"
        message="The route contains an invalid approval request id."
      />
    )
  }

  if (approvalRequestQuery.isPending) {
    return (
      <StatusPanel
        title="Loading approval request"
        message="Fetching approval request detail."
      />
    )
  }

  if (
    approvalRequestQuery.error &&
    !isAuthenticationError(approvalRequestQuery.error)
  ) {
    return (
      <StatusPanel
        title="Approval request unavailable"
        message={approvalRequestQuery.error.message}
      />
    )
  }

  if (!approvalRequestQuery.data) {
    return (
      <StatusPanel
        title="Approval request unavailable"
        message="No approval request detail was returned."
      />
    )
  }

  const approvalRequest = approvalRequestQuery.data
  const mutationError =
    reviewMutation.error && !isAuthenticationError(reviewMutation.error)
      ? reviewMutation.error.message
      : null

  return (
    <div className="grid min-w-0 gap-5">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <p className="text-sm font-medium text-emerald-700">Approval detail</p>
          <h2 className="mt-2 break-all text-2xl font-semibold text-neutral-950">
            Request #{approvalRequest.id}
          </h2>
        </div>
        <Link
          to="/approvals"
          className="inline-flex w-fit rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950"
        >
          Back to approvals
        </Link>
      </div>

      <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
        <div className="border-b border-neutral-200 p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div className="min-w-0">
              <p className="text-sm font-medium text-neutral-500">
                Tenant #{approvalRequest.tenantId}
              </p>
              <h3 className="mt-2 break-all text-lg font-semibold text-neutral-950">
                {approvalRequest.actionType}
              </h3>
            </div>
            <StatusBadge status={approvalRequest.status} />
          </div>
        </div>

        <div className="grid min-w-0 gap-0 divide-y divide-neutral-200 md:grid-cols-3 md:divide-x md:divide-y-0">
          <SummaryBlock label="Entity" value={approvalRequest.entityType} detail={`#${approvalRequest.entityId}`} />
          <SummaryBlock label="Requested by" value={`#${approvalRequest.requestedBy}`} />
          <SummaryBlock label="Reviewed by" value={formatOptionalUser(approvalRequest.reviewedBy)} />
        </div>
      </section>

      <section className="grid min-w-0 gap-4 lg:grid-cols-2">
        <DetailPanel title="Request context">
          <DetailItem label="Request ID" value={approvalRequest.requestId} />
          <DetailItem label="Action" value={approvalRequest.actionType} />
          <DetailItem label="Entity" value={`${approvalRequest.entityType} #${approvalRequest.entityId}`} />
          <DetailItem label="Status" value={approvalRequest.status} />
        </DetailPanel>

        <DetailPanel title="Timing">
          <DetailItem label="Created" value={formatDateTime(approvalRequest.createdAt)} />
          <DetailItem
            label="Reviewed"
            value={formatOptionalDateTime(approvalRequest.reviewedAt, 'Not reviewed')}
          />
          <DetailItem
            label="Executed"
            value={formatOptionalDateTime(approvalRequest.executedAt, 'Not executed')}
          />
        </DetailPanel>
      </section>

      <PayloadPanel payloadJson={approvalRequest.payloadJson} />

      <ReviewPanel
        approvalRequest={approvalRequest}
        reviewIntent={reviewIntent}
        mutationPending={reviewMutation.isPending}
        mutationError={mutationError}
        onChooseIntent={(intent) => {
          reviewMutation.reset()
          setReviewIntent(intent)
        }}
        onCancelIntent={() => {
          reviewMutation.reset()
          setReviewIntent(null)
        }}
        onConfirmIntent={() => {
          if (reviewIntent) {
            reviewMutation.mutate(reviewIntent)
          }
        }}
      />
    </div>
  )
}

function SummaryBlock({
  label,
  value,
  detail,
}: {
  label: string
  value: string
  detail?: string
}) {
  return (
    <div className="min-w-0 p-5">
      <p className="text-sm font-medium text-neutral-500">{label}</p>
      <p className="mt-2 break-all text-xl font-semibold text-neutral-950">{value}</p>
      {detail ? <p className="mt-1 break-all text-sm text-neutral-500">{detail}</p> : null}
    </div>
  )
}

function DetailPanel({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white p-5">
      <h3 className="text-lg font-semibold text-neutral-950">{title}</h3>
      <dl className="mt-4 grid gap-4 text-sm">{children}</dl>
    </section>
  )
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs font-semibold uppercase text-neutral-500">{label}</dt>
      <dd className="mt-1 break-words text-neutral-800">{value}</dd>
    </div>
  )
}

function PayloadPanel({ payloadJson }: { payloadJson: string }) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <div className="border-b border-neutral-200 p-5">
        <h3 className="text-lg font-semibold text-neutral-950">Payload</h3>
      </div>
      <div className="p-5">
        <code className="block max-h-96 overflow-auto whitespace-pre-wrap break-all rounded bg-neutral-50 p-3 text-xs leading-5 text-neutral-800">
          {formatPayloadJson(payloadJson) || 'Empty payload'}
        </code>
      </div>
    </section>
  )
}

function ReviewPanel({
  approvalRequest,
  reviewIntent,
  mutationPending,
  mutationError,
  onChooseIntent,
  onCancelIntent,
  onConfirmIntent,
}: {
  approvalRequest: ApprovalRequest
  reviewIntent: ReviewIntent | null
  mutationPending: boolean
  mutationError: string | null
  onChooseIntent: (intent: ReviewIntent) => void
  onCancelIntent: () => void
  onConfirmIntent: () => void
}) {
  if (approvalRequest.status !== 'PENDING') {
    return (
      <section className="min-w-0 rounded-lg border border-neutral-200 bg-white p-5">
        <h3 className="text-lg font-semibold text-neutral-950">Review state</h3>
        <p className="mt-3 text-sm text-neutral-600">
          This request is {approvalRequest.status}. No review action is available.
        </p>
      </section>
    )
  }

  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white p-5">
      <h3 className="text-lg font-semibold text-neutral-950">Review controls</h3>
      <p className="mt-3 max-w-3xl text-sm leading-6 text-neutral-600">
        Approve synchronously executes the underlying action for this request:
        disable user, import selective replay, or ticket comment creation.
        Reject resolves the request without executing the underlying action.
      </p>

      <div className="mt-5 flex flex-wrap gap-3">
        <button
          type="button"
          disabled={mutationPending}
          onClick={() => onChooseIntent('approve')}
          className="rounded-md border border-emerald-700 bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:border-emerald-800 hover:bg-emerald-800 disabled:border-neutral-200 disabled:bg-neutral-100 disabled:text-neutral-400"
        >
          Approve
        </button>
        <button
          type="button"
          disabled={mutationPending}
          onClick={() => onChooseIntent('reject')}
          className="rounded-md border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950 disabled:border-neutral-200 disabled:text-neutral-400"
        >
          Reject
        </button>
      </div>

      {reviewIntent ? (
        <div className="mt-5 rounded-lg border border-amber-200 bg-amber-50 p-4">
          <h4 className="text-sm font-semibold text-amber-950">
            Confirm {reviewIntent === 'approve' ? 'approval' : 'rejection'}
          </h4>
          <p className="mt-2 text-sm leading-6 text-amber-900">
            {reviewIntent === 'approve'
              ? getApprovalImpact(approvalRequest.actionType)
              : 'Reject marks this request as rejected and does not disable a user, create an import replay job, or create a ticket comment.'}
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              disabled={mutationPending}
              onClick={onCancelIntent}
              className="rounded-md border border-amber-300 bg-white px-3 py-2 text-sm font-medium text-amber-950 transition hover:border-amber-500 disabled:border-neutral-200 disabled:text-neutral-400"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={mutationPending}
              onClick={onConfirmIntent}
              className="rounded-md border border-amber-950 bg-amber-950 px-3 py-2 text-sm font-medium text-white transition hover:border-neutral-950 hover:bg-neutral-950 disabled:border-neutral-200 disabled:bg-neutral-100 disabled:text-neutral-400"
            >
              {mutationPending
                ? 'Submitting...'
                : reviewIntent === 'approve'
                  ? 'Confirm approve'
                  : 'Confirm reject'}
            </button>
          </div>
        </div>
      ) : null}

      {mutationError ? (
        <p className="mt-4 text-sm font-medium text-rose-700">{mutationError}</p>
      ) : null}
    </section>
  )
}

function StatusBadge({ status }: { status: string }) {
  const statusClass =
    status === 'APPROVED'
      ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
      : status === 'REJECTED'
        ? 'border-rose-200 bg-rose-50 text-rose-700'
        : 'border-neutral-200 bg-neutral-50 text-neutral-700'

  return (
    <span className={`inline-flex w-fit rounded-full border px-2.5 py-1 text-xs font-medium ${statusClass}`}>
      {status}
    </span>
  )
}

function getApprovalImpact(actionType: string): string {
  if (actionType === 'USER_STATUS_DISABLE') {
    return 'Approve synchronously disables the target user through the existing user status write chain.'
  }

  if (actionType === 'IMPORT_JOB_SELECTIVE_REPLAY') {
    return 'Approve synchronously runs import selective replay and may create a derived import job.'
  }

  if (actionType === 'TICKET_COMMENT_CREATE') {
    return 'Approve synchronously creates the proposed ticket comment through the existing ticket comment write chain.'
  }

  return 'Approve synchronously executes the configured underlying approval action.'
}

function parseApprovalRequestId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null
  }

  const parsed = Number(value)

  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    return null
  }

  return parsed
}

function requireApprovalRequestId(value: number | null): number {
  if (value === null) {
    throw new Error('Invalid approval request id.')
  }

  return value
}

function formatPayloadJson(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
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
