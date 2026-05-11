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

export const importJobListItemSchema = z.object({
  id: z.number(),
  importType: z.string(),
  sourceType: z.string(),
  sourceFilename: z.string(),
  status: z.string(),
  requestedBy: z.number(),
  hasFailures: z.boolean(),
  totalCount: z.number(),
  successCount: z.number(),
  failureCount: z.number(),
  errorSummary: z.string().nullable(),
  createdAt: z.string(),
  startedAt: z.string().nullable(),
  finishedAt: z.string().nullable(),
})

export const importJobPageSchema = z.object({
  items: z.array(importJobListItemSchema),
  page: z.number(),
  size: z.number(),
  total: z.number(),
  totalPages: z.number(),
})

export const importJobErrorCodeCountSchema = z.object({
  errorCode: z.string(),
  count: z.number(),
})

export const importJobErrorItemSchema = z.object({
  id: z.number(),
  rowNumber: z.number().nullable(),
  errorCode: z.string(),
  errorMessage: z.string(),
  rawPayload: z.string().nullable(),
  createdAt: z.string(),
})

export const importJobDetailSchema = z.object({
  id: z.number(),
  tenantId: z.number(),
  importType: z.string(),
  sourceType: z.string(),
  sourceFilename: z.string(),
  storageKey: z.string(),
  sourceJobId: z.number().nullable(),
  status: z.string(),
  requestedBy: z.number(),
  requestId: z.string(),
  totalCount: z.number(),
  successCount: z.number(),
  failureCount: z.number(),
  errorSummary: z.string().nullable(),
  createdAt: z.string(),
  startedAt: z.string().nullable(),
  finishedAt: z.string().nullable(),
  errorCodeCounts: z.array(importJobErrorCodeCountSchema),
  itemErrors: z.array(importJobErrorItemSchema),
})

export const importJobErrorPageSchema = z.object({
  items: z.array(importJobErrorItemSchema),
  page: z.number(),
  size: z.number(),
  total: z.number(),
  totalPages: z.number(),
})

export const approvalRequestListItemSchema = z.object({
  id: z.number(),
  actionType: z.string(),
  entityType: z.string(),
  entityId: z.number(),
  requestedBy: z.number(),
  reviewedBy: z.number().nullable(),
  status: z.string(),
  createdAt: z.string(),
  reviewedAt: z.string().nullable(),
  executedAt: z.string().nullable(),
})

export const approvalRequestPageSchema = z.object({
  items: z.array(approvalRequestListItemSchema),
  page: z.number(),
  size: z.number(),
  total: z.number(),
  totalPages: z.number(),
})

export const approvalRequestSchema = z.object({
  id: z.number(),
  tenantId: z.number(),
  actionType: z.string(),
  entityType: z.string(),
  entityId: z.number(),
  requestedBy: z.number(),
  reviewedBy: z.number().nullable(),
  status: z.string(),
  payloadJson: z.string(),
  requestId: z.string(),
  createdAt: z.string(),
  reviewedAt: z.string().nullable(),
  executedAt: z.string().nullable(),
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

export const aiInteractionUsageSummaryByInteractionTypeSchema = z.object({
  interactionType: z.string(),
  count: z.number(),
  succeededCount: z.number(),
  failedCount: z.number(),
  totalTokens: z.number(),
  totalCostMicros: z.number(),
})

export const aiInteractionUsageSummaryByStatusSchema = z.object({
  status: z.string(),
  count: z.number(),
  totalTokens: z.number(),
  totalCostMicros: z.number(),
})

export const aiInteractionUsageSummaryByPromptVersionSchema = z.object({
  promptVersion: z.string(),
  count: z.number(),
  succeededCount: z.number(),
  failedCount: z.number(),
  totalTokens: z.number(),
  totalCostMicros: z.number(),
})

export const aiInteractionUsageSummarySchema = z.object({
  from: z.string().nullable(),
  to: z.string().nullable(),
  totalInteractions: z.number(),
  succeededCount: z.number(),
  failedCount: z.number(),
  totalPromptTokens: z.number(),
  totalCompletionTokens: z.number(),
  totalTokens: z.number(),
  totalCostMicros: z.number(),
  byInteractionType: z.array(aiInteractionUsageSummaryByInteractionTypeSchema),
  byStatus: z.array(aiInteractionUsageSummaryByStatusSchema),
  byPromptVersion: z.array(aiInteractionUsageSummaryByPromptVersionSchema),
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
export type ImportJobListItem = z.infer<typeof importJobListItemSchema>
export type ImportJobPage = z.infer<typeof importJobPageSchema>
export type ImportJobErrorCodeCount = z.infer<typeof importJobErrorCodeCountSchema>
export type ImportJobErrorItem = z.infer<typeof importJobErrorItemSchema>
export type ImportJobDetail = z.infer<typeof importJobDetailSchema>
export type ImportJobErrorPage = z.infer<typeof importJobErrorPageSchema>
export type ApprovalRequestListItem = z.infer<typeof approvalRequestListItemSchema>
export type ApprovalRequestPage = z.infer<typeof approvalRequestPageSchema>
export type ApprovalRequest = z.infer<typeof approvalRequestSchema>
export type FeatureFlagItem = z.infer<typeof featureFlagItemSchema>
export type FeatureFlagList = z.infer<typeof featureFlagListSchema>
export type FeatureFlagUpdateRequest = z.infer<typeof featureFlagUpdateRequestSchema>
export type AiInteractionUsageSummary = z.infer<typeof aiInteractionUsageSummarySchema>
export type AiInteractionUsageSummaryByInteractionType = z.infer<
  typeof aiInteractionUsageSummaryByInteractionTypeSchema
>
export type AiInteractionUsageSummaryByStatus = z.infer<
  typeof aiInteractionUsageSummaryByStatusSchema
>
export type AiInteractionUsageSummaryByPromptVersion = z.infer<
  typeof aiInteractionUsageSummaryByPromptVersionSchema
>
export type JwtDisplayClaims = z.infer<typeof jwtDisplayClaimsSchema>
