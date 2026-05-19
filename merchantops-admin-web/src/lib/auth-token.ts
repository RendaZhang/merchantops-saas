import { z } from 'zod'

import {
  type JwtDisplayClaims,
  type LoginResponse,
  jwtDisplayClaimsSchema,
} from './schemas'

export const AUTH_STORAGE_KEY = 'merchantops.admin.auth.v1'

const storedAuthSessionSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.string().min(1),
  expiresAtEpochMs: z.number().positive(),
})

type StoredAuthSession = z.infer<typeof storedAuthSessionSchema>

export type AuthSession = StoredAuthSession & {
  claims: JwtDisplayClaims | null
}

export function saveAuthSession(loginResponse: LoginResponse): AuthSession | null {
  const storedSession: StoredAuthSession = {
    accessToken: loginResponse.accessToken,
    tokenType: loginResponse.tokenType,
    expiresAtEpochMs: Date.now() + loginResponse.expiresIn * 1000,
  }

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(storedSession))
  return readAuthSession()
}

export function readAuthSession(): AuthSession | null {
  const rawSession = window.localStorage.getItem(AUTH_STORAGE_KEY)

  if (!rawSession) {
    return null
  }

  const parsedSession = storedAuthSessionSchema.safeParse(parseJson(rawSession))

  if (!parsedSession.success || parsedSession.data.expiresAtEpochMs <= Date.now()) {
    clearAuthSession()
    return null
  }

  return {
    ...parsedSession.data,
    claims: decodeJwtDisplayClaims(parsedSession.data.accessToken),
  }
}

export function clearAuthSession() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}

export function getAuthorizationHeader(): string | undefined {
  const session = readAuthSession()

  if (!session) {
    return undefined
  }

  return `${session.tokenType} ${session.accessToken}`
}

function parseJson(value: string): unknown {
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function decodeJwtDisplayClaims(accessToken: string): JwtDisplayClaims | null {
  const payload = accessToken.split('.')[1]

  if (!payload) {
    return null
  }

  try {
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(
      Math.ceil(normalizedPayload.length / 4) * 4,
      '=',
    )
    const decodedPayload = window.atob(paddedPayload)
    const bytes = Uint8Array.from(decodedPayload, (character) =>
      character.charCodeAt(0),
    )
    const json = new TextDecoder().decode(bytes)

    return jwtDisplayClaimsSchema.parse(JSON.parse(json))
  } catch {
    return null
  }
}
