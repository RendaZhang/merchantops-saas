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

export const ticketListItemSchema = z.object({
  id: z.number(),
  title: z.string(),
  status: z.string(),
  assigneeId: z.number().nullable(),
  assigneeUsername: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const ticketPageSchema = z.object({
  items: z.array(ticketListItemSchema),
  page: z.number(),
  size: z.number(),
  total: z.number(),
  totalPages: z.number(),
})

export const featureFlagItemSchema = z.object({
  id: z.number().nullable(),
  key: z.string(),
  enabled: z.boolean(),
  updatedAt: z.string().nullable(),
})

export const featureFlagListSchema = z.object({
  items: z.array(featureFlagItemSchema),
})

export const featureFlagUpdateRequestSchema = z.object({
  enabled: z.boolean(),
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
export type TicketListItem = z.infer<typeof ticketListItemSchema>
export type TicketPage = z.infer<typeof ticketPageSchema>
export type FeatureFlagItem = z.infer<typeof featureFlagItemSchema>
export type FeatureFlagList = z.infer<typeof featureFlagListSchema>
export type FeatureFlagUpdateRequest = z.infer<typeof featureFlagUpdateRequestSchema>
export type JwtDisplayClaims = z.infer<typeof jwtDisplayClaimsSchema>
