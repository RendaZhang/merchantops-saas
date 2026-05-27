import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { type FormEvent, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import {
  createImportSelectiveReplayProposal,
  getImportJob,
  getImportJobErrors,
  isAuthenticationError,
  isPermissionDeniedError,
} from '../../lib/api-client'
import type {
  ApprovalRequest,
  ImportJobDetail,
  ImportJobErrorCodeCount,
  ImportJobErrorItem,
  ImportSelectiveReplayProposalRequest,
} from '../../lib/schemas'

const importJobErrorsPageRequest = { page: 0, size: 10 } as const
const importJobsQueryKey = ['import-jobs'] as const
const importJobDetailQueryKey = ['import-job'] as const
const approvalRequestsQueryKey = ['approval-requests'] as const
const PROPOSAL_REASON_MAX_LENGTH = 255

export function ImportJobDetailPage() {
  const { id } = useParams()
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const queryClient = useQueryClient()
  const [selectedErrorCodes, setSelectedErrorCodes] = useState<string[]>([])
  const [proposalReason, setProposalReason] = useState('')
  const [proposalValidationError, setProposalValidationError] = useState<string | null>(null)
  const jobId = parseImportJobId(id)
  const hasValidJobId = jobId !== null
  const importJobQuery = useQuery({
    queryKey: ['import-job', jobId],
    queryFn: () => getImportJob(requireImportJobId(jobId)),
    enabled: hasValidJobId,
  })
  const importJobErrorsQuery = useQuery({
    queryKey: ['import-job-errors', jobId, importJobErrorsPageRequest],
    queryFn: () => getImportJobErrors(requireImportJobId(jobId), importJobErrorsPageRequest),
    enabled: hasValidJobId,
  })
  const createProposalMutation = useMutation({
    mutationFn: (request: ImportSelectiveReplayProposalRequest) =>
      createImportSelectiveReplayProposal(requireImportJobId(jobId), request),
    onSuccess: (approvalRequest) => {
      setProposalValidationError(null)
      queryClient.setQueryData(
        ['approval-requests', 'detail', approvalRequest.id],
        approvalRequest,
      )
      void queryClient.invalidateQueries({ queryKey: approvalRequestsQueryKey })
      void queryClient.invalidateQueries({ queryKey: importJobDetailQueryKey })
      void queryClient.invalidateQueries({ queryKey: importJobsQueryKey })
    },
  })

  useEffect(() => {
    handleAuthenticationError(importJobQuery.error)
  }, [handleAuthenticationError, importJobQuery.error])

  useEffect(() => {
    handleAuthenticationError(importJobErrorsQuery.error)
  }, [handleAuthenticationError, importJobErrorsQuery.error])

  useEffect(() => {
    handleAuthenticationError(createProposalMutation.error)
  }, [createProposalMutation.error, handleAuthenticationError])

  function handleProposalSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (createProposalMutation.isPending) {
      return
    }

    createProposalMutation.reset()

    const selectedCodesInDisplayOrder = getSelectedErrorCodesInDisplayOrder(
      importJobQuery.data?.errorCodeCounts ?? [],
      selectedErrorCodes,
    )

    if (selectedCodesInDisplayOrder.length === 0) {
      setProposalValidationError('Select at least one error code.')
      return
    }

    const trimmedReason = proposalReason.trim()

    if (trimmedReason.length > PROPOSAL_REASON_MAX_LENGTH) {
      setProposalValidationError(
        `Proposal reason must be ${PROPOSAL_REASON_MAX_LENGTH} characters or fewer.`,
      )
      return
    }

    const request: ImportSelectiveReplayProposalRequest = {
      errorCodes: selectedCodesInDisplayOrder,
    }

    if (trimmedReason) {
      request.proposalReason = trimmedReason
    }

    setProposalValidationError(null)
    createProposalMutation.mutate(request)
  }

  function handleErrorCodeToggle(errorCode: string, checked: boolean) {
    setSelectedErrorCodes((currentCodes) => {
      if (checked) {
        return currentCodes.includes(errorCode)
          ? currentCodes
          : [...currentCodes, errorCode]
      }

      return currentCodes.filter((currentCode) => currentCode !== errorCode)
    })
    setProposalValidationError(null)
    createProposalMutation.reset()
  }

  function handleProposalReasonChange(value: string) {
    setProposalReason(value)

    if (proposalValidationError) {
      setProposalValidationError(null)
    }

    if (createProposalMutation.error || createProposalMutation.data) {
      createProposalMutation.reset()
    }
  }

  if (!hasValidJobId) {
    return (
      <StatusPanel
        title="Import job unavailable"
        message="The route contains an invalid import job id."
      />
    )
  }

  if (importJobQuery.isPending) {
    return (
      <StatusPanel
        title="Loading import job"
        message="Fetching import diagnostics."
      />
    )
  }

  if (importJobQuery.error && !isAuthenticationError(importJobQuery.error)) {
    return (
      <StatusPanel
        title="Import job unavailable"
        message={importJobQuery.error.message}
      />
    )
  }

  if (!importJobQuery.data) {
    return (
      <StatusPanel
        title="Import job unavailable"
        message="No import job detail was returned."
      />
    )
  }

  const job = importJobQuery.data
  const proposalErrorMessage =
    proposalValidationError ?? getProposalMutationErrorMessage(createProposalMutation.error)

  return (
    <div className="grid min-w-0 gap-5">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <p className="text-sm font-medium text-emerald-700">Import detail</p>
          <h2 className="mt-2 break-all text-2xl font-semibold text-neutral-950">
            {job.sourceFilename}
          </h2>
        </div>
        <Link
          to="/imports"
          className="inline-flex w-fit rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950"
        >
          Back to imports
        </Link>
      </div>

      <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
        <div className="border-b border-neutral-200 p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p className="text-sm font-medium text-neutral-500">Job #{job.id}</p>
              <h3 className="mt-2 text-lg font-semibold text-neutral-950">
                {job.importType} from {job.sourceType}
              </h3>
            </div>
            <span className="inline-flex w-fit rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-700">
              {job.status}
            </span>
          </div>
          {job.errorSummary ? (
            <p className="mt-4 text-sm leading-6 text-rose-700">{job.errorSummary}</p>
          ) : null}
        </div>

        <div className="grid min-w-0 gap-0 divide-y divide-neutral-200 md:grid-cols-3 md:divide-x md:divide-y-0">
          <CountBlock label="Success" value={job.successCount} />
          <CountBlock label="Failure" value={job.failureCount} />
          <CountBlock label="Total" value={job.totalCount} />
        </div>
      </section>

      <section className="grid min-w-0 gap-4 lg:grid-cols-2">
        <DetailPanel title="Overview" job={job} />
        <TimingPanel job={job} />
      </section>

      <ErrorCodeCounts counts={job.errorCodeCounts} />

      <SelectiveReplayProposalPanel
        counts={job.errorCodeCounts}
        selectedErrorCodes={selectedErrorCodes}
        proposalReason={proposalReason}
        errorMessage={proposalErrorMessage}
        isSubmitting={createProposalMutation.isPending}
        approvalRequest={createProposalMutation.data ?? null}
        onToggleErrorCode={handleErrorCodeToggle}
        onProposalReasonChange={handleProposalReasonChange}
        onSubmit={handleProposalSubmit}
      />

      <FailedRowsSection
        isPending={importJobErrorsQuery.isPending}
        error={importJobErrorsQuery.error}
        items={importJobErrorsQuery.data?.items ?? []}
        total={importJobErrorsQuery.data?.total ?? 0}
      />
    </div>
  )
}

