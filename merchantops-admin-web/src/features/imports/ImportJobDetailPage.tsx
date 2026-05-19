import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import {
  getImportJob,
  getImportJobErrors,
  isAuthenticationError,
} from '../../lib/api-client'
import type {
  ImportJobDetail,
  ImportJobErrorCodeCount,
  ImportJobErrorItem,
} from '../../lib/schemas'

const importJobErrorsPageRequest = { page: 0, size: 10 } as const

export function ImportJobDetailPage() {
  const { id } = useParams()
  const { handleAuthenticationError } = useAuthenticatedRoute()
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

  useEffect(() => {
    handleAuthenticationError(importJobQuery.error)
  }, [handleAuthenticationError, importJobQuery.error])

  useEffect(() => {
    handleAuthenticationError(importJobErrorsQuery.error)
  }, [handleAuthenticationError, importJobErrorsQuery.error])

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
