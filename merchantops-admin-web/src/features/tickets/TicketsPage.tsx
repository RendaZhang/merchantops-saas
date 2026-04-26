import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import { getTickets, isAuthenticationError } from '../../lib/api-client'
import type { TicketListItem } from '../../lib/schemas'

const ticketsPageRequest = { page: 0, size: 10 } as const

export function TicketsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const ticketsQuery = useQuery({
    queryKey: ['tickets', ticketsPageRequest],
    queryFn: () => getTickets(ticketsPageRequest),
  })

  useEffect(() => {
    handleAuthenticationError(ticketsQuery.error)
  }, [handleAuthenticationError, ticketsQuery.error])

  if (ticketsQuery.isPending) {
    return <StatusPanel title="Loading tickets" message="Fetching the current queue." />
  }

  if (ticketsQuery.error && !isAuthenticationError(ticketsQuery.error)) {
    return (
      <StatusPanel
        title="Tickets unavailable"
        message={ticketsQuery.error.message}
      />
    )
  }

  if (!ticketsQuery.data || ticketsQuery.data.items.length === 0) {
    return (
      <section className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-emerald-700">Tickets</p>
        <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
          Current queue
        </h2>
        <p className="mt-4 text-sm text-neutral-600">
          No tickets are waiting in this tenant queue.
        </p>
      </section>
    )
  }

  const page = ticketsQuery.data

  return (
    <section className="rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Tickets</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Current queue
          </h2>
        </div>
        <p className="text-sm text-neutral-600">
          Showing {page.items.length} of {page.total}
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-[760px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[34%] px-5 py-3">Ticket</th>
              <th className="w-[16%] px-5 py-3">Status</th>
              <th className="w-[18%] px-5 py-3">Assignee</th>
              <th className="w-[16%] px-5 py-3">Created</th>
              <th className="w-[16%] px-5 py-3">Updated</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {page.items.map((ticket) => (
              <TicketRow key={ticket.id} ticket={ticket} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function TicketRow({ ticket }: { ticket: TicketListItem }) {
  return (
    <tr>
      <td className="px-5 py-4 align-top">
        <div className="min-w-0">
          <p className="truncate font-medium text-neutral-950">{ticket.title}</p>
          <p className="mt-1 text-xs text-neutral-500">#{ticket.id}</p>
        </div>
      </td>
      <td className="px-5 py-4 align-top">
        <span className="inline-flex rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-700">
          {ticket.status}
        </span>
      </td>
      <td className="px-5 py-4 align-top text-neutral-700">
        {ticket.assigneeUsername ?? 'Unassigned'}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(ticket.createdAt)}
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        {formatDateTime(ticket.updatedAt)}
      </td>
    </tr>
  )
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
