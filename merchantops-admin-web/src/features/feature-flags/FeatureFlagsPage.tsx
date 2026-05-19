import {
  type UseMutationResult,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAuthenticatedRoute } from '../../components/authenticated-route-context'
import { StatusPanel } from '../../components/StatusPanel'
import {
  getFeatureFlags,
  isAuthenticationError,
  isPermissionDeniedError,
  updateFeatureFlag,
} from '../../lib/api-client'
import type { FeatureFlagItem, FeatureFlagList } from '../../lib/schemas'

const featureFlagsQueryKey = ['feature-flags'] as const

type FeatureFlagUpdateVariables = {
  key: string
  enabled: boolean
}

export function FeatureFlagsPage() {
  const { handleAuthenticationError } = useAuthenticatedRoute()
  const queryClient = useQueryClient()
  const featureFlagsQuery = useQuery({
    queryKey: featureFlagsQueryKey,
    queryFn: getFeatureFlags,
  })
  const updateMutation = useMutation({
    mutationFn: ({ key, enabled }: FeatureFlagUpdateVariables) =>
      updateFeatureFlag(key, enabled),
    onSuccess: (updatedFlag) => {
      queryClient.setQueryData<FeatureFlagList>(
        featureFlagsQueryKey,
        (currentList) => {
          if (!currentList) {
            return currentList
          }

          return {
            items: currentList.items.map((flag) =>
              flag.key === updatedFlag.key ? updatedFlag : flag,
            ),
          }
        },
      )
      void queryClient.invalidateQueries({ queryKey: featureFlagsQueryKey })
    },
  })

  useEffect(() => {
    handleAuthenticationError(featureFlagsQuery.error)
  }, [featureFlagsQuery.error, handleAuthenticationError])

  useEffect(() => {
    handleAuthenticationError(updateMutation.error)
  }, [handleAuthenticationError, updateMutation.error])

  if (featureFlagsQuery.isPending) {
    return (
      <StatusPanel
        title="Loading feature flags"
        message="Fetching current tenant rollout controls."
      />
    )
  }

  if (isPermissionDeniedError(featureFlagsQuery.error)) {
    return (
      <StatusPanel
        title="权限不足"
        message="当前账号没有 FEATURE_FLAG_MANAGE 权限。请使用具备该权限的账号访问此页面。"
      />
    )
  }

  if (
    featureFlagsQuery.error &&
    !isAuthenticationError(featureFlagsQuery.error)
  ) {
    return (
      <StatusPanel
        title="Feature flags unavailable"
        message={featureFlagsQuery.error.message}
      />
    )
  }

  if (!featureFlagsQuery.data || featureFlagsQuery.data.items.length === 0) {
    return (
      <section className="rounded-lg border border-neutral-200 bg-white p-5">
        <p className="text-sm font-medium text-emerald-700">Feature Flags</p>
        <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
          Rollout controls
        </h2>
        <p className="mt-4 text-sm text-neutral-600">
          No feature flags were returned for the current tenant.
        </p>
      </section>
    )
  }

  return (
    <section className="rounded-lg border border-neutral-200 bg-white">
      <div className="flex flex-col gap-2 border-b border-neutral-200 p-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-700">Feature Flags</p>
          <h2 className="mt-2 text-2xl font-semibold text-neutral-950">
            Rollout controls
          </h2>
        </div>
        <p className="text-sm text-neutral-600">
          Showing {featureFlagsQuery.data.items.length} fixed tenant flags
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-[780px] table-fixed divide-y divide-neutral-200 text-left">
          <thead className="bg-neutral-50 text-xs font-semibold uppercase text-neutral-500">
            <tr>
              <th className="w-[42%] px-5 py-3">Flag</th>
              <th className="w-[16%] px-5 py-3">State</th>
              <th className="w-[24%] px-5 py-3">Updated</th>
              <th className="w-[18%] px-5 py-3">Control</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 text-sm">
            {featureFlagsQuery.data.items.map((flag) => (
              <FeatureFlagRow
                key={flag.key}
                flag={flag}
                mutation={updateMutation}
              />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function FeatureFlagRow({
  flag,
  mutation,
}: {
  flag: FeatureFlagItem
  mutation: UseMutationResult<FeatureFlagItem, Error, FeatureFlagUpdateVariables>
}) {
  const isUpdating = mutation.isPending && mutation.variables?.key === flag.key
  const rowError =
    mutation.error &&
    mutation.variables?.key === flag.key &&
    !isAuthenticationError(mutation.error)
      ? mutation.error.message
      : null

  return (
    <tr>
      <td className="px-5 py-4 align-top">
        <code className="break-all rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-800">
          {flag.key}
        </code>
        {rowError ? (
          <p className="mt-3 text-xs font-medium text-rose-700">{rowError}</p>
        ) : null}
      </td>
      <td className="px-5 py-4 align-top">
        <span
          className={[
            'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
            flag.enabled
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-neutral-200 bg-neutral-50 text-neutral-600',
          ].join(' ')}
        >
          {flag.enabled ? 'Enabled' : 'Disabled'}
        </span>
      </td>
      <td className="px-5 py-4 align-top text-neutral-600">
        <span className={flag.updatedAt ? undefined : 'text-neutral-400'}>
          {formatDateTime(flag.updatedAt)}
        </span>
      </td>
      <td className="px-5 py-4 align-top">
        <button
          type="button"
          aria-pressed={flag.enabled}
          disabled={isUpdating}
          onClick={() =>
            mutation.mutate({
              key: flag.key,
              enabled: !flag.enabled,
            })
          }
          className="inline-flex min-w-32 items-center justify-between gap-3 rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 transition hover:border-neutral-500 hover:text-neutral-950 disabled:border-neutral-200 disabled:text-neutral-400"
        >
          <span
            className={[
              'relative h-5 w-9 rounded-full transition',
              flag.enabled ? 'bg-emerald-600' : 'bg-neutral-300',
            ].join(' ')}
            aria-hidden="true"
          >
            <span
              className={[
                'absolute top-0.5 h-4 w-4 rounded-full bg-white transition',
                flag.enabled ? 'left-4' : 'left-0.5',
              ].join(' ')}
            />
          </span>
          <span>{isUpdating ? 'Saving...' : flag.enabled ? 'Disable' : 'Enable'}</span>
        </button>
      </td>
    </tr>
  )
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return 'Default inventory'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ')
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