function CountBlock({ label, value }: { label: string; value: number }) {
  return (
    <div className="p-5">
      <p className="text-sm font-medium text-neutral-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-neutral-950">{value}</p>
    </div>
  )
}

function DetailPanel({ title, job }: { title: string; job: ImportJobDetail }) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white p-5">
      <h3 className="text-lg font-semibold text-neutral-950">{title}</h3>
      <dl className="mt-4 grid gap-4 text-sm">
        <DetailItem label="Source filename" value={job.sourceFilename} />
        <DetailItem label="Source job" value={formatOptionalId(job.sourceJobId)} />
        <DetailItem label="Requested by" value={`#${job.requestedBy}`} />
        <DetailItem label="Request ID" value={job.requestId} />
      </dl>
    </section>
  )
}

function TimingPanel({ job }: { job: ImportJobDetail }) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white p-5">
      <h3 className="text-lg font-semibold text-neutral-950">Timing</h3>
      <dl className="mt-4 grid gap-4 text-sm">
        <DetailItem label="Created" value={formatDateTime(job.createdAt)} />
        <DetailItem label="Started" value={formatOptionalDateTime(job.startedAt, 'Not started')} />
        <DetailItem label="Finished" value={formatOptionalDateTime(job.finishedAt, 'In progress')} />
      </dl>
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

