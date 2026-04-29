import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import { getImportJobs, isAuthenticationError } from '../../lib/api-client'
import type { ImportJobListItem } from '../../lib/schemas'

const importJobsPageRequest = { page: 0, size: 10 } as const

export function ImportsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const importJobsQuery = useQuery({
    queryKey: ['import-jobs', importJobsPageRequest],
    queryFn: () => getImportJobs(importJobsPageRequest),
  })

  useEffect(() => {
    handleAuthenticationError(importJobsQuery.error)
  }, [handleAuthenticationError, importJobsQuery.error])

  if (importJobsQuery.isPending) {
    return (
      <StatusPanel
        title="Loading imports"
        message="Fetching the current import queue."
      />
    )
  }

  if (importJobsQuery.error && !isAuthenticationError(importJobsQuery.error)) {
    return (
      <StatusPanel
        title="Imports unavailable"
        message={importJobsQuery.error.message}
      />
    )
  }

  if (!importJobsQuery.data || importJobsQuery.data.items.length === 0) {
    return (
      <section className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-emerald-700">Imports</p>
        <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
          Current queue
        </h2>
        <p className="mt-4 text-sm text-neutral-600">
          No import jobs were returned for the current tenant.
        </p>
      </section>
    )
  }

  const page = importJobsQuery.data

  return (
    <section className="rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Imports</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Current queue
          </h2>
        </div>
        <p className="text-sm text-neutral-600">
          Showing {page.items.length} of {page.total}
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-[920px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[26%] px-5 py-3">Job</th>
              <th className="w-[16%] px-5 py-3">Status</th>
              <th className="w-[16%] px-5 py-3">Type</th>
              <th className="w-[16%] px-5 py-3">Counts</th>
              <th className="w-[13%] px-5 py-3">Created</th>
              <th className="w-[13%] px-5 py-3">Finished</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {page.items.map((job) => (
              <ImportJobRow key={job.id} job={job} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function ImportJobRow({ job }: { job: ImportJobListItem }) {
  return (
    <tr>
      <td className="px-5 py-4 align-top">
        <div className="min-w-0">
          <p className="truncate font-medium text-neutral-950">
            {job.sourceFilename}
          </p>
          <p className="mt-1 text-xs text-neutral-500">#{job.id}</p>
        </div>
      </td>
      <td className="px-5 py-4 align-top">
        <span className="inline-flex rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-700">
          {job.status}
        </span>
        <p
          className={[
            'mt-2 text-xs font-medium',
            job.hasFailures ? 'text-rose-700' : 'text-neutral-500',
          ].join(' ')}
        >
          {job.hasFailures ? 'Has failures' : 'No failures'}
        </p>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        <p className="font-medium text-neutral-900">{job.importType}</p>
        <p className="mt-1 text-xs text-neutral-500">{job.sourceType}</p>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        <p>
          {job.successCount} / {job.failureCount} / {job.totalCount}
        </p>
        <p className="mt-1 text-xs text-neutral-500">success / failure / total</p>
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(job.createdAt)}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatOptionalDateTime(job.finishedAt)}
      </td>
    </tr>
  )
}

function formatOptionalDateTime(value: string | null): string {
  if (!value) {
    return 'In progress'
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
