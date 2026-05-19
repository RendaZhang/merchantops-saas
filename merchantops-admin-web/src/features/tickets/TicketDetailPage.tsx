import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { type FormEvent, type ReactNode, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import {
  addTicketComment,
  getTicket,
  isAuthenticationError,
  isPermissionDeniedError,
} from '../../lib/api-client'
import type {
  TicketComment,
  TicketOperationLog,
} from '../../lib/schemas'

const COMMENT_CONTENT_MAX_LENGTH = 2000

export function TicketDetailPage() {
  const { id } = useParams()
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const queryClient = useQueryClient()
  const [commentContent, setCommentContent] = useState('')
  const [commentValidationError, setCommentValidationError] = useState<string | null>(null)
  const ticketId = parseTicketId(id)
  const hasValidTicketId = ticketId !== null
  const ticketQuery = useQuery({
    queryKey: ['tickets', 'detail', ticketId],
    queryFn: () => getTicket(requireTicketId(ticketId)),
    enabled: hasValidTicketId,
  })
  const commentMutation = useMutation({
    mutationFn: (content: string) =>
      addTicketComment(requireTicketId(ticketId), { content }),
    onSuccess: (comment) => {
      setCommentContent('')
      setCommentValidationError(null)
      void queryClient.invalidateQueries({
        queryKey: ['tickets', 'detail', comment.ticketId],
      })
      void queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })

  useEffect(() => {
    handleAuthenticationError(ticketQuery.error)
  }, [handleAuthenticationError, ticketQuery.error])

  useEffect(() => {
    handleAuthenticationError(commentMutation.error)
  }, [commentMutation.error, handleAuthenticationError])

  function handleCommentSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (commentMutation.isPending) {
      return
    }

    commentMutation.reset()

    const trimmedContent = commentContent.trim()

    if (!trimmedContent) {
      setCommentValidationError('Comment content is required.')
      return
    }

    if (trimmedContent.length > COMMENT_CONTENT_MAX_LENGTH) {
      setCommentValidationError(
        `Comment content must be ${COMMENT_CONTENT_MAX_LENGTH} characters or fewer.`,
      )
      return
    }

    setCommentValidationError(null)
    commentMutation.mutate(trimmedContent)
  }

  function handleCommentContentChange(value: string) {
    setCommentContent(value)

    if (commentValidationError) {
      setCommentValidationError(null)
    }

    if (commentMutation.error) {
      commentMutation.reset()
    }
  }

  if (!hasValidTicketId) {
    return (
      <StatusPanel
        title="Ticket unavailable"
        message="The route contains an invalid ticket id."
      />
    )
  }

  if (ticketQuery.isPending) {
    return (
      <StatusPanel
        title="Loading ticket"
        message="Fetching ticket detail and activity."
      />
    )
  }

  if (ticketQuery.error && !isAuthenticationError(ticketQuery.error)) {
    return (
      <StatusPanel
        title="Ticket unavailable"
        message={ticketQuery.error.message}
      />
    )
  }

  if (!ticketQuery.data) {
    return (
      <StatusPanel
        title="Ticket unavailable"
        message="No ticket detail was returned."
      />
    )
  }

  const ticket = ticketQuery.data
  const commentErrorMessage =
    commentValidationError ?? getCommentMutationErrorMessage(commentMutation.error)

  return (
    <div className="grid min-w-0 gap-5">
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <p className="text-sm font-medium text-emerald-700">Ticket detail</p>
          <h2 className="mt-2 break-words text-2xl font-semibold text-neutral-950">
            {ticket.title}
          </h2>
        </div>
        <Link
          to="/tickets"
          className="inline-flex w-fit rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950"
        >
          Back to tickets
        </Link>
      </div>

      <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
        <div className="border-b border-neutral-200 p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div className="min-w-0">
              <p className="text-sm font-medium text-neutral-500">
                Ticket #{ticket.id}
              </p>
              <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-neutral-700">
                {ticket.description || 'No description was returned.'}
              </p>
            </div>
            <StatusBadge status={ticket.status} />
          </div>
        </div>

        <div className="grid min-w-0 gap-0 divide-y divide-neutral-200 md:grid-cols-4 md:divide-x md:divide-y-0">
          <SummaryBlock
            label="Assignee"
            value={formatUserLabel(
              ticket.assigneeId,
              ticket.assigneeUsername,
              'Unassigned',
            )}
            detail={formatUserDetail(ticket.assigneeId, ticket.assigneeUsername)}
          />
          <SummaryBlock
            label="Creator"
            value={formatUserLabel(ticket.createdBy, ticket.createdByUsername)}
            detail={formatUserDetail(ticket.createdBy, ticket.createdByUsername)}
          />
          <SummaryBlock label="Created" value={formatDateTime(ticket.createdAt)} />
          <SummaryBlock label="Updated" value={formatDateTime(ticket.updatedAt)} />
        </div>
      </section>

      <section className="grid min-w-0 gap-4 lg:grid-cols-2">
        <CommentsSection
          comments={ticket.comments}
          content={commentContent}
          errorMessage={commentErrorMessage}
          isSubmitting={commentMutation.isPending}
          onContentChange={handleCommentContentChange}
          onSubmit={handleCommentSubmit}
        />
        <OperationLogsSection operationLogs={ticket.operationLogs} />
      </section>
    </div>
  )
}

function CommentsSection({
  comments,
  content,
  errorMessage,
  isSubmitting,
  onContentChange,
  onSubmit,
}: {
  comments: TicketComment[]
  content: string
  errorMessage: string | null
  isSubmitting: boolean
  onContentChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  const contentLength = content.trim().length
  const isOverLimit = contentLength > COMMENT_CONTENT_MAX_LENGTH

  return (
    <DetailPanel title="Comments" count={comments.length}>
      <form
        className="grid gap-3 border-b border-neutral-200 p-5"
        onSubmit={onSubmit}
      >
        <label
          className="text-sm font-semibold text-neutral-950"
          htmlFor="ticket-comment-content"
        >
          Internal comment
        </label>
        <textarea
          id="ticket-comment-content"
          value={content}
          rows={4}
          disabled={isSubmitting}
          aria-invalid={Boolean(errorMessage)}
          aria-describedby="ticket-comment-count ticket-comment-error"
          onChange={(event) => onContentChange(event.target.value)}
          placeholder="Add an internal ticket update."
          className={[
            'min-h-28 w-full resize-y rounded-md border bg-white px-3 py-2 text-sm leading-6 text-neutral-900 outline-none transition disabled:bg-neutral-50 disabled:text-neutral-500',
            isOverLimit || errorMessage
              ? 'border-rose-300 focus:border-rose-500 focus:ring-2 focus:ring-rose-100'
              : 'border-neutral-300 focus:border-emerald-700 focus:ring-2 focus:ring-emerald-100',
          ].join(' ')}
        />
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p
            id="ticket-comment-count"
            className={[
              'text-xs',
              isOverLimit ? 'font-medium text-rose-700' : 'text-neutral-500',
            ].join(' ')}
          >
            {contentLength}/{COMMENT_CONTENT_MAX_LENGTH}
          </p>
          <button
            type="submit"
            disabled={isSubmitting}
            className="inline-flex w-fit rounded-md border border-emerald-700 bg-emerald-700 px-4 py-2 text-sm font-medium text-white transition hover:border-emerald-800 hover:bg-emerald-800 disabled:border-neutral-200 disabled:bg-neutral-100 disabled:text-neutral-400"
          >
            {isSubmitting ? 'Posting...' : 'Post internal comment'}
          </button>
        </div>
        {errorMessage ? (
          <p
            id="ticket-comment-error"
            role="alert"
            className="text-sm font-medium text-rose-700"
          >
            {errorMessage}
          </p>
        ) : null}
      </form>
      {comments.length === 0 ? (
        <p className="p-5 text-sm text-neutral-600">
          No comments were returned for this ticket.
        </p>
      ) : (
        <ol className="divide-y divide-neutral-100">
          {comments.map((comment) => (
            <li key={comment.id} className="min-w-0 p-5">
              <p className="whitespace-pre-wrap break-words text-sm leading-6 text-neutral-800">
                {comment.content}
              </p>
              <p className="mt-3 break-words text-xs text-neutral-500">
                {formatUserLabel(comment.createdBy, comment.createdByUsername)} -{' '}
                #{comment.id} - {formatDateTime(comment.createdAt)}
              </p>
            </li>
          ))}
        </ol>
      )}
    </DetailPanel>
  )
}

function OperationLogsSection({
  operationLogs,
}: {
  operationLogs: TicketOperationLog[]
}) {
  return (
    <DetailPanel title="Operation logs" count={operationLogs.length}>
      {operationLogs.length === 0 ? (
        <p className="p-5 text-sm text-neutral-600">
          No operation logs were returned for this ticket.
        </p>
      ) : (
        <ol className="divide-y divide-neutral-100">
          {operationLogs.map((operationLog) => (
            <li key={operationLog.id} className="min-w-0 p-5">
              <div className="flex min-w-0 flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <code className="w-fit max-w-full break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
                  {operationLog.operationType}
                </code>
                <p className="break-words text-xs text-neutral-500">
                  {formatDateTime(operationLog.createdAt)}
                </p>
              </div>
              <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-neutral-800">
                {operationLog.detail}
              </p>
              <p className="mt-3 break-words text-xs text-neutral-500">
                {formatUserLabel(
                  operationLog.operatorId,
                  operationLog.operatorUsername,
                )}{' '}
                - #{operationLog.id}
              </p>
            </li>
          ))}
        </ol>
      )}
    </DetailPanel>
  )
}

function DetailPanel({
  title,
  count,
  children,
}: {
  title: string
  count: number
  children: ReactNode
}) {
  return (
    <section className="min-w-0 rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <h3 className="text-lg font-semibold text-neutral-950">{title}</h3>
        <p className="text-sm text-neutral-600">{count} returned</p>
      </div>
      {children}
    </section>
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
      <p className="mt-2 break-words text-lg font-semibold text-neutral-950">
        {value}
      </p>
      {detail ? <p className="mt-1 break-all text-sm text-neutral-500">{detail}</p> : null}
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span className="inline-flex w-fit rounded-full border border-neutral-200 bg-neutral-50 px-2.5 py-1 text-xs font-medium text-neutral-700">
      {status}
    </span>
  )
}

function parseTicketId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null
  }

  const parsed = Number(value)

  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    return null
  }

  return parsed
}

function requireTicketId(value: number | null): number {
  if (value === null) {
    throw new Error('Invalid ticket id.')
  }

  return value
}

function getCommentMutationErrorMessage(error: unknown): string | null {
  if (!error || isAuthenticationError(error)) {
    return null
  }

  if (isPermissionDeniedError(error)) {
    return 'Current account does not have TICKET_WRITE permission.'
  }

  return error instanceof Error ? error.message : 'Comment could not be posted.'
}

function formatUserLabel(
  userId: number | null,
  username: string | null,
  fallback = 'Unknown user',
): string {
  if (userId === null) {
    return fallback
  }

  return username || `#${userId}`
}

function formatUserDetail(
  userId: number | null,
  username: string | null,
): string | undefined {
  if (userId === null || !username) {
    return undefined
  }

  return `#${userId}`
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
