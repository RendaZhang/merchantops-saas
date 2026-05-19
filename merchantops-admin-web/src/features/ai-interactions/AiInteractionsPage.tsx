import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import {
  getAiInteractionUsageSummary,
  isAuthenticationError,
} from '../../lib/api-client'
import type {
  AiInteractionUsageSummaryByInteractionType,
  AiInteractionUsageSummaryByPromptVersion,
  AiInteractionUsageSummaryByStatus,
} from '../../lib/schemas'

const aiInteractionUsageSummaryQueryKey = ['ai-interaction-usage-summary'] as const

export function AiInteractionsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const usageSummaryQuery = useQuery({
    queryKey: aiInteractionUsageSummaryQueryKey,
    queryFn: getAiInteractionUsageSummary,
  })

  useEffect(() => {
    handleAuthenticationError(usageSummaryQuery.error)
  }, [handleAuthenticationError, usageSummaryQuery.error])

  if (usageSummaryQuery.isPending) {
    return (
      <StatusPanel
        title="Loading AI interactions"
        message="Fetching the current tenant usage summary."
      />
    )
  }

  if (usageSummaryQuery.error && !isAuthenticationError(usageSummaryQuery.error)) {
    return (
      <StatusPanel
        title="AI interactions unavailable"
        message={usageSummaryQuery.error.message}
      />
    )
  }

  if (!usageSummaryQuery.data) {
    return (
      <StatusPanel
        title="AI interactions unavailable"
        message="No usage summary was returned for the current tenant."
      />
    )
  }

  const summary = usageSummaryQuery.data

  return (
    <section className="grid min-w-0 gap-5">
      <div className="min-w-0 rounded-lg border border-neutral-200 bg-white p-5">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-medium text-emerald-700">AI Interactions</p>
            <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
              Usage summary
            </h2>
          </div>
          <p className="text-sm text-neutral-600">
            Current tenant aggregate metadata
          </p>
        </div>

        <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <MetricCard label="Total interactions" value={summary.totalInteractions} />
          <MetricCard label="Succeeded" value={summary.succeededCount} />
          <MetricCard label="Failed" value={summary.failedCount} />
          <MetricCard label="Total tokens" value={summary.totalTokens} />
          <MetricCard
            label="Total cost micros"
            value={summary.totalCostMicros}
            detail="raw micros"
          />
        </div>
      </div>

      <InteractionTypeBreakdownTable items={summary.byInteractionType} />
      <StatusBreakdownTable items={summary.byStatus} />
      <PromptVersionBreakdownTable items={summary.byPromptVersion} />
    </section>
  )
}

function MetricCard({
  label,
  value,
  detail,
}: {
  label: string
  value: number
  detail?: string
}) {
  return (
    <article className="min-h-28 rounded-lg border border-neutral-200 bg-neutral-50 p-4">
      <p className="text-sm font-medium text-neutral-600">{label}</p>
      <p className="mt-3 text-3xl font-semibold text-neutral-950">
        {formatNumber(value)}
      </p>
      {detail ? <p className="mt-1 text-xs text-neutral-500">{detail}</p> : null}
    </article>
  )
}

function InteractionTypeBreakdownTable({
  items,
}: {
  items: AiInteractionUsageSummaryByInteractionType[]
}) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <BreakdownHeader
        title="By interaction type"
        count={items.length}
        emptyLabel="No interaction type buckets returned"
      />
      <div className="overflow-x-auto">
        <table className="min-w-[840px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[32%] px-5 py-3">Interaction type</th>
              <th className="w-[13%] px-5 py-3">Count</th>
              <th className="w-[13%] px-5 py-3">Succeeded</th>
              <th className="w-[13%] px-5 py-3">Failed</th>
              <th className="w-[14%] px-5 py-3">Tokens</th>
              <th className="w-[15%] px-5 py-3">Cost micros</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {items.length === 0 ? (
              <EmptyBreakdownRow colSpan={6} message="No interaction type buckets were returned." />
            ) : (
              items.map((item) => (
                <tr key={item.interactionType}>
                  <td className="px-5 py-4 align-top">
                    <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
                      {item.interactionType}
                    </code>
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.count)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.succeededCount)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.failedCount)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.totalTokens)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.totalCostMicros)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function StatusBreakdownTable({
  items,
}: {
  items: AiInteractionUsageSummaryByStatus[]
}) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <BreakdownHeader
        title="By status"
        count={items.length}
        emptyLabel="No status buckets returned"
      />
      <div className="overflow-x-auto">
        <table className="min-w-[620px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[38%] px-5 py-3">Status</th>
              <th className="w-[20%] px-5 py-3">Count</th>
              <th className="w-[20%] px-5 py-3">Tokens</th>
              <th className="w-[22%] px-5 py-3">Cost micros</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {items.length === 0 ? (
              <EmptyBreakdownRow colSpan={4} message="No status buckets were returned." />
            ) : (
              items.map((item) => (
                <tr key={item.status}>
                  <td className="px-5 py-4 align-top">
                    <span className="inline-flex rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-700">
                      {item.status}
                    </span>
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.count)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.totalTokens)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.totalCostMicros)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function PromptVersionBreakdownTable({
  items,
}: {
  items: AiInteractionUsageSummaryByPromptVersion[]
}) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <BreakdownHeader
        title="By prompt version"
        count={items.length}
        emptyLabel="No prompt version buckets returned"
      />
      <div className="overflow-x-auto">
        <table className="min-w-[900px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[34%] px-5 py-3">Prompt version</th>
              <th className="w-[12%] px-5 py-3">Count</th>
              <th className="w-[13%] px-5 py-3">Succeeded</th>
              <th className="w-[13%] px-5 py-3">Failed</th>
              <th className="w-[13%] px-5 py-3">Tokens</th>
              <th className="w-[15%] px-5 py-3">Cost micros</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {items.length === 0 ? (
              <EmptyBreakdownRow colSpan={6} message="No prompt version buckets were returned." />
            ) : (
              items.map((item) => (
                <tr key={item.promptVersion}>
                  <td className="px-5 py-4 align-top">
                    <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
                      {item.promptVersion}
                    </code>
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.count)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.succeededCount)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.failedCount)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.totalTokens)}
                  </td>
                  <td className="px-5 py-4 align-top text-neutral-700">
                    {formatNumber(item.totalCostMicros)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function BreakdownHeader({
  title,
  count,
  emptyLabel,
}: {
  title: string
  count: number
  emptyLabel: string
}) {
  return (
    <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-sm font-medium text-emerald-700">Breakdown</p>
        <h3 className="mt-2 text-xl font-semibold text-neutral-950">{title}</h3>
      </div>
      <p className="text-sm text-neutral-600">
        {count > 0 ? `${formatNumber(count)} buckets` : emptyLabel}
      </p>
    </div>
  )
}

function EmptyBreakdownRow({
  colSpan,
  message,
}: {
  colSpan: number
  message: string
}) {
  return (
    <tr>
      <td className="px-5 py-5 text-sm text-neutral-600" colSpan={colSpan}>
        {message}
      </td>
    </tr>
  )
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat().format(value)
}
