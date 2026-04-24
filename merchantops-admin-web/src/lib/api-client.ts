import { type ZodType, z } from 'zod'

import { clearAuthSession, getAuthorizationHeader } from './auth-token'
import {
  type ContextResponse,
  type LoginRequest,
  type LoginResponse,
  type TicketPage,
  contextResponseSchema,
  loginRequestSchema,
  loginResponseSchema,
  ticketPageSchema,
} from './schemas'

const SUCCESS_CODE = 'SUCCESS'
const STALE_CLAIMS_MESSAGE = 'token claims are stale, please login again'
const INACTIVE_TENANT_MESSAGE = 'tenant is not active'
const INACTIVE_USER_MESSAGE = 'user is not active'
const AUTH_ENDING_FORBIDDEN_MESSAGES = new Set([
  STALE_CLAIMS_MESSAGE,
  INACTIVE_TENANT_MESSAGE,
  INACTIVE_USER_MESSAGE,
])

const apiEnvelopeSchema = <T extends ZodType>(dataSchema: T) =>
  z.object({
    code: z.string(),
    message: z.string(),
    data: dataSchema.nullable(),
  })

type ApiRequestOptions = RequestInit & {
  authenticated?: boolean
  allowNullData?: boolean
}

type TicketPageRequest = {
  page?: number
  size?: number
}

export class ApiClientError extends Error {
  readonly status?: number
  readonly code?: string

  constructor(message: string, options?: { status?: number; code?: string }) {
    super(message)
    this.name = 'ApiClientError'
    this.status = options?.status
    this.code = options?.code
  }
}

export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  const parsedCredentials = loginRequestSchema.safeParse(credentials)

  if (!parsedCredentials.success) {
    throw new ApiClientError(
      parsedCredentials.error.issues[0]?.message ?? 'Check the login fields.',
    )
  }

  return apiRequest('/api/v1/auth/login', loginResponseSchema, {
    method: 'POST',
    body: JSON.stringify(parsedCredentials.data),
  })
}

export function getContext(): Promise<ContextResponse> {
  return apiRequest('/api/v1/context', contextResponseSchema, {
    authenticated: true,
  })
}

export function getTickets({ page = 0, size = 10 }: TicketPageRequest = {}): Promise<TicketPage> {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  })

  return apiRequest(`/api/v1/tickets?${searchParams.toString()}`, ticketPageSchema, {
    authenticated: true,
  })
}

export async function logout(): Promise<void> {
  await apiRequest('/api/v1/auth/logout', z.null(), {
    method: 'POST',
    authenticated: true,
    allowNullData: true,
  })
}

export async function logoutAll(): Promise<void> {
  await apiRequest('/api/v1/auth/logout-all', z.null(), {
    method: 'POST',
    authenticated: true,
    allowNullData: true,
  })
}

export function isAuthenticationError(error: unknown): boolean {
  return (
    error instanceof ApiClientError &&
    (error.status === 401 || error.code === 'AUTH_REQUIRED' || isAuthEndingForbiddenError(error))
  )
}

async function apiRequest<T>(
  path: string,
  dataSchema: ZodType<T>,
  options: ApiRequestOptions = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.authenticated) {
    const authorization = getAuthorizationHeader()

    if (!authorization) {
      throw new ApiClientError('Sign in again to continue.', {
        code: 'AUTH_REQUIRED',
      })
    }

    headers.set('Authorization', authorization)
  }

  const response = await fetch(path, {
    ...options,
    headers,
  })
  const responseBody = await parseResponseBody(response)

  if (!response.ok) {
    const errorMessage = extractErrorMessage(responseBody, response.statusText)
    const errorCode = extractErrorCode(responseBody)

    if (shouldClearAuthSession(response.status, errorCode, errorMessage)) {
      clearAuthSession()
    }

    throw new ApiClientError(errorMessage, {
      status: response.status,
      code: errorCode,
    })
  }

  const parsedEnvelope = apiEnvelopeSchema(dataSchema).safeParse(responseBody)

  if (!parsedEnvelope.success) {
    throw new ApiClientError('The server returned an unexpected response.')
  }

  if (
    parsedEnvelope.data.code !== SUCCESS_CODE ||
    (parsedEnvelope.data.data === null && !options.allowNullData)
  ) {
    throw new ApiClientError(parsedEnvelope.data.message, {
      status: response.status,
      code: parsedEnvelope.data.code,
    })
  }

  return parsedEnvelope.data.data as T
}

function shouldClearAuthSession(status: number, code: string | undefined, message: string): boolean {
  return status === 401 || isAuthEndingForbiddenResponse(status, code, message)
}

function isAuthEndingForbiddenError(error: ApiClientError): boolean {
  return isAuthEndingForbiddenResponse(error.status, error.code, error.message)
}

function isAuthEndingForbiddenResponse(
  status: number | undefined,
  code: string | undefined,
  message: string,
): boolean {
  return (
    status === 403 &&
    code === 'FORBIDDEN' &&
    AUTH_ENDING_FORBIDDEN_MESSAGES.has(message)
  )
}

async function parseResponseBody(response: Response): Promise<unknown> {
  const text = await response.text()

  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function extractErrorMessage(responseBody: unknown, fallback: string): string {
  if (
    typeof responseBody === 'object' &&
    responseBody !== null &&
    'message' in responseBody &&
    typeof responseBody.message === 'string'
  ) {
    return responseBody.message
  }

  return fallback || 'Request failed.'
}

function extractErrorCode(responseBody: unknown): string | undefined {
  if (
    typeof responseBody === 'object' &&
    responseBody !== null &&
    'code' in responseBody &&
    typeof responseBody.code === 'string'
  ) {
    return responseBody.code
  }

  return undefined
}