function ErrorCodeCounts({ counts }: { counts: ImportJobErrorCodeCount[] }) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <div className="border-b border-neutral-200 p-5">
        <h3 className="text-lg font-semibold text-neutral-950">Error diagnostics</h3>
      </div>

      {counts.length === 0 ? (
        <p className="p-5 text-sm text-neutral-600">No error code counts were returned.</p>
      ) : (
        <div className="grid min-w-0 gap-0 divide-y divide-neutral-200 md:grid-cols-3 md:divide-x md:divide-y-0">
          {counts.map((item) => (
            <div key={item.errorCode} className="min-w-0 p-5">
              <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
                {item.errorCode}
              </code>
              <p className="mt-3 text-2xl font-semibold text-neutral-950">{item.count}</p>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function SelectiveReplayProposalPanel({
  counts,
  selectedErrorCodes,
  proposalReason,
  errorMessage,
  isSubmitting,
  approvalRequest,
  onToggleErrorCode,
  onProposalReasonChange,
  onSubmit,
}: {
  counts: ImportJobErrorCodeCount[]
  selectedErrorCodes: string[]
  proposalReason: string
  errorMessage: string | null
  isSubmitting: boolean
  approvalRequest: ApprovalRequest | null
  onToggleErrorCode: (errorCode: string, checked: boolean) => void
  onProposalReasonChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  const reasonLength = proposalReason.trim().length
  const isReasonOverLimit = reasonLength > PROPOSAL_REASON_MAX_LENGTH

  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <div className="border-b border-neutral-200 p-5">
        <p className="text-sm font-medium text-emerald-700">Selective replay</p>
        <h3 className="mt-2 text-lg font-semibold text-neutral-950">
          Proposal request
        </h3>
      </div>

      <form className="grid min-w-0 gap-5 p-5" onSubmit={onSubmit}>
        <fieldset className="grid min-w-0 gap-3" disabled={isSubmitting}>
          <legend className="text-sm font-semibold text-neutral-950">
            Error codes
          </legend>

          {counts.length === 0 ? (
            <p className="text-sm text-neutral-600">
              No error codes are available for proposal.
            </p>
          ) : (
            <div className="grid min-w-0 gap-3 md:grid-cols-2 xl:grid-cols-3">
              {counts.map((item, index) => {
                const checkboxId = `import-proposal-error-code-${index}`
                const checked = selectedErrorCodes.includes(item.errorCode)

                return (
                  <label
                    key={item.errorCode}
                    htmlFor={checkboxId}
                    className={[
                      'flex min-w-0 items-start gap-3 rounded-md border p-3 transition',
                      checked
                        ? 'border-emerald-300 bg-emerald-50'
                        : 'border-neutral-200 bg-white hover:border-neutral-300',
                    ].join(' ')}
                  >
                    <input
                      id={checkboxId}
                      type="checkbox"
                      checked={checked}
                      onChange={(event) =>
                        onToggleErrorCode(item.errorCode, event.target.checked)
                      }
                      className="mt-1 h-4 w-4 shrink-0 accent-emerald-700"
                    />
                    <span className="min-w-0">
                      <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
                        {item.errorCode}
                      </code>
                      <span className="mt-2 block text-sm text-neutral-600">
                        {item.count} failed {item.count === 1 ? 'row' : 'rows'}
                      </span>
                    </span>
                  </label>
                )
              })}
            </div>
          )}
        </fieldset>

        <div className="grid min-w-0 gap-2">
          <label
            className="text-sm font-semibold text-neutral-950"
            htmlFor="selective-replay-proposal-reason"
          >
            Reason
          </label>
          <textarea
            id="selective-replay-proposal-reason"
            value={proposalReason}
            rows={3}
            disabled={isSubmitting || counts.length === 0}
            aria-invalid={isReasonOverLimit}
            aria-describedby="selective-replay-proposal-reason-count selective-replay-proposal-error"
            onChange={(event) => onProposalReasonChange(event.target.value)}
            placeholder="Optional reviewer context."
            className={[
              'min-h-24 w-full resize-y rounded-md border bg-white px-3 py-2 text-sm leading-6 text-neutral-900 outline-none transition disabled:bg-neutral-50 disabled:text-neutral-500',
              isReasonOverLimit
                ? 'border-rose-300 focus:border-rose-500 focus:ring-2 focus:ring-rose-100'
                : 'border-neutral-300 focus:border-emerald-700 focus:ring-2 focus:ring-emerald-100',
            ].join(' ')}
          />
          <p
            id="selective-replay-proposal-reason-count"
            className={[
              'text-xs',
              isReasonOverLimit ? 'font-medium text-rose-700' : 'text-neutral-500',
            ].join(' ')}
          >
            {reasonLength}/{PROPOSAL_REASON_MAX_LENGTH}
          </p>
        </div>

        <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="submit"
            disabled={isSubmitting || counts.length === 0}
            className="inline-flex w-fit rounded-md border border-emerald-700 bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:border-emerald-800 hover:bg-emerald-800 disabled:border-neutral-200 disabled:bg-neutral-100 disabled:text-neutral-400"
          >
            {isSubmitting ? 'Creating...' : 'Create proposal'}
          </button>

          {approvalRequest ? (
            <Link
              to={`/approvals/${approvalRequest.id}`}
              className="inline-flex w-fit rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950"
            >
              Approval request #{approvalRequest.id}
            </Link>
          ) : null}
        </div>

        {errorMessage ? (
          <p
            id="selective-replay-proposal-error"
            role="alert"
            className="text-sm font-medium text-rose-700"
          >
            {errorMessage}
          </p>
        ) : null}
      </form>
    </section>
  )
}

function FailedRowsSection({
  isPending,
  error,
  items,
  total,
}: {
  isPending: boolean
  error: Error | null
  items: ImportJobErrorItem[]
  total: number
}) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Failed rows</p>
          <h3 className="mt-2 text-lg font-semibold text-neutral-950">
            First error page
          </h3>
        </div>
        <p className="text-sm text-neutral-600">Showing {items.length} of {total}</p>
      </div>

      {isPending ? (
        <p className="p-5 text-sm text-neutral-600">Loading failed rows.</p>
      ) : error && !isAuthenticationError(error) ? (
        <p className="p-5 text-sm font-medium text-rose-700">{error.message}</p>
      ) : items.length === 0 ? (
        <p className="p-5 text-sm text-neutral-600">No failed rows were returned.</p>
      ) : (
        <div className="max-w-full overflow-x-auto">
          <table className="min-w-[980px] table-fixed divide-y divide-neutral-200 text-left">
            <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
              <tr>
                <th className="w-[10%] px-5 py-3">Row</th>
                <th className="w-[18%] px-5 py-3">Code</th>
                <th className="w-[26%] px-5 py-3">Message</th>
                <th className="w-[30%] px-5 py-3">Raw payload</th>
                <th className="w-[16%] px-5 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 text-sm">
              {items.map((item) => (
                <FailedRow key={item.id} item={item} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function FailedRow({ item }: { item: ImportJobErrorItem }) {
  return (
    <tr>
      <td className="px-5 py-4 align-top text-neutral-700">
        {item.rowNumber ?? 'Global'}
      </td>
      <td className="px-5 py-4 align-top">
        <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
          {item.errorCode}
        </code>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        <span className="break-words">{item.errorMessage}</span>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        <code className="block max-h-28 overflow-y-auto whitespace-pre-wrap break-all rounded bg-neutral-50 px-2 py-1 text-xs text-neutral-800">
          {item.rawPayload ?? 'No raw payload'}
        </code>
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(item.createdAt)}
      </td>
    </tr>
  )
}

function getSelectedErrorCodesInDisplayOrder(
  counts: ImportJobErrorCodeCount[],
  selectedErrorCodes: string[],
): string[] {
  return counts
    .map((item) => item.errorCode)
    .filter((errorCode) => selectedErrorCodes.includes(errorCode))
}

function getProposalMutationErrorMessage(error: unknown): string | null {
  if (!error || isAuthenticationError(error)) {
    return null
  }

  if (isPermissionDeniedError(error)) {
    return 'Current account does not have USER_WRITE permission.'
  }

  return error instanceof Error
    ? error.message
    : 'Selective replay proposal could not be created.'
}

function parseImportJobId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null
  }

  const parsed = Number(value)

  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    return null
  }

  return parsed
}

function requireImportJobId(value: number | null): number {
  if (value === null) {
    throw new Error('Invalid import job id.')
  }

  return value
}

function formatOptionalId(value: number | null): string {
  if (value === null) {
    return 'Original job'
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
