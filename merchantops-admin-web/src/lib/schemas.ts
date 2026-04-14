import { z } from 'zod'

export const loginRequestSchema = z.object({
  tenantCode: z.string().trim().min(1, 'Tenant code is required'),
  username: z.string().trim().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

export const loginResponseSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.string().min(1),
  expiresIn: z.number().positive(),
})

export const contextResponseSchema = z.object({
  tenantId: z.number(),
  tenantCode: z.string(),
  userId: z.number(),
  username: z.string(),
})

export const jwtDisplayClaimsSchema = z
  .object({
    tenantId: z.union([z.number(), z.string()]).transform(Number),
    tenantCode: z.string(),
    username: z.string(),
    roles: z.array(z.string()).default([]),
    permissions: z.array(z.string()).default([]),
  })
  .passthrough()

export type LoginRequest = z.infer<typeof loginRequestSchema>
export type LoginResponse = z.infer<typeof loginResponseSchema>
export type ContextResponse = z.infer<typeof contextResponseSchema>
export type JwtDisplayClaims = z.infer<typeof jwtDisplayClaimsSchema>
